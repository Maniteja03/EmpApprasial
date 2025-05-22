package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.ProjectDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Project;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_ProjectRepository;
import com.cvrce.apraisal.service.PartA_ProjectService;
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
}
