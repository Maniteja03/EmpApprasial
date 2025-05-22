package com.cvrce.apraisal.dto.parta;

import com.cvrce.apraisal.enums.PublicationType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class PublicationDTO {
    private UUID id;

    @NotBlank(message = "Title is required")
    private String title;

    private String authors;
    private int cvrAuthorCount;

    private String doiNumber;
    private String orcidId;

    private LocalDate publicationDate;
    private LocalDate indexedInScopusDate;

    private PublicationType publicationType;

    @PositiveOrZero(message = "Points must be 0 or more")
    private float pointsClaimed;

    private String proofFilePath;

    @NotNull(message = "Appraisal form ID is required")
    private UUID appraisalFormId;
}
