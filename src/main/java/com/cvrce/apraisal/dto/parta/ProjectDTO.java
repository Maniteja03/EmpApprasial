package com.cvrce.apraisal.dto.parta;

import com.cvrce.apraisal.enums.ProjectStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ProjectDTO {
    private UUID id;

    @NotNull(message = "Appraisal Form ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Project title is required")
    private String projectTitle;

    @NotBlank(message = "Funding agency is required")
    private String fundingAgency;

    private String investigators;

    @PositiveOrZero(message = "Amount must be positive")
    private double amountSanctioned;

    private ProjectStatus status;
    private LocalDate submissionDate;
    private int sanctionedYear;

    @PositiveOrZero(message = "Points must be >= 0")
    private float pointsClaimed;

    private String proofFilePath;
}
