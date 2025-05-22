package com.cvrce.apraisal.dto.parta;

import com.cvrce.apraisal.enums.PatentStatus;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PatentDTO {
    private UUID id;

    @NotNull(message = "AppraisalForm ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Patent title is required")
    private String title;

    private String applicationNumber;

    private LocalDate filingDate;

    private String inventors;

    private PatentStatus status;

    @PositiveOrZero(message = "Points must be 0 or more")
    private float pointsClaimed;

    private String proofFilePath;
}
