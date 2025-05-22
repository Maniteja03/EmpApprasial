package com.cvrce.apraisal.entity.partA;

import com.cvrce.apraisal.entity.AppraisalForm;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "parta_consultancies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartA_Consultancy {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    private String consultancyTitle;
    private String investigators;
    private String description;
    private LocalDate sanctionedDate;
    private int sanctionedYear;
    private double amount;
    private double pointsClaimed;
    private String proofFilePath;
}
