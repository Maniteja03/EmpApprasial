package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.AdminWorkDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_AdminWork;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_AdminWorkRepository;
import com.cvrce.apraisal.service.PartB_AdminWorkService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBAdminWorkDTO; // Added
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
public class PartB_AdminWorkServiceImpl implements PartB_AdminWorkService {

    private final PartB_AdminWorkRepository adminRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public AdminWorkDTO add(AdminWorkDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found"));

        PartB_AdminWork entity = PartB_AdminWork.builder()
                .appraisalForm(form)
                .component(dto.getComponent())
                .description(dto.getDescription())
                .proofFilePath(dto.getProofFilePath())
                .pointsClaimed(dto.getPointsClaimed())
                .build();

        entity = adminRepo.save(entity);
        log.info("Admin work saved with ID {}", entity.getId());
        return mapToDTO(entity);
    }

    @Override
    public List<AdminWorkDTO> getByFormId(UUID formId) {
        return adminRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminWorkDTO update(UUID id, AdminWorkDTO dto) {
        PartB_AdminWork entity = adminRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin work not found"));

        entity.setComponent(dto.getComponent());
        entity.setDescription(dto.getDescription());
        entity.setProofFilePath(dto.getProofFilePath());
        entity.setPointsClaimed(dto.getPointsClaimed());

        return mapToDTO(adminRepo.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!adminRepo.existsById(id)) {
            throw new ResourceNotFoundException("Admin work not found");
        }
        adminRepo.deleteById(id);
        log.info("Deleted admin work with ID {}", id);
    }

    private AdminWorkDTO mapToDTO(PartB_AdminWork entity) {
        AdminWorkDTO dto = new AdminWorkDTO();
        dto.setId(entity.getId());
        dto.setAppraisalFormId(entity.getAppraisalForm().getId());
        dto.setComponent(entity.getComponent());
        dto.setDescription(entity.getDescription());
        dto.setProofFilePath(entity.getProofFilePath());
        dto.setPointsClaimed(entity.getPointsClaimed());
        return dto;
    }

    @Override
    @Transactional
    public AdminWorkDTO hodUpdateAdminWork(UUID adminWorkId, HodUpdatePartBAdminWorkDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartB_AdminWork adminWork = adminRepo.findById(adminWorkId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin Work not found: " + adminWorkId));

        AppraisalForm form = adminWork.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit admin work when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        adminWork.setComponent(dto.getComponent());
        adminWork.setDescription(dto.getDescription());
        adminWork.setPointsClaimed(dto.getPointsClaimed());
        adminWork.setProofFilePath(dto.getProofFilePath());
        
        PartB_AdminWork updatedAdminWork = adminRepo.save(adminWork);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part B Admin Work: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedAdminWork.getComponent()); // Using component as an identifier
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of admin work: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated admin work {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, adminWorkId, form.getId());
        return mapToDTO(updatedAdminWork);
    }
}
