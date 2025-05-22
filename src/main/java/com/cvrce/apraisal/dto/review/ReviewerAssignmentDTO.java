package com.cvrce.apraisal.dto.review;

import lombok.Data;

import java.util.UUID;

@Data
public class ReviewerAssignmentDTO {
    private UUID id;
    private UUID reviewerId;
    private String reviewerName;
    private UUID appraisalFormId;
}
