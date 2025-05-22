package com.cvrce.apraisal.entity.review;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.enums.ReviewDecision;
import com.cvrce.apraisal.enums.ReviewLevel;

@Entity
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    @Enumerated(EnumType.STRING)
    private ReviewDecision decision; // APPROVE, REUPLOAD, FORWARD

    private String remarks;

    private LocalDateTime reviewedAt;

    @Enumerated(EnumType.STRING)
    private ReviewLevel level; // DEPARTMENT_REVIEW, HOD_REVIEW, etc.
}
