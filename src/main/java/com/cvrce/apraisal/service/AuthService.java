package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.auth.LoginRequestDTO;
import com.cvrce.apraisal.dto.auth.LoginResponseDTO;
import com.cvrce.apraisal.dto.user.UserCreateDTO;

public interface AuthService {
    LoginResponseDTO login(LoginRequestDTO request);
    void register(UserCreateDTO dto);
}
