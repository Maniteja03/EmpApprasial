package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.FeedbackScoreDTO;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBFeedbackScoreDTO; // Added import

import java.util.UUID;

public interface PartB_FeedbackScoreService {
    FeedbackScoreDTO addOrUpdateFeedbackScore(FeedbackScoreDTO dto);
    FeedbackScoreDTO getByFormId(UUID formId);
    FeedbackScoreDTO hodUpdateFeedbackScore(UUID feedbackScoreId, HodUpdatePartBFeedbackScoreDTO dto, UUID hodUserId); // Added method
}
