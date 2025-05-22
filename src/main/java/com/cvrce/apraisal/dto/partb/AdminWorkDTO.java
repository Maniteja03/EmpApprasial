package com.cvrce.apraisal.dto.partb;

import lombok.Data;
import java.util.UUID;

@Data
public class AdminWorkDTO {
    private UUID id;
    private UUID appraisalFormId;
    private String component;
    private String description;
    private float pointsClaimed;
    private String proofFilePath;
}
