package com.cvrce.apraisal.dto.partb;

import lombok.Data;
import com.cvrce.apraisal.enums.EventType;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class EventDTO {
    private UUID id;

    @NotNull(message = "Appraisal Form ID is required")
    private UUID appraisalFormId;

    @NotBlank(message = "Event title is required")
    private String eventTitle;

    @NotBlank(message = "Organization is required")
    private String organization;

    @NotBlank(message = "Role is required")
    private String role;

    @NotBlank(message = "Venue is required")
    private String venue;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Event type is required")
    private EventType eventType;

    @PositiveOrZero(message = "Points must be ≥ 0")
    private float pointsClaimed;

    private String proofFilePath;
}

