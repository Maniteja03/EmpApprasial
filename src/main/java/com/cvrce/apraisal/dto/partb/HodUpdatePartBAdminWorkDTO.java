package com.cvrce.apraisal.dto.partb;

import lombok.Data;

@Data
public class HodUpdatePartBAdminWorkDTO {
    private String component;
    private String description;
    private float pointsClaimed;
    private String proofFilePath;
}
