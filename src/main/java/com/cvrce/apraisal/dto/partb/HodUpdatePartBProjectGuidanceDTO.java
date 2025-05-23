package com.cvrce.apraisal.dto.partb;

import lombok.Data;

@Data
public class HodUpdatePartBProjectGuidanceDTO {
    private String projectTitle;
    private String projectType;
    private String academicYear;
    private float pointsClaimed;
    private String proofFilePath;
}
