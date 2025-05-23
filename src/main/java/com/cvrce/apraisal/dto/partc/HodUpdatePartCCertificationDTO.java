package com.cvrce.apraisal.dto.partc;

import lombok.Data;

@Data
public class HodUpdatePartCCertificationDTO {
    private String certificationTitle;
    private String company;
    private int studentsAllotted;
    private int studentsCertified;
    private float pointsClaimed;
    private String proofFilePath;
}
