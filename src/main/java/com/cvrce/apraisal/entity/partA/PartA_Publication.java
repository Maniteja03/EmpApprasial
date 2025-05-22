package com.cvrce.apraisal.entity.partA;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.enums.PublicationType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "parta_publications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartA_Publication {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;

    @Column(nullable = false)
    private String title;

    private String orcidId;
    private String doiNumber;
    private String authors;
    private int cvrAuthorCount;

    @Enumerated(EnumType.STRING)
    private PublicationType publicationType;

    private LocalDate publicationDate;
    private LocalDate indexedInScopusDate;

    private double pointsClaimed;
    private String proofFilePath;
}
