package com.cvrce.apraisal.dto.parta;

import com.cvrce.apraisal.enums.ProjectStatus;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HodUpdatePartAProjectDTO {
    private String projectTitle;
    private String investigators;
    private String fundingAgency;
    private ProjectStatus status;
    private LocalDate submissionDate;
    private int sanctionedYear;
    private double amountSanctioned;
    private double pointsClaimed;
    private String proofFilePath;
}
