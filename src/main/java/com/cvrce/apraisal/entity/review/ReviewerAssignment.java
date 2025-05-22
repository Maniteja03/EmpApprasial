package com.cvrce.apraisal.entity.review;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.User;

@Entity
@Table(name = "reviewer_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewerAssignment {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_form_id", nullable = false)
    private AppraisalForm appraisalForm;
}

