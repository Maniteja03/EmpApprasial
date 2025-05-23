package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.appraisal.AppraisalFormDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.AppraisalVersion;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.enums.ReviewLevel; // Added
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.AppraisalVersionRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.AppraisalFormService;
import com.cvrce.apraisal.service.DeadlineService;
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

    @Override
    @org.springframework.transaction.annotation.Transactional
    public AppraisalFormDTO hodFinalizeCorrections(UUID formId, UUID hodUserId, ReviewLevel restartReviewLevel) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));
        // Add role check for HOD if not handled by controller security alone

        AppraisalForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("AppraisalForm not found: " + formId));

        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("Form must be in REUPLOAD_REQUIRED status to finalize corrections. Current status: " + form.getStatus());
        }

        // Validate restartReviewLevel
        if (restartReviewLevel != ReviewLevel.DEPARTMENT_REVIEW &&
            restartReviewLevel != ReviewLevel.HOD_REVIEW &&
            restartReviewLevel != ReviewLevel.VERIFYING_STAFF_REVIEW) {
             throw new IllegalArgumentException("Invalid restart review level: " + restartReviewLevel + ". Must be DEPARTMENT_REVIEW, HOD_REVIEW, or VERIFYING_STAFF_REVIEW.");
        }
        
        AppraisalStatus nextAppraisalStatus;
        String remarkMessage = "HOD " + hodUser.getFullName() + " finalized corrections. ";

        switch (restartReviewLevel) {
            case DEPARTMENT_REVIEW:
                nextAppraisalStatus = AppraisalStatus.DEPARTMENT_REVIEW;
                remarkMessage += "Form moved to Department Review.";
                break;
            case HOD_REVIEW:
                nextAppraisalStatus = AppraisalStatus.HOD_REVIEW;
                remarkMessage += "Form moved to HOD Review.";
                break;
            case VERIFYING_STAFF_REVIEW: // This implies PENDING_VERIFICATION status
                nextAppraisalStatus = AppraisalStatus.PENDING_VERIFICATION;
                 remarkMessage += "Form moved to Pending Verification.";
                break;
            default: // Should be caught by validation above
                 throw new IllegalArgumentException("Unsupported restart review level: " + restartReviewLevel);
        }

        // updateAppraisalStatus handles versioning and notification to staff user.
        AppraisalFormDTO updatedFormDTO = this.updateAppraisalStatus(formId, nextAppraisalStatus, remarkMessage, hodUserId);

        // Optional: Send a specific notification to the HOD who performed the action
        NotificationDTO hodNotification = NotificationDTO.builder()
                .userId(hodUserId)
                .title("Corrections Finalized")
                .message("You have successfully finalized corrections for appraisal form ID: " + form.getId() + 
                         ". It has been moved to " + nextAppraisalStatus + ".")
                .build();
        try {
            notificationService.sendNotification(hodNotification);
        } catch (Exception e) {
            log.error("Failed to send HOD finalization confirmation for form {}: {}", form.getId(), e.getMessage());
        }

        return updatedFormDTO;
    }

}
