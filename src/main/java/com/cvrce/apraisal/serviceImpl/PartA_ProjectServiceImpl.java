package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.ProjectDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Project;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_ProjectRepository;
import com.cvrce.apraisal.service.PartA_ProjectService;
import com.cvrce.apraisal.dto.parta.HodUpdatePartAProjectDTO; // Added
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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartA_ProjectServiceImpl implements PartA_ProjectService {

    private final PartA_ProjectRepository projectRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public ProjectDTO addProject(ProjectDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found for ID: " + dto.getAppraisalFormId()));

        PartA_Project project = mapToEntity(dto);
        project.setAppraisalForm(form);

        PartA_Project saved = projectRepo.save(project);
        log.info("Project added with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    @Override
    public List<ProjectDTO> getProjectsByFormId(UUID formId) {
        List<PartA_Project> projects = projectRepo.findByAppraisalFormId(formId);
        log.info("Retrieved {} projects for form ID {}", projects.size(), formId);
        return projects.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectDTO updateProject(UUID id, ProjectDTO dto) {
        PartA_Project project = projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + id));

        project.setProjectTitle(dto.getProjectTitle());
        project.setFundingAgency(dto.getFundingAgency());
        project.setInvestigators(dto.getInvestigators());
        project.setAmountSanctioned(dto.getAmountSanctioned());
        project.setStatus(dto.getStatus());
        project.setSubmissionDate(dto.getSubmissionDate());
        project.setSanctionedYear(dto.getSanctionedYear());
        project.setPointsClaimed(dto.getPointsClaimed());
        project.setProofFilePath(dto.getProofFilePath());

        PartA_Project updated = projectRepo.save(project);
        log.info("Updated project with ID: {}", updated.getId());
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteProject(UUID id) {
        if (!projectRepo.existsById(id)) {
            throw new ResourceNotFoundException("Project not found with ID: " + id);
        }
        projectRepo.deleteById(id);
        log.info("Deleted project with ID: {}", id);
    }

    private ProjectDTO mapToDTO(PartA_Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setAppraisalFormId(project.getAppraisalForm().getId());
        dto.setProjectTitle(project.getProjectTitle());
        dto.setFundingAgency(project.getFundingAgency());
        dto.setInvestigators(project.getInvestigators());
        dto.setAmountSanctioned(project.getAmountSanctioned());
        dto.setStatus(project.getStatus());
        dto.setSubmissionDate(project.getSubmissionDate());
        dto.setSanctionedYear(project.getSanctionedYear());
        dto.setPointsClaimed((float) project.getPointsClaimed());
        dto.setProofFilePath(project.getProofFilePath());
        return dto;
    }

    private PartA_Project mapToEntity(ProjectDTO dto) {
        return PartA_Project.builder()
                .projectTitle(dto.getProjectTitle())
                .fundingAgency(dto.getFundingAgency())
                .investigators(dto.getInvestigators())
                .amountSanctioned(dto.getAmountSanctioned())
                .status(dto.getStatus())
                .submissionDate(dto.getSubmissionDate())
                .sanctionedYear(dto.getSanctionedYear())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();
    }

    @Override
    @Transactional
    public ProjectDTO hodUpdateProject(UUID projectId, HodUpdatePartAProjectDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartA_Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + projectId));

        AppraisalForm form = project.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit projects when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        project.setProjectTitle(dto.getProjectTitle());
        project.setInvestigators(dto.getInvestigators());
        project.setFundingAgency(dto.getFundingAgency());
        project.setStatus(dto.getStatus());
        project.setSubmissionDate(dto.getSubmissionDate());
        project.setSanctionedYear(dto.getSanctionedYear());
        project.setAmountSanctioned(dto.getAmountSanctioned());
        project.setPointsClaimed(dto.getPointsClaimed());
        project.setProofFilePath(dto.getProofFilePath());
        
        PartA_Project updatedProject = projectRepo.save(project);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part A Project: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedProject.getProjectTitle());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of project: {}", e.getMessage());
            // Potentially throw a custom exception or handle, for now log and proceed with null snapshot
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated project {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, projectId, form.getId());
        return mapToDTO(updatedProject);
    }
}
