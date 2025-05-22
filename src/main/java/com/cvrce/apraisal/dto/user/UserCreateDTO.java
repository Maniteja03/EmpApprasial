package com.cvrce.apraisal.dto.user;

import lombok.Data;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
public class UserCreateDTO {
    private String employeeId;
    private String fullName;
    private String email;
    private String password;
    private LocalDate dateOfJoining;
    private LocalDate lastPromotionDate;
    private Long departmentId;

    private Set<String> roles; 
}

