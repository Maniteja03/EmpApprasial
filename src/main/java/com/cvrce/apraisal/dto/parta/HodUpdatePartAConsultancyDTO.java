package com.cvrce.apraisal.dto.parta;

import lombok.Data;
import java.time.LocalDate;

@Data
public class HodUpdatePartAConsultancyDTO {
    private String consultancyTitle;
    private String investigators;
    private String description;
    private LocalDate sanctionedDate;
    private int sanctionedYear;
    private double amount;
    private double pointsClaimed;
    private String proofFilePath;
}
