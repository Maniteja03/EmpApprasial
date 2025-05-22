package com.cvrce.apraisal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "summary_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryEvaluation {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false, unique = true)
    private AppraisalForm appraisalForm;

    private float totalPointsClaimed;
    private float totalPointsAwarded;

    private String finalRecommendation; // e.g., "Promote to Level 2"
    private String remarks;

    private String reviewedByRole; // HOD / Chairperson etc.
    private UUID reviewedByUserId;
}
