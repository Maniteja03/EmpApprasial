package com.cvrce.apraisal.dto.parta;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class CitationDTO {
    private UUID id;

    @NotNull(message = "AppraisalForm ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Scopus Author ID is required")
    private String scopusAuthorId;

    @Min(value = 0, message = "Citation count must be >= 0")
    private int citationCount;

    @Min(value = 2000, message = "Citation year must be valid")
    private int citationYear;

    @PositiveOrZero(message = "Points must be zero or more")
    private float pointsClaimed;

    private String proofFilePath;
}
