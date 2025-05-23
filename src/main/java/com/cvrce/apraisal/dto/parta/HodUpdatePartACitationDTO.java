package com.cvrce.apraisal.dto.parta;

import lombok.Data;

@Data
public class HodUpdatePartACitationDTO {
    private String scopusAuthorId;
    private int citationCount;
    private int citationYear;
    private double pointsClaimed;
    private String proofFilePath;
}
