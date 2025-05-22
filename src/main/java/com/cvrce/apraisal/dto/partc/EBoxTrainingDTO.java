package com.cvrce.apraisal.dto.partc;

import lombok.Data;

import java.util.UUID;

@Data
public class EBoxTrainingDTO {
    private UUID id;
    private UUID appraisalFormId;

    private String academicYear;
    private String courseTitle;
    private String branch;
    private String semester;

    private int studentsAllotted;
    private int studentsCompleted;
    private float pointsClaimed;

    private String proofFilePath;
}
