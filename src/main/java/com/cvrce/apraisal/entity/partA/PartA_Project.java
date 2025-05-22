package com.cvrce.apraisal.entity.partA;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "parta_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartA_Project {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String projectTitle;
    private String investigators;
    private String fundingAgency;

    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    private LocalDate submissionDate;
    private int sanctionedYear;
    private double amountSanctioned;
    private double pointsClaimed;
    private String proofFilePath;
}
