package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.OtpService;
// Assuming other dependencies of AuthServiceImpl like AuthenticationManager, JwtTokenProvider,
// RoleRepository, DepartmentRepository are not directly used by these two methods,
// but they would be @Mocked if the full class was under test for other methods.
// For these specific tests, we only need to mock what's directly used.
import org.springframework.security.crypto.password.PasswordEncoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private OtpService otpService;
    @Mock
    private PasswordEncoder passwordEncoder;
    // Mock other AuthServiceImpl dependencies if needed for setup, but keep them lenient if not used in these tests
    // @Mock private AuthenticationManager authenticationManager;
    // @Mock private JwtTokenProvider jwtTokenProvider;
    // @Mock private RoleRepository roleRepository;
    // @Mock private DepartmentRepository departmentRepository;


    @InjectMocks
    private AuthServiceImpl authService;

    private String testEmail;
    private String testOtp;
    private String testNewPassword;
    private User testUser;

    @BeforeEach
    void setUp() {
        testEmail = "testuser@example.com";
        testOtp = "123456";
        testNewPassword = "newSecurePassword123";
        testUser = User.builder().id(UUID.randomUUID()).email(testEmail).fullName("Test User").password("oldEncodedPassword").build();
    }

    // Tests for initiatePasswordReset
    @Test
    void initiatePasswordReset_UserExists_CallsOtpServiceAndReturnsGenericMessage() {
        // Arrange
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(otpService.generateOtp(testEmail)).thenReturn(testOtp); // otpService.generateOtp now also sends email

        // Act
        String response = authService.initiatePasswordReset(testEmail);

        // Assert
        verify(otpService).generateOtp(testEmail);
        assertEquals("If your email address is registered with us, you will receive a password reset OTP.", response);
    }

    @Test
    void initiatePasswordReset_UserDoesNotExist_DoesNotCallOtpServiceAndReturnsGenericMessage() {
        // Arrange
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        // Act
        String response = authService.initiatePasswordReset(testEmail);

        // Assert
        verify(otpService, never()).generateOtp(anyString());
        assertEquals("If your email address is registered with us, you will receive a password reset OTP.", response);
    }

    // Tests for resetPasswordWithOtp
    @Test
    void resetPasswordWithOtp_ValidOtpAndUserExists_ResetsPasswordAndInvalidatesOtp() {
        // Arrange
        String encodedPassword = "encodedNewPassword";
        when(otpService.validateOtp(testEmail, testOtp)).thenReturn(true);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(testNewPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        String response = authService.resetPasswordWithOtp(testEmail, testOtp, testNewPassword);

        // Assert
        verify(otpService).validateOtp(testEmail, testOtp);
        verify(userRepository).findByEmail(testEmail);
        verify(passwordEncoder).encode(testNewPassword);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(encodedPassword, userCaptor.getValue().getPassword());
        verify(otpService).invalidateOtp(testEmail);
        assertEquals("Password has been reset successfully. You can now login with your new password.", response);
    }

    @Test
    void resetPasswordWithOtp_InvalidOtp_ReturnsErrorMessage() {
        // Arrange
        when(otpService.validateOtp(testEmail, testOtp)).thenReturn(false);

        // Act
        String response = authService.resetPasswordWithOtp(testEmail, testOtp, testNewPassword);

        // Assert
        verify(otpService).validateOtp(testEmail, testOtp);
        verify(userRepository, never()).findByEmail(anyString());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(otpService, never()).invalidateOtp(anyString());
        assertEquals("Invalid or expired OTP. Please try again or request a new OTP.", response);
    }

    @Test
    void resetPasswordWithOtp_UserNotFoundAfterOtpValidation_ThrowsResourceNotFoundException() {
        // Arrange
        when(otpService.validateOtp(testEmail, testOtp)).thenReturn(true);
        when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            authService.resetPasswordWithOtp(testEmail, testOtp, testNewPassword);
        });
        assertTrue(exception.getMessage().contains("User not found: " + testEmail));
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(otpService, never()).invalidateOtp(testEmail); // OTP should not be invalidated if process fails before completion
    }
    
    // ArgumentCaptor for verifying saved user details
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
}
