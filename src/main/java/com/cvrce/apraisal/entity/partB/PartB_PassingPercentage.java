package com.cvrce.apraisal.entity.partB;

import com.cvrce.apraisal.entity.AppraisalForm;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "partb_passing_percentages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartB_PassingPercentage {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String academicYear;
    private String subject;
    private String semester;
    private String section;
    private int registeredStudents;
    private int passedStudents;
    private float pointsClaimed;
    
    @Column(name = "proof_file_path")
    private String proofFilePath;

}
