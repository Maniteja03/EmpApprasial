package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partc.EBoxTrainingDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partC.PartC_EBoxTraining;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartC_EBoxTrainingRepository;
import com.cvrce.apraisal.service.PartC_EBoxTrainingService;
import com.cvrce.apraisal.dto.partc.HodUpdatePartCEBoxTrainingDTO; // Added
import com.cvrce.apraisal.entity.AppraisalVersion; // Added
import com.cvrce.apraisal.repo.AppraisalVersionRepository; // Added
import com.cvrce.apraisal.repo.UserRepository; // Added
import com.cvrce.apraisal.entity.User; // Added
import com.cvrce.apraisal.enums.AppraisalStatus; // Added
import com.fasterxml.jackson.databind.ObjectMapper; // Added
import com.fasterxml.jackson.core.JsonProcessingException; // Added
import java.time.LocalDateTime; // Added
import jakarta.transaction.Transactional; // Added for @Transactional
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartC_EBoxTrainingServiceImpl implements PartC_EBoxTrainingService {

    private final PartC_EBoxTrainingRepository eboxRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    public EBoxTrainingDTO addEBoxTraining(EBoxTrainingDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal Form not found"));

        PartC_EBoxTraining training = PartC_EBoxTraining.builder()
                .appraisalForm(form)
                .academicYear(dto.getAcademicYear())
                .courseTitle(dto.getCourseTitle())
                .branch(dto.getBranch())
                .semester(dto.getSemester())
                .studentsAllotted(dto.getStudentsAllotted())
                .studentsCompleted(dto.getStudentsCompleted())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();

        return mapToDTO(eboxRepo.save(training));
    }

    @Override
    public List<EBoxTrainingDTO> getEBoxTrainingsByFormId(UUID formId) {
        return eboxRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EBoxTrainingDTO updateEBoxTraining(UUID id, EBoxTrainingDTO dto) {
        PartC_EBoxTraining training = eboxRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Box Training not found"));

        training.setAcademicYear(dto.getAcademicYear());
        training.setCourseTitle(dto.getCourseTitle());
        training.setBranch(dto.getBranch());
        training.setSemester(dto.getSemester());
        training.setStudentsAllotted(dto.getStudentsAllotted());
        training.setStudentsCompleted(dto.getStudentsCompleted());
        training.setPointsClaimed(dto.getPointsClaimed());
        training.setProofFilePath(dto.getProofFilePath());

        return mapToDTO(eboxRepo.save(training));
    }

    @Override
    public void deleteEBoxTraining(UUID id) {
        if (!eboxRepo.existsById(id)) {
            throw new ResourceNotFoundException("E-Box Training not found");
        }
        eboxRepo.deleteById(id);
    }

    private EBoxTrainingDTO mapToDTO(PartC_EBoxTraining training) {
        EBoxTrainingDTO dto = new EBoxTrainingDTO();
        dto.setId(training.getId());
        dto.setAppraisalFormId(training.getAppraisalForm().getId());
        dto.setAcademicYear(training.getAcademicYear());
        dto.setCourseTitle(training.getCourseTitle());
        dto.setBranch(training.getBranch());
        dto.setSemester(training.getSemester());
        dto.setStudentsAllotted(training.getStudentsAllotted());
        dto.setStudentsCompleted(training.getStudentsCompleted());
        dto.setPointsClaimed(training.getPointsClaimed());
        dto.setProofFilePath(training.getProofFilePath());
        return dto;
    }

    @Override
    @Transactional
    public EBoxTrainingDTO hodUpdateEBoxTraining(UUID eboxTrainingId, HodUpdatePartCEBoxTrainingDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartC_EBoxTraining eboxTraining = eboxRepo.findById(eboxTrainingId)
                .orElseThrow(() -> new ResourceNotFoundException("EBoxTraining not found: " + eboxTrainingId));

        AppraisalForm form = eboxTraining.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit E-Box training when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        eboxTraining.setAcademicYear(dto.getAcademicYear());
        eboxTraining.setCourseTitle(dto.getCourseTitle());
        eboxTraining.setBranch(dto.getBranch());
        eboxTraining.setSemester(dto.getSemester());
        eboxTraining.setStudentsAllotted(dto.getStudentsAllotted());
        eboxTraining.setStudentsCompleted(dto.getStudentsCompleted());
        eboxTraining.setPointsClaimed(dto.getPointsClaimed());
        eboxTraining.setProofFilePath(dto.getProofFilePath());
        
        PartC_EBoxTraining updatedEBoxTraining = eboxRepo.save(eboxTraining);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part C E-Box Training: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedEBoxTraining.getCourseTitle()); // Using course title as an identifier
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of E-Box training: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated E-Box training {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, eboxTrainingId, form.getId());
        return mapToDTO(updatedEBoxTraining);
    }
}
