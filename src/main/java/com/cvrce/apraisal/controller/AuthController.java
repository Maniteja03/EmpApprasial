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
        // The authService.initiatePasswordReset method now handles:
        // 1. Checking if user exists by email.
        // 2. Calling otpService.generateOtp (which logs OTP and sends email).
        // 3. Returning an appropriate user-facing message.
        String serviceResponse = authService.initiatePasswordReset(email);
        
        // Determine HTTP status based on a convention if needed, or assume service handles logging of "user not found"
        // For now, if service returns a message, assume it's generally an OK scenario from controller perspective.
        // The service method itself prevents leaking info about whether email is registered.
        if (serviceResponse.toLowerCase().contains("if your email address is registered")) { // Heuristic
             return ResponseEntity.ok(serviceResponse);
        } else {
            // This case should ideally not be hit if the service always returns the generic message.
            // But as a fallback, or if service changes its message.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request.");
        }
    }

    // 🔄 Reset password after OTP verification
    @PostMapping("/verify-otp-reset-password")
    public ResponseEntity<String> resetPasswordViaOtp(@RequestBody ResetPasswordRequest dto) {
        String serviceResponse = authService.resetPasswordWithOtp(dto.getEmail(), dto.getOtp(), dto.getNewPassword());

        if (serviceResponse.toLowerCase().contains("successfully")) {
            return ResponseEntity.ok(serviceResponse);
        } else if (serviceResponse.toLowerCase().contains("invalid or expired otp")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(serviceResponse);
        } else {
            // For other errors, e.g., user not found post-OTP validation (should be rare)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(serviceResponse);
        }
    }
}
