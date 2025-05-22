package com.cvrce.apraisal.dto.appraisal;

import com.cvrce.apraisal.enums.AppraisalStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AppraisalVersionDTO {
    private UUID id;
    private UUID appraisalFormId;
    private AppraisalStatus statusAtVersion;
    private String remarks;
    private String serializedSnapshot;
    private LocalDateTime versionTimestamp;
}
