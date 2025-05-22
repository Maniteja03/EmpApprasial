package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.ProjectGuidanceDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_ProjectGuidance;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_ProjectGuidanceRepository;
import com.cvrce.apraisal.service.PartB_ProjectGuidanceService;

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
}
