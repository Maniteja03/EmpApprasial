package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.appraisal.AppraisalVersionDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.AppraisalVersion;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.AppraisalVersionRepository;
import com.cvrce.apraisal.service.AppraisalVersionService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppraisalVersionServiceImpl implements AppraisalVersionService {

    private final AppraisalVersionRepository versionRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    @Transactional
    public AppraisalVersionDTO addVersion(AppraisalVersionDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .remarks(dto.getRemarks())
                .serializedSnapshot(dto.getSerializedSnapshot())
                .statusAtVersion(dto.getStatusAtVersion())
                .versionTimestamp(LocalDateTime.now())
                .build();

        AppraisalVersion saved = versionRepo.save(version);
        log.info("New version saved for form {}", form.getId());
        return mapToDTO(saved);
    }

    @Override
    public List<AppraisalVersionDTO> getVersionsByForm(UUID formId) {
        return versionRepo.findByAppraisalFormIdOrderByVersionTimestampDesc(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private AppraisalVersionDTO mapToDTO(AppraisalVersion version) {
        AppraisalVersionDTO dto = new AppraisalVersionDTO();
        dto.setId(version.getId());
        dto.setAppraisalFormId(version.getAppraisalForm().getId());
        dto.setStatusAtVersion(version.getStatusAtVersion());
        dto.setRemarks(version.getRemarks());
        dto.setSerializedSnapshot(version.getSerializedSnapshot());
        dto.setVersionTimestamp(version.getVersionTimestamp());
        return dto;
    }
}
