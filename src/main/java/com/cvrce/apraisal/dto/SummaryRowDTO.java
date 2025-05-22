package com.cvrce.apraisal.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SummaryRowDTO {
    private String staffName;
    private String department;
    private String academicYear;
    private double totalScore;
    private float pointsClaimed;
    private float pointsAwarded;
    private String remarks;
    private String recommendation;
}
