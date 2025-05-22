package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.ResetPasswordRequest;
import com.cvrce.apraisal.dto.auth.LoginRequestDTO;
import com.cvrce.apraisal.dto.auth.LoginResponseDTO;
import com.cvrce.apraisal.dto.user.UserCreateDTO;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.AuthService;
import com.cvrce.apraisal.service.OtpService;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    // 🔐 Login Endpoint
	/*
	 * @PostMapping("/login") public ResponseEntity<LoginResponseDTO>
	 * login(@RequestBody LoginRequestDTO request) { return
	 * ResponseEntity.ok(authService.login(request)); }
	 */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            LoginResponseDTO response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Something went wrong: " + e.getMessage());
        }
    }

    // 👤 Register new user (Super Admin only)
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserCreateDTO dto) {
        authService.register(dto);
        return ResponseEntity.ok("User registered successfully");
    }

    // 📩 Request OTP for password reset
    @PostMapping("/request-otp")
    public ResponseEntity<String> requestOtp(@RequestParam String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        String otp = otpService.generateOtp(email);
        // TODO: Integrate with real notification/email service
        return ResponseEntity.ok("OTP sent to your email: " + otp); // Just return for testing
    }

    // 🔄 Reset password after OTP verification
    @PostMapping("/verify-otp-reset-password")
    public ResponseEntity<String> resetPasswordViaOtp(@RequestBody ResetPasswordRequest dto) {
        if (!otpService.validateOtp(dto.getEmail(), dto.getOtp())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);

        otpService.invalidateOtp(dto.getEmail()); // Clear OTP after use

        return ResponseEntity.ok("Password updated successfully");
    }
}
