package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.SummaryEvaluationDTO;

import java.util.UUID;

public interface SummaryEvaluationService {
    SummaryEvaluationDTO getSummaryByFormId(UUID formId);
    SummaryEvaluationDTO saveOrUpdateSummary(SummaryEvaluationDTO dto);
}
