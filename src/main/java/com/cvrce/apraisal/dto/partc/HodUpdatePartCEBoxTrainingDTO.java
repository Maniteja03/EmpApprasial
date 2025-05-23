package com.cvrce.apraisal.dto.partc;

import lombok.Data;

@Data
public class HodUpdatePartCEBoxTrainingDTO {
    private String academicYear;
    private String courseTitle;
    private String branch;
    private String semester;
    private int studentsAllotted;
    private int studentsCompleted;
    private float pointsClaimed;
    private String proofFilePath;
}
