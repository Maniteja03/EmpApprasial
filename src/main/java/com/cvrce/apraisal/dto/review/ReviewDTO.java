package com.cvrce.apraisal.dto.review;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ReviewDTO {
    private UUID id;
    private UUID reviewerId;
    private UUID appraisalFormId;
    private String decision;
    private String remarks;
    private String level;
    private LocalDateTime reviewedAt;
}
