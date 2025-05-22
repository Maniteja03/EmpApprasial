package com.cvrce.apraisal.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class SummaryDTO {
    private UUID appraisalFormId;
    private String employeeId;
    private String fullName;
    private String academicYear;
    private float totalScore;
    private String finalGrade;
    private String status; // e.g., COMPLETED, SUBMITTED, etc.
}
