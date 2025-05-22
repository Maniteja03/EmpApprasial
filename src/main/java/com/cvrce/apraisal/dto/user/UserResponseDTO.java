package com.cvrce.apraisal.dto.user;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;



@Data
@Builder
public class UserResponseDTO {
    private UUID id;
    private String employeeId;
    private String fullName;
    private String email;
    private boolean enabled;
    private LocalDate dateOfJoining;
    private LocalDate lastPromotionDate;
    private String departmentName;
	
}
