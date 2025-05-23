package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.AwardDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_Award;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_AwardRepository;
import com.cvrce.apraisal.service.PartB_AwardService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBAwardDTO; // Added
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
public class PartB_AwardServiceImpl implements PartB_AwardService {

    private final PartB_AwardRepository awardRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public AwardDTO addAward(AwardDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal Form not found"));

        PartB_Award award = PartB_Award.builder()
                .appraisalForm(form)
                .awardTitle(dto.getAwardTitle())
                .academicYear(dto.getAcademicYear())
                .dateAwarded(dto.getDateAwarded())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();

        award = awardRepo.save(award);
        log.info("Award added with ID {}", award.getId());
        return mapToDTO(award);
    }

    @Override
    public List<AwardDTO> getAwardsByFormId(UUID formId) {
        return awardRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AwardDTO updateAward(UUID id, AwardDTO dto) {
        PartB_Award award = awardRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Award not found"));

        award.setAwardTitle(dto.getAwardTitle());
        award.setAcademicYear(dto.getAcademicYear());
        award.setDateAwarded(dto.getDateAwarded());
        award.setPointsClaimed(dto.getPointsClaimed());
        award.setProofFilePath(dto.getProofFilePath());

        award = awardRepo.save(award);
        log.info("Award updated: {}", id);
        return mapToDTO(award);
    }

    @Override
    @Transactional
    public void deleteAward(UUID id) {
        if (!awardRepo.existsById(id)) {
            throw new ResourceNotFoundException("Award not found");
        }
        awardRepo.deleteById(id);
        log.info("Award deleted: {}", id);
    }

    private AwardDTO mapToDTO(PartB_Award award) {
        AwardDTO dto = new AwardDTO();
        dto.setId(award.getId());
        dto.setAppraisalFormId(award.getAppraisalForm().getId());
        dto.setAwardTitle(award.getAwardTitle());
        dto.setAcademicYear(award.getAcademicYear());
        dto.setDateAwarded(award.getDateAwarded());
        dto.setPointsClaimed(award.getPointsClaimed());
        dto.setProofFilePath(award.getProofFilePath());
        return dto;
    }

    @Override
    @Transactional
    public AwardDTO hodUpdateAward(UUID awardId, HodUpdatePartBAwardDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartB_Award award = awardRepo.findById(awardId)
                .orElseThrow(() -> new ResourceNotFoundException("Award not found: " + awardId));

        AppraisalForm form = award.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit awards when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        award.setAwardTitle(dto.getAwardTitle());
        award.setAcademicYear(dto.getAcademicYear());
        award.setDateAwarded(dto.getDateAwarded());
        award.setPointsClaimed(dto.getPointsClaimed());
        award.setProofFilePath(dto.getProofFilePath());
        
        PartB_Award updatedAward = awardRepo.save(award);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part B Award: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedAward.getAwardTitle());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of award: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated award {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, awardId, form.getId());
        return mapToDTO(updatedAward);
    }
}
