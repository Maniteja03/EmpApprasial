package com.cvrce.apraisal.entity.partC;

import com.cvrce.apraisal.entity.AppraisalForm;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "partc_certifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartC_Certification {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String certificationTitle;
    private String company;
    private int studentsAllotted;
    private int studentsCertified;
    private float pointsClaimed;

    private String proofFilePath;
}
