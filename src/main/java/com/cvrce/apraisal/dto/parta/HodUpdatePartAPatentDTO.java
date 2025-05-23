package com.cvrce.apraisal.dto.parta;

import com.cvrce.apraisal.enums.PatentStatus;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HodUpdatePartAPatentDTO {
    private String title;
    private String inventors;
    private String applicationNumber;
    private PatentStatus status;
    private LocalDate filingDate;
    private double pointsClaimed;
    private String proofFilePath;
}
