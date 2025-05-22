package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.service.OtpService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class OtpServiceImpl implements OtpService {

    private static final int EXPIRY_MINUTES = 5;

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
        return otp;
    }

    @Override
    public boolean validateOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null || !entry.otp.equals(otp)) return false;

        return LocalDateTime.now().isBefore(entry.createdTime.plusMinutes(EXPIRY_MINUTES));
    }

    public void invalidateOtp(String email) {
        otpStore.remove(email);
    }
}
