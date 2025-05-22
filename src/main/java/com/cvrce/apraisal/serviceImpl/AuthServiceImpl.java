package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.auth.LoginRequestDTO;
import com.cvrce.apraisal.dto.auth.LoginResponseDTO;
import com.cvrce.apraisal.dto.user.UserCreateDTO;
import com.cvrce.apraisal.entity.Department;
import com.cvrce.apraisal.entity.Role;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.repo.DepartmentRepository;
import com.cvrce.apraisal.repo.RoleRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.security.JwtTokenProvider;
import com.cvrce.apraisal.service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String token = jwtTokenProvider.generateToken(user);

        return LoginResponseDTO.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRoles().iterator().next().getName()) // Assumes one role per user
                .build();
    }

    @Transactional
    @Override
    public void register(UserCreateDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("User already exists with this email");
        }

        Role defaultRole = roleRepository.findByName("STAFF")
                .orElseThrow(() -> new RuntimeException("Default role not found"));

        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Invalid department"));

        User user = User.builder()
                .id(UUID.randomUUID())
                .employeeId(dto.getEmployeeId())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .enabled(true)
                .deleted(false)
                .dateOfJoining(dto.getDateOfJoining())
                .lastPromotionDate(dto.getLastPromotionDate())
                .department(department)
                .roles(Collections.singleton(defaultRole))
                .build();

        userRepository.save(user);
    }

}
