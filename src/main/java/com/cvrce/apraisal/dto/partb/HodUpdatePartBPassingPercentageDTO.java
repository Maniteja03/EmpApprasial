package com.cvrce.apraisal.dto.partb;

import lombok.Data;

@Data
public class HodUpdatePartBPassingPercentageDTO {
    private String academicYear;
    private String subject;
    private String semester;
    private String section;
    private int registeredStudents;
    private int passedStudents;
    private float pointsClaimed;
    private String proofFilePath;
}
