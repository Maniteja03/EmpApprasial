package com.cvrce.apraisal.dto.partb;

import lombok.Data;

import java.util.UUID;

import jakarta.validation.constraints.*;

@Data
public class ResearchGuidanceDTO {
    private UUID id;

    @NotNull(message = "AppraisalFormId is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Scholar name is required")
    private String scholarName;

    private String admissionId;

    @NotBlank(message = "University is required")
    private String university;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    private float pointsClaimed;

    private String proofFilePath;
}

