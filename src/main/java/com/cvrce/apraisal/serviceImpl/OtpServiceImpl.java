package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.entity.User; // Added
import com.cvrce.apraisal.exception.ResourceNotFoundException; // Added
import com.cvrce.apraisal.repo.UserRepository; // Added
import com.cvrce.apraisal.service.OtpService;
import lombok.RequiredArgsConstructor; // Added assuming it will be used
import lombok.extern.slf4j.Slf4j; // Added for logging
import org.springframework.mail.SimpleMailMessage; // Added
import org.springframework.mail.javamail.JavaMailSender; // Added
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional; // Added

@Service
@RequiredArgsConstructor // Added
@Slf4j // Added
public class OtpServiceImpl implements OtpService {

    private static final int EXPIRY_MINUTES = 5;

    private final JavaMailSender mailSender; // Added
    private final UserRepository userRepository; // Added

    private static class OtpEntry {
        String otp;
        LocalDateTime createdTime;

        OtpEntry(String otp, LocalDateTime createdTime) {
            this.otp = otp;
            this.createdTime = createdTime;
        }
    }

    private final Map<String, OtpEntry> otpStore = new HashMap<>();

    @Override
    public String generateOtp(String email) {
        // Generate OTP using UUID (only numbers, 6 digits)
        String uuid = java.util.UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        String otp = uuid.length() >= 6 ? uuid.substring(0, 6) : String.format("%06d", (uuid.hashCode() & 0xfffffff) % 1000000);

        otpStore.put(email, new OtpEntry(otp, LocalDateTime.now()));
        log.info("Generated OTP for email {}: {}", email, otp); // Log OTP for debugging if needed

        // Send OTP via Email
        try {
            // Fetch user to get full name for a more personalized email
            Optional<User> userOptional = userRepository.findByEmail(email); // Assuming findByEmail exists
            
            String userName = "User"; // Default if user not found or no name
            if (userOptional.isPresent() && userOptional.get().getFullName() != null && !userOptional.get().getFullName().isEmpty()) {
                userName = userOptional.get().getFullName();
            } else {
                log.warn("User not found or name not available for email: {}. Sending generic email.", email);
                // For a strict "forgot password", if userOptional is empty, you might throw an exception
                // or not send an email, as password reset implies user must exist.
                // For this task, we'll send a generic email if user details are not fully present.
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Your Password Reset OTP");
            message.setText("Dear " + userName + ",\n\n" +
                            "Your One-Time Password for resetting your password is: " + otp + "\n\n" +
                            "This OTP is valid for " + EXPIRY_MINUTES + " minutes.\n\n" +
                            "If you did not request a password reset, please ignore this email or contact support if you have concerns.\n\n" +
                            "Thank you.");
            mailSender.send(message);
            log.info("Successfully sent OTP to email: {}", email);
        } catch (Exception e) {
            log.error("Error sending OTP email to {}: {}", email, e.getMessage(), e);
            // Depending on policy, you might want to:
            // 1. Throw a custom exception to indicate email sending failed.
            // 2. Silently fail (OTP is generated and stored, but not emailed).
            // For now, logging the error. The OTP is still generated and valid.
        }

        return otp; // Return OTP as per existing contract
    }

    @Override
    public boolean validateOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null || !entry.otp.equals(otp)) return false;

        boolean isValid = LocalDateTime.now().isBefore(entry.createdTime.plusMinutes(EXPIRY_MINUTES));
        if (!isValid) {
            otpStore.remove(email); // Remove expired OTP
            log.warn("Attempt to validate expired OTP for email: {}", email);
        }
        return isValid;
    }

    @Override // Added @Override as it's good practice for interface methods
    public void invalidateOtp(String email) {
        OtpEntry removed = otpStore.remove(email);
        if (removed != null) {
            log.info("Invalidated OTP for email: {}", email);
        }
    }
}
