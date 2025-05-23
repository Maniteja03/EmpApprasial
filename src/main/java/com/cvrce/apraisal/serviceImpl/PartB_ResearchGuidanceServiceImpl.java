package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.ResearchGuidanceDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_ResearchGuidance;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_ResearchGuidanceRepository;
import com.cvrce.apraisal.service.PartB_ResearchGuidanceService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBResearchGuidanceDTO; // Added
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
public class PartB_ResearchGuidanceServiceImpl implements PartB_ResearchGuidanceService {

    private final PartB_ResearchGuidanceRepository researchRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

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

    @Override
    @Transactional
    public ResearchGuidanceDTO hodUpdateResearchGuidance(UUID researchGuidanceId, HodUpdatePartBResearchGuidanceDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartB_ResearchGuidance researchGuidance = researchRepo.findById(researchGuidanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Research Guidance not found: " + researchGuidanceId));

        AppraisalForm form = researchGuidance.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit research guidance when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        researchGuidance.setScholarName(dto.getScholarName());
        researchGuidance.setAdmissionId(dto.getAdmissionId());
        researchGuidance.setUniversity(dto.getUniversity());
        researchGuidance.setAcademicYear(dto.getAcademicYear());
        researchGuidance.setPointsClaimed(dto.getPointsClaimed());
        researchGuidance.setProofFilePath(dto.getProofFilePath());
        
        PartB_ResearchGuidance updatedResearchGuidance = researchRepo.save(researchGuidance);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part B Research Guidance for Scholar: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedResearchGuidance.getScholarName());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of research guidance: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated research guidance {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, researchGuidanceId, form.getId());
        return mapToDTO(updatedResearchGuidance);
    }
}
