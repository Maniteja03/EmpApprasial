package com.cvrce.apraisal.dto.partb;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class PassingPercentageDTO {
    private UUID id;

    @NotNull(message = "Appraisal Form ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Semester is required")
    private String semester;

    @NotBlank(message = "Section is required")
    private String section;

    @Min(value = 1, message = "Registered students must be at least 1")
    private int registeredStudents;

    @Min(value = 0, message = "Passed students must be ≥ 0")
    private int passedStudents;

    @PositiveOrZero(message = "Points must be ≥ 0")
    private float pointsClaimed;

    private String proofFilePath;
}
