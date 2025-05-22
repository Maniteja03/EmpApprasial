package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.AdminWorkDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_AdminWork;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_AdminWorkRepository;
import com.cvrce.apraisal.service.PartB_AdminWorkService;

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
}
