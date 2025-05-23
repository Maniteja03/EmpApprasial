package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.auth.LoginRequestDTO;
import com.cvrce.apraisal.dto.auth.LoginResponseDTO;
import com.cvrce.apraisal.dto.user.UserCreateDTO;
import com.cvrce.apraisal.entity.Department;
import com.cvrce.apraisal.entity.Role;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.exception.ResourceNotFoundException; // Added if not already present
import com.cvrce.apraisal.repo.DepartmentRepository;
import com.cvrce.apraisal.repo.RoleRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.security.JwtTokenProvider;
import com.cvrce.apraisal.service.AuthService;
import com.cvrce.apraisal.service.OtpService; // Added
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Added
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j // Added
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService; // Added

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

    @Override
    public String initiatePasswordReset(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (!userOptional.isPresent()) {
            log.warn("Password reset initiated for non-existent email: {}", email);
            return "If your email address is registered with us, you will receive a password reset OTP.";
        }

        // String otp = otpService.generateOtp(email); // generateOtp now also sends email
        otpService.generateOtp(email); // Call to generate and send OTP. No need to use the returned OTP here.
        
        log.info("Password reset OTP generation process initiated and email potentially sent for user: {}", email);
        
        return "If your email address is registered with us, you will receive a password reset OTP.";
    }

    @Override
    @Transactional // Ensure this method is transactional as it involves multiple repo operations
    public String resetPasswordWithOtp(String email, String otp, String newPassword) {
        if (!otpService.validateOtp(email, otp)) {
            log.warn("Invalid or expired OTP provided for email: {}", email);
            // It's often good practice to also invalidate on failed attempt after a few tries,
            // but for now, just failing is fine.
            return "Invalid or expired OTP. Please try again or request a new OTP.";
        }

        // OTP is valid, proceed to reset password
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    // This case should ideally not happen if OTP validation requires a context tied to a user.
                    // If otpService.validateOtp implies user existence, this is more of a safeguard.
                    log.error("User not found for email {} after OTP validation. Inconsistency detected.", email);
                    return new ResourceNotFoundException("User not found: " + email + ". Password reset failed post-OTP validation.");
                });

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        otpService.invalidateOtp(email); // Invalidate OTP after successful use

        log.info("Password successfully reset for user: {}", email);
        return "Password has been reset successfully. You can now login with your new password.";
    }
}
