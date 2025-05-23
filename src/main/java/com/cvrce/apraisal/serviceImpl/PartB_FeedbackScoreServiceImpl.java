package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.FeedbackScoreDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_FeedbackScore;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_FeedbackScoreRepository;
import com.cvrce.apraisal.service.PartB_FeedbackScoreService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBFeedbackScoreDTO; // Added
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

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartB_FeedbackScoreServiceImpl implements PartB_FeedbackScoreService {

    private final PartB_FeedbackScoreRepository scoreRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

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

    @Override
    @Transactional
    public FeedbackScoreDTO hodUpdateFeedbackScore(UUID feedbackScoreId, HodUpdatePartBFeedbackScoreDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartB_FeedbackScore feedbackScore = scoreRepo.findById(feedbackScoreId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback Score not found: " + feedbackScoreId));
        
        // For FeedbackScore, it's uniquely tied to an AppraisalForm, so we might not have a separate feedbackScoreId
        // if it's always 1-to-1 with the form. The existing getByFormId suggests this.
        // However, the method signature asks for feedbackScoreId, so we'll assume it's the PK of PartB_FeedbackScore.

        AppraisalForm form = feedbackScore.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit feedback scores when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        feedbackScore.setFeedbackScore(dto.getFeedbackScore());
        feedbackScore.setPointsClaimed(dto.getPointsClaimed());
        
        PartB_FeedbackScore updatedFeedbackScore = scoreRepo.save(feedbackScore);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part B Feedback Score for form %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), form.getId()); // Using form ID as identifier
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of feedback score: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated feedback score {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, feedbackScoreId, form.getId());
        return mapToDTO(updatedFeedbackScore);
    }
}
