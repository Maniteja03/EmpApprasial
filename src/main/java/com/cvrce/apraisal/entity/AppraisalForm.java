package com.cvrce.apraisal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

import com.cvrce.apraisal.enums.AppraisalStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "appraisal_forms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppraisalForm {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String academicYear; // e.g., "2024-25"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppraisalStatus status; // SUBMITTED, REUPLOAD_REQUIRED, etc.

    private LocalDate submittedDate;

    private double totalScore;

    @Builder.Default
    private boolean locked = false; // Lock after deadline

    @Builder.Default
    private boolean deleted = false;
    
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdAt;

    @UpdateTimestamp
    private LocalDate updatedAt;
    
    @Column(name = "submitted_as_role")
    private String submittedAsRole; // e.g., "STAFF", "HOD"


}
