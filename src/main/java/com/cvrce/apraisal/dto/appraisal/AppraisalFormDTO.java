package com.cvrce.apraisal.dto.appraisal;

import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

import com.cvrce.apraisal.enums.AppraisalStatus;

@Data
public class AppraisalFormDTO {
    private UUID id;
    private String academicYear;
    private float totalScore;
    private AppraisalStatus status;
    private boolean locked;
    private LocalDate submittedDate;
    private UUID userId;
    private String userName;
    private String submittedAsRole;

}

