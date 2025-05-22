package com.cvrce.apraisal.entity.partB;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

import com.cvrce.apraisal.entity.AppraisalForm;

@Entity
@Table(name = "partb_admin_work")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartB_AdminWork {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String component;

    private String description;

    private float pointsClaimed;

    private String proofFilePath;
}
