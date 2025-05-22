package com.cvrce.apraisal.dto.parta;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class ConsultancyDTO {
    private UUID id;

    @NotNull(message = "Appraisal Form ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Consultancy title is required")
    private String consultancyTitle;

    private String investigators;
    private String description;
    private LocalDate sanctionedDate;

    @Min(value = 1900, message = "Year must be valid")
    private int sanctionedYear;

    @PositiveOrZero(message = "Amount must be >= 0")
    private double amount;

    @PositiveOrZero(message = "Points must be >= 0")
    private double pointsClaimed;

    private String proofFilePath;
}
