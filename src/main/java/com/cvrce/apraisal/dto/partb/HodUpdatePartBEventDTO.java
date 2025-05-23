package com.cvrce.apraisal.dto.partb;

import com.cvrce.apraisal.enums.EventType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HodUpdatePartBEventDTO {
    private EventType eventType;
    private String eventTitle;
    private String role;
    private String organization;
    private String venue;
    private LocalDate startDate;
    private LocalDate endDate;
    private float pointsClaimed;
    private String proofFilePath;
}
