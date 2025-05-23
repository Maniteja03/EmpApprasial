package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.ProjectGuidanceDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_ProjectGuidance;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_ProjectGuidanceRepository;
import com.cvrce.apraisal.service.PartB_ProjectGuidanceService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBProjectGuidanceDTO; // Added
import com.cvrce.apraisal.entity.AppraisalVersion; // Added
import com.cvrce.apraisal.repo.AppraisalVersionRepository; // Added
import com.cvrce.apraisal.repo.UserRepository; // Added
import com.cvrce.apraisal.entity.User; // Added
import com.cvrce.apraisal.enums.AppraisalStatus; // Added
import com.fasterxml.jackson.databind.ObjectMapper; // Added
import com.fasterxml.jackson.core.JsonProcessingException; // Added
import java.time.LocalDateTime; // Added

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartB_ProjectGuidanceServiceImpl implements PartB_ProjectGuidanceService {

    private final PartB_ProjectGuidanceRepository projectRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public ProjectGuidanceDTO add(ProjectGuidanceDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found"));

        PartB_ProjectGuidance entity = PartB_ProjectGuidance.builder()
                .appraisalForm(form)
                .projectTitle(dto.getProjectTitle())
                .projectType(dto.getProjectType())
                .academicYear(dto.getAcademicYear())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();

        entity = projectRepo.save(entity);
        log.info("Saved project guidance with ID {}", entity.getId());
        return mapToDTO(entity);
    }

    @Override
    public List<ProjectGuidanceDTO> getByFormId(UUID formId) {
        return projectRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectGuidanceDTO update(UUID id, ProjectGuidanceDTO dto) {
        PartB_ProjectGuidance entity = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project guidance not found"));

        entity.setProjectTitle(dto.getProjectTitle());
        entity.setProjectType(dto.getProjectType());
        entity.setAcademicYear(dto.getAcademicYear());
        entity.setPointsClaimed(dto.getPointsClaimed());
        entity.setProofFilePath(dto.getProofFilePath());

        return mapToDTO(projectRepo.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!projectRepo.existsById(id)) {
            throw new ResourceNotFoundException("Project guidance not found");
        }
        projectRepo.deleteById(id);
        log.info("Deleted project guidance with ID {}", id);
    }

    private ProjectGuidanceDTO mapToDTO(PartB_ProjectGuidance entity) {
        ProjectGuidanceDTO dto = new ProjectGuidanceDTO();
        dto.setId(entity.getId());
        dto.setAppraisalFormId(entity.getAppraisalForm().getId());
        dto.setProjectTitle(entity.getProjectTitle());
        dto.setProjectType(entity.getProjectType());
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setPointsClaimed(entity.getPointsClaimed());
        dto.setProofFilePath(entity.getProofFilePath());
        return dto;
    }

    @Override
    @Transactional
    public ProjectGuidanceDTO hodUpdateProjectGuidance(UUID projectGuidanceId, HodUpdatePartBProjectGuidanceDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartB_ProjectGuidance projectGuidance = projectRepo.findById(projectGuidanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Project Guidance not found: " + projectGuidanceId));

        AppraisalForm form = projectGuidance.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit project guidance when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        projectGuidance.setProjectTitle(dto.getProjectTitle());
        projectGuidance.setProjectType(dto.getProjectType());
        projectGuidance.setAcademicYear(dto.getAcademicYear());
        projectGuidance.setPointsClaimed(dto.getPointsClaimed());
        projectGuidance.setProofFilePath(dto.getProofFilePath());
        
        PartB_ProjectGuidance updatedProjectGuidance = projectRepo.save(projectGuidance);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part B Project Guidance: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedProjectGuidance.getProjectTitle());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of project guidance: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated project guidance {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, projectGuidanceId, form.getId());
        return mapToDTO(updatedProjectGuidance);
    }
}
