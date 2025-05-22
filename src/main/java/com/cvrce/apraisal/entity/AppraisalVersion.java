package com.cvrce.apraisal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.cvrce.apraisal.enums.AppraisalStatus;

@Entity
@Table(name = "appraisal_versions", indexes = {
    @Index(name = "idx_version_form", columnList = "appraisal_form_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppraisalVersion {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppraisalStatus statusAtVersion;

    private String remarks;

    private LocalDateTime versionTimestamp;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String serializedSnapshot;
}
