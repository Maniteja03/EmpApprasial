package com.cvrce.apraisal.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardSummaryDTO {
    private long totalSubmissions;
    private long submittedToday;
    private long pendingDepartmentReviews;
    private long pendingCommitteeReviews;
    private long pendingChairpersonReviews;
    private long totalApproved;
    private long totalReuploadRequested;
    private long totalDepartments;
    private List<ReviewerLoadDTO> reviewerLoad;
}
