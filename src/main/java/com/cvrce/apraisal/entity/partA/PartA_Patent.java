package com.cvrce.apraisal.entity.partA;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.enums.PatentStatus;

@Entity
@Table(name = "parta_patents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartA_Patent {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    @Column(nullable = false)
    private String title;

    private String inventors;
    private String applicationNumber;

    @Enumerated(EnumType.STRING)
    private PatentStatus status;

    private LocalDate filingDate;

    private double pointsClaimed;

    private String proofFilePath;
}
