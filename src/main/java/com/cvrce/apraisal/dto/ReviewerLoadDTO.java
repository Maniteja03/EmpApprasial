package com.cvrce.apraisal.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReviewerLoadDTO {
    private String reviewerName;
    private long pendingReviews;
}
