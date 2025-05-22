package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.ResearchGuidanceDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_ResearchGuidance;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_ResearchGuidanceRepository;
import com.cvrce.apraisal.service.PartB_ResearchGuidanceService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartB_ResearchGuidanceServiceImpl implements PartB_ResearchGuidanceService {

    private final PartB_ResearchGuidanceRepository researchRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    @Transactional
    public ResearchGuidanceDTO add(ResearchGuidanceDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found"));

        PartB_ResearchGuidance entity = PartB_ResearchGuidance.builder()
                .appraisalForm(form)
                .scholarName(dto.getScholarName())
                .admissionId(dto.getAdmissionId())
                .university(dto.getUniversity())
                .academicYear(dto.getAcademicYear())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();

        entity = researchRepo.save(entity);
        log.info("Research guidance saved with ID {}", entity.getId());
        return mapToDTO(entity);
    }

    @Override
    public List<ResearchGuidanceDTO> getByFormId(UUID formId) {
        return researchRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResearchGuidanceDTO update(UUID id, ResearchGuidanceDTO dto) {
        PartB_ResearchGuidance entity = researchRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Research guidance not found"));

        entity.setScholarName(dto.getScholarName());
        entity.setAdmissionId(dto.getAdmissionId());
        entity.setUniversity(dto.getUniversity());
        entity.setAcademicYear(dto.getAcademicYear());
        entity.setPointsClaimed(dto.getPointsClaimed());
        entity.setProofFilePath(dto.getProofFilePath());

        return mapToDTO(researchRepo.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!researchRepo.existsById(id)) {
            throw new ResourceNotFoundException("Research guidance not found");
        }
        researchRepo.deleteById(id);
        log.info("Deleted research guidance with ID {}", id);
    }

    private ResearchGuidanceDTO mapToDTO(PartB_ResearchGuidance entity) {
        ResearchGuidanceDTO dto = new ResearchGuidanceDTO();
        dto.setId(entity.getId());
        dto.setAppraisalFormId(entity.getAppraisalForm().getId());
        dto.setScholarName(entity.getScholarName());
        dto.setAdmissionId(entity.getAdmissionId());
        dto.setUniversity(entity.getUniversity());
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setPointsClaimed(entity.getPointsClaimed());
        dto.setProofFilePath(entity.getProofFilePath());
        return dto;
    }
}
