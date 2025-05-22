package com.cvrce.apraisal.dto.user;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String fullName;
    private LocalDate dateOfJoining;
    private LocalDate lastPromotionDate;
    private Long departmentId;
}

