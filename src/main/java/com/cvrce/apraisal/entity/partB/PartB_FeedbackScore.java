package com.cvrce.apraisal.entity.partB;

import com.cvrce.apraisal.entity.AppraisalForm;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "partb_feedback_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartB_FeedbackScore {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private float feedbackScore; // Out of 10

    private float pointsClaimed;
}
