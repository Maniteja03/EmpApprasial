package com.cvrce.apraisal.entity.partB;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.enums.EventType;

@Entity
@Table(name = "partb_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartB_Event {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appraisal_form_id", nullable = false)
    private AppraisalForm appraisalForm;


    @Enumerated(EnumType.STRING)
    private EventType eventType; // ORGANIZED / ATTENDED

    private String eventTitle;
    private String role;
    private String organization;
    private String venue;

    private LocalDate startDate;
    private LocalDate endDate;

    private float pointsClaimed;
    private String proofFilePath;
}
