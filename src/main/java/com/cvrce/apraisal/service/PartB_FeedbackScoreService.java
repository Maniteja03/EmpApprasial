package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.FeedbackScoreDTO;

import java.util.UUID;

public interface PartB_FeedbackScoreService {
    FeedbackScoreDTO addOrUpdateFeedbackScore(FeedbackScoreDTO dto);
    FeedbackScoreDTO getByFormId(UUID formId);
}
