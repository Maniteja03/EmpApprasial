package com.cvrce.apraisal.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {
    private String userId;
    private String email;
    private String role;
    private String token;
}
