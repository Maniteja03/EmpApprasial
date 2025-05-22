package com.cvrce.apraisal.dto.partb;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.util.UUID;

@Data
public class ProjectGuidanceDTO {
    private UUID id;

    @NotNull(message = "Appraisal Form ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Project title is required")
    private String projectTitle;

    @NotBlank(message = "Project type is required")
    private String projectType;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @PositiveOrZero(message = "Points must be ≥ 0")
    private float pointsClaimed;

    private String proofFilePath;
}
