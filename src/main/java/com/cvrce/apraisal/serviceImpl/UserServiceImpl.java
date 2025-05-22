package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.user.*;
import com.cvrce.apraisal.entity.*;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.*;
import com.cvrce.apraisal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    @Override
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    @Override
    public List<UserResponseDTO> getUsersByDepartment(Long deptId) {
        return userRepository.findByDepartmentId(deptId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDTO createUser(UserCreateDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid department"));

        Set<Role> roles = dto.getRoles().stream()
            .map(name -> roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid role: " + name)))
            .collect(Collectors.toSet());

        User user = User.builder()
               // .id(UUID.randomUUID())
                .email(dto.getEmail())
                .employeeId(dto.getEmployeeId())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .department(dept)
                .enabled(true)
                .deleted(false)
                .dateOfJoining(dto.getDateOfJoining())
                .lastPromotionDate(dto.getLastPromotionDate())
                .roles(roles) 
                .build();

        return mapToDTO(userRepository.save(user));
    }

    @Override
    public UserResponseDTO updateUser(UUID id, UserUpdateDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.getFullName() != null) user.setFullName(dto.getFullName());
        if (dto.getDateOfJoining() != null) user.setDateOfJoining(dto.getDateOfJoining());
        if (dto.getLastPromotionDate() != null) user.setLastPromotionDate(dto.getLastPromotionDate());
        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invalid department"));
            user.setDepartment(dept);
        }

        return mapToDTO(userRepository.save(user));
    }

    @Override
    public void setUserEnabled(UUID userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Override
    public void softDeleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
    }

    private UserResponseDTO mapToDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .employeeId(user.getEmployeeId())
                .fullName(user.getFullName())
                .enabled(user.isEnabled())
                .dateOfJoining(user.getDateOfJoining())
                .lastPromotionDate(user.getLastPromotionDate())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }
}
