package com.cvrce.apraisal.entity.partC;

import com.cvrce.apraisal.entity.AppraisalForm;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "partc_ebox_training")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartC_EBoxTraining {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String academicYear;
    private String courseTitle;
    private String branch;
    private String semester;

    private int studentsAllotted;
    private int studentsCompleted;
    private float pointsClaimed;

    private String proofFilePath;
}
