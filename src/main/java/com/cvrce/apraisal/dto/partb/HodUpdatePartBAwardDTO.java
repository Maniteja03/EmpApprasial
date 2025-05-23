package com.cvrce.apraisal.dto.partb;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HodUpdatePartBAwardDTO {
    private String awardTitle;
    private String academicYear;
    private LocalDate dateAwarded;
    private float pointsClaimed;
    private String proofFilePath;
}
