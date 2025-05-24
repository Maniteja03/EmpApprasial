package com.cvrce.apraisal.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBasicInfoDTO {
    private UUID userId;
    private String employeeId;
    private String fullName;
    private String email;
    private LocalDate dateOfJoining;
}
