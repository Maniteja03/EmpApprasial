package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.FeedbackScoreDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_FeedbackScore;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_FeedbackScoreRepository;
import com.cvrce.apraisal.service.PartB_FeedbackScoreService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartB_FeedbackScoreServiceImpl implements PartB_FeedbackScoreService {

    private final PartB_FeedbackScoreRepository scoreRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    @Transactional
    public FeedbackScoreDTO addOrUpdateFeedbackScore(FeedbackScoreDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found with ID: " + dto.getAppraisalFormId()));

        PartB_FeedbackScore score = scoreRepo.findByAppraisalFormId(dto.getAppraisalFormId());

        if (score == null) {
            score = new PartB_FeedbackScore();
            score.setAppraisalForm(form);
        }

        score.setFeedbackScore(dto.getFeedbackScore());
        score.setPointsClaimed(dto.getPointsClaimed());

        score = scoreRepo.save(score);
        log.info("Saved feedback score for form {}", dto.getAppraisalFormId());
        return mapToDTO(score);
    }

    @Override
    public FeedbackScoreDTO getByFormId(UUID formId) {
        PartB_FeedbackScore score = scoreRepo.findByAppraisalFormId(formId);
        if (score == null) throw new ResourceNotFoundException("No feedback score found for form ID: " + formId);
        return mapToDTO(score);
    }

    private FeedbackScoreDTO mapToDTO(PartB_FeedbackScore entity) {
        FeedbackScoreDTO dto = new FeedbackScoreDTO();
        dto.setId(entity.getId());
        dto.setAppraisalFormId(entity.getAppraisalForm().getId());
        dto.setFeedbackScore(entity.getFeedbackScore());
        dto.setPointsClaimed(entity.getPointsClaimed());
        return dto;
    }
}
