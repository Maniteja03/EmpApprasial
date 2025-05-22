package com.cvrce.apraisal.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class SummaryEvaluationDTO {
    private UUID id;
    private UUID appraisalFormId;
    private float totalPointsClaimed;
    private float totalPointsAwarded;
    private String finalRecommendation;
    private String remarks;
    private String reviewedByRole;
    private UUID reviewedByUserId;
}
