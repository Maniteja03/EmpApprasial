package com.cvrce.apraisal.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedDocumentDTO {
    private UUID id;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String section;
    private UUID appraisalFormId;
    private LocalDateTime uploadedAt;
}
