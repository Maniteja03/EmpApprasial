package com.cvrce.apraisal.entity.partA;

import com.cvrce.apraisal.entity.AppraisalForm;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "parta_citations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartA_Citation {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String scopusAuthorId;

    private int citationCount;

    private int citationYear;

    private double pointsClaimed;

    private String proofFilePath;
}
