package com.cvrce.apraisal.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NotificationDTO {
    private UUID id;
    private String title;
    private String message;
    private boolean read;
    private LocalDateTime timestamp;
    private UUID userId;
}
