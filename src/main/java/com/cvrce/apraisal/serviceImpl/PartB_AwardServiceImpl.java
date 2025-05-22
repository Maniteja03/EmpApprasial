package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.AwardDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_Award;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_AwardRepository;
import com.cvrce.apraisal.service.PartB_AwardService;

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
}
