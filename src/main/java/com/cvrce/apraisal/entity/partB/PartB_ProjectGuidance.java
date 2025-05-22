package com.cvrce.apraisal.entity.partB;

import com.cvrce.apraisal.entity.AppraisalForm;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "partb_project_guidance")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartB_ProjectGuidance {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String projectTitle;
    private String projectType;
    private String academicYear;
    private float pointsClaimed;
    private String proofFilePath;
}
