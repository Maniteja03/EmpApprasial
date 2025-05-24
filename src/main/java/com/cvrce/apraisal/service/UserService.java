package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.user.*;

import java.util.*;

public interface UserService {
    UserResponseDTO getUserByEmail(String email);
    UserResponseDTO getUserById(UUID id);
    List<UserResponseDTO> getUsersByDepartment(Long deptId);
    UserResponseDTO createUser(UserCreateDTO dto);
    UserResponseDTO updateUser(UUID id, UserUpdateDTO dto);
    void setUserEnabled(UUID userId, boolean enabled);
    void softDeleteUser(UUID id);

    List<UserBasicInfoDTO> getStaffByAuthenticatedHodDepartment();
}
