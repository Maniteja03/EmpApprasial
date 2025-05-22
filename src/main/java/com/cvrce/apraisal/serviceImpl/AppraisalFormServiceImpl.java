package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.appraisal.AppraisalFormDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.AppraisalVersion;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.AppraisalVersionRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.AppraisalFormService;
import com.cvrce.apraisal.service.DeadlineService;
import com.cvrce.apraisal.service.NotificationService; // Added
import com.cvrce.apraisal.dto.notification.NotificationDTO; // Added
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppraisalFormServiceImpl implements AppraisalFormService {

    private final AppraisalFormRepository formRepository;
    private final AppraisalVersionRepository versionRepo;
    private final UserRepository userRepository;
    private final DeadlineService deadlineService;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService; // Added

    @Override
    public AppraisalFormDTO createDraftForm(String academicYear, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<AppraisalForm> existing = formRepository.findByUserIdAndAcademicYear(userId, academicYear);
        if (existing.isPresent()) {
            throw new IllegalStateException("Form already exists for year");
        }

//        AppraisalForm form = AppraisalForm.builder()
//                .academicYear(academicYear)
//                .status(AppraisalStatus.DRAFT)
//                .user(user)
//                .locked(false)
//                .build();
        AppraisalForm form = AppraisalForm.builder()
                .academicYear(academicYear)
                .status(AppraisalStatus.DRAFT)
                .user(user)
                .locked(false)
                .submittedAsRole(user.getRoles().stream()
                    .findFirst()
                    .map(role -> role.getName())
                    .orElse("STAFF"))
                .build();


        AppraisalForm saved = formRepository.save(form);
        return mapToDTO(saved);
    }

    @Override
    public List<AppraisalFormDTO> getMySubmissions(UUID userId) {
        return formRepository.findByUserId(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppraisalFormDTO submit(UUID formId) {
        AppraisalForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        if (!deadlineService.isSubmissionOpen(form.getAcademicYear())) {
            throw new IllegalStateException("Submission deadline passed");
        }

        if (form.isLocked()) throw new IllegalStateException("Form is already submitted");
        String primaryRole = form.getUser().getRoles().stream()
        	    .findFirst()
        	    .map(role -> role.getName())
        	    .orElse("STAFF");

        form.setSubmittedAsRole(primaryRole);

        form.setStatus(AppraisalStatus.SUBMITTED);
        form.setSubmittedDate(LocalDate.now());
        form.setLocked(true);
        AppraisalForm saved = formRepository.save(form);

        // Auto-version save
        versionRepo.save(
                AppraisalVersion.builder()
                        .appraisalForm(saved)
                        .statusAtVersion(saved.getStatus())
                        .remarks("Form submitted by staff")
                        .versionTimestamp(LocalDateTime.now())
                        .serializedSnapshot(serializeForm(saved))
                        .build()
        );

        return mapToDTO(saved);
    }

    private String serializeForm(AppraisalForm form) {
        try {
            return objectMapper.writeValueAsString(form);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize form", e);
            throw new RuntimeException("Failed to serialize form");
        }
    }

    private AppraisalFormDTO mapToDTO(AppraisalForm form) {
        AppraisalFormDTO dto = new AppraisalFormDTO();
        dto.setId(form.getId());
        dto.setAcademicYear(form.getAcademicYear());
        dto.setTotalScore((float) form.getTotalScore());
        dto.setStatus(form.getStatus());
        dto.setLocked(form.isLocked());
        dto.setSubmittedDate(form.getSubmittedDate());
        dto.setUserId(form.getUser().getId());
        dto.setUserName(form.getUser().getFullName());
        dto.setSubmittedAsRole(form.getSubmittedAsRole());

        return dto;
    }

    @Override
    public List<AppraisalFormDTO> filterByStatus(AppraisalStatus status) {
        return formRepository.findByStatus(status).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public AppraisalFormDTO getById(UUID formId) {
        AppraisalForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));
        return mapToDTO(form);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public AppraisalFormDTO updateAppraisalStatus(UUID formId, AppraisalStatus newStatus, String remark, UUID changedByUserId) {
        AppraisalForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("AppraisalForm not found with id: " + formId));

        User changedByUser = userRepository.findById(changedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + changedByUserId));

        form.setStatus(newStatus);
        AppraisalForm savedForm = formRepository.save(form);

        // Notify the user whose form status changed
        String notificationTitle = "Appraisal Form Status Updated";
        String notificationMessage = "Your appraisal form for academic year " + savedForm.getAcademicYear() +
                                     " has been updated to status: " + savedForm.getStatus() + "." +
                                     (remark != null && !remark.isEmpty() ? " Remark: " + remark : "");
        
        NotificationDTO userNotification = NotificationDTO.builder()
                .userId(savedForm.getUser().getId())
                .title(notificationTitle)
                .message(notificationMessage)
                .build();
        try {
            notificationService.sendNotification(userNotification);
            log.info("Sent status update notification to user {} for form {}", savedForm.getUser().getId(), savedForm.getId());
        } catch (Exception e) {
            log.error("Failed to send status update notification to user {} for form {}: {}", savedForm.getUser().getId(), savedForm.getId(), e.getMessage());
            // Decide if this failure should be critical or just logged. For now, just log.
        }

        String versionRemark = "Status changed to " + newStatus +
                " by " + changedByUser.getFullName() + "." +
                (remark != null && !remark.isEmpty() ? " Remark: " + remark : "");

        versionRepo.save(
                AppraisalVersion.builder()
                        .appraisalForm(savedForm)
                        .statusAtVersion(savedForm.getStatus())
                        .remarks(versionRemark)
                        .versionTimestamp(LocalDateTime.now())
                        .serializedSnapshot(serializeForm(savedForm)) // Assuming serializeForm exists
                        .build()
        );

        log.info("AppraisalForm {} status updated to {} by user {}", formId, newStatus, changedByUserId);
        return mapToDTO(savedForm); // Assuming mapToDTO exists
    }
}
