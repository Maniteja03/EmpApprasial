package com.cvrce.apraisal.dto.partc;

import lombok.Data;

import java.util.UUID;

@Data
public class CertificationDTO {
    private UUID id;
    private UUID appraisalFormId;

    private String certificationTitle;
    private String company;
    private int studentsAllotted;
    private int studentsCertified;
    private float pointsClaimed;

    private String proofFilePath;
}
