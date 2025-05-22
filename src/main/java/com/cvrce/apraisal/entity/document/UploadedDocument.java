package com.cvrce.apraisal.entity.document;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.cvrce.apraisal.entity.AppraisalForm;

@Entity
@Table(name = "uploaded_documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedDocument {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    private String fileName;

    private String filePath;

    private long fileSize;

    private String section; // e.g., "PARTA_PUBLICATION"

    @ManyToOne
    @JoinColumn(name = "appraisal_form_id")
    private AppraisalForm appraisalForm;

    private LocalDateTime uploadedAt;
}
