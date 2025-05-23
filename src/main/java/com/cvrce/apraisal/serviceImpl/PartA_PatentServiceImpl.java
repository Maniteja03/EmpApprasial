package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.PatentDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Patent;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_PatentRepository;
import com.cvrce.apraisal.service.PartA_PatentService;
import com.cvrce.apraisal.dto.parta.HodUpdatePartAPatentDTO; // Added
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
public class PartA_PatentServiceImpl implements PartA_PatentService {

    private final PartA_PatentRepository patentRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public PatentDTO addPatent(PatentDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found with ID: " + dto.getAppraisalFormId()));

        PartA_Patent patent = mapToEntity(dto);
        patent.setAppraisalForm(form);

        patent = patentRepo.save(patent);
        log.info("Added patent with ID {}", patent.getId());
        return mapToDTO(patent);
    }

    @Override
    @Transactional
    public PatentDTO updatePatent(UUID id, PatentDTO dto) {
        PartA_Patent patent = patentRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patent not found with ID: " + id));

        patent.setTitle(dto.getTitle());
        patent.setApplicationNumber(dto.getApplicationNumber());
        patent.setFilingDate(dto.getFilingDate());
        patent.setInventors(dto.getInventors());
        patent.setStatus(dto.getStatus());
        patent.setPointsClaimed(dto.getPointsClaimed());
        patent.setProofFilePath(dto.getProofFilePath());

        PartA_Patent updated = patentRepo.save(patent);
        log.info("Updated patent with ID {}", updated.getId());
        return mapToDTO(updated);
    }

    @Override
    public List<PatentDTO> getPatentsByFormId(UUID formId) {
        List<PartA_Patent> patents = patentRepo.findByAppraisalFormId(formId);
        log.info("Fetched {} patents for form {}", patents.size(), formId);
        return patents.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePatent(UUID id) {
        if (!patentRepo.existsById(id)) {
            throw new ResourceNotFoundException("Patent not found with ID: " + id);
        }
        patentRepo.deleteById(id);
        log.info("Deleted patent with ID {}", id);
    }

    private PatentDTO mapToDTO(PartA_Patent patent) {
        PatentDTO dto = new PatentDTO();
        dto.setId(patent.getId());
        dto.setAppraisalFormId(patent.getAppraisalForm().getId());
        dto.setTitle(patent.getTitle());
        dto.setApplicationNumber(patent.getApplicationNumber());
        dto.setFilingDate(patent.getFilingDate());
        dto.setInventors(patent.getInventors());
        dto.setStatus(patent.getStatus());
        dto.setPointsClaimed((float) patent.getPointsClaimed());
        dto.setProofFilePath(patent.getProofFilePath());
        return dto;
    }

    private PartA_Patent mapToEntity(PatentDTO dto) {
        return PartA_Patent.builder()
                .title(dto.getTitle())
                .applicationNumber(dto.getApplicationNumber())
                .filingDate(dto.getFilingDate())
                .inventors(dto.getInventors())
                .status(dto.getStatus())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();
    }

    @Override
    @Transactional
    public PatentDTO hodUpdatePatent(UUID patentId, HodUpdatePartAPatentDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartA_Patent patent = patentRepo.findById(patentId)
                .orElseThrow(() -> new ResourceNotFoundException("Patent not found: " + patentId));

        AppraisalForm form = patent.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit patents when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        patent.setTitle(dto.getTitle());
        patent.setInventors(dto.getInventors());
        patent.setApplicationNumber(dto.getApplicationNumber());
        patent.setStatus(dto.getStatus());
        patent.setFilingDate(dto.getFilingDate());
        patent.setPointsClaimed(dto.getPointsClaimed());
        patent.setProofFilePath(dto.getProofFilePath());
        
        PartA_Patent updatedPatent = patentRepo.save(patent);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part A Patent: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedPatent.getTitle());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of patent: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated patent {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, patentId, form.getId());
        return mapToDTO(updatedPatent);
    }
}
