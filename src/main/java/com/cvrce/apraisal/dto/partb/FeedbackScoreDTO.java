package com.cvrce.apraisal.dto.partb;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class FeedbackScoreDTO {
    private UUID id;

    @NotNull(message = "Appraisal Form ID is required")
    private UUID appraisalFormId;

    @Min(value = 0, message = "Feedback score must be >= 0")
    private float feedbackScore;

    @Min(value = 0, message = "Points must be >= 0")
    private float pointsClaimed;
}
