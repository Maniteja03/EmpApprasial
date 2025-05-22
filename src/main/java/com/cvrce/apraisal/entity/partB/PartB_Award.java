package com.cvrce.apraisal.entity.partB;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

import com.cvrce.apraisal.entity.AppraisalForm;

@Entity
@Table(name = "partb_awards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartB_Award {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String awardTitle;

    private String academicYear;

    private LocalDate dateAwarded;

    private float pointsClaimed;

    private String proofFilePath;
}
