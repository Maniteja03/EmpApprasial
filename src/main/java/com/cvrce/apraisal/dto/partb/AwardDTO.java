package com.cvrce.apraisal.dto.partb;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AwardDTO {
    private UUID id;

    @NotNull(message = "Appraisal Form ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Award title is required")
    private String awardTitle;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotNull(message = "Award date is required")
    private LocalDate dateAwarded;

    @PositiveOrZero(message = "Points must be ≥ 0")
    private float pointsClaimed;

    private String proofFilePath;
}
