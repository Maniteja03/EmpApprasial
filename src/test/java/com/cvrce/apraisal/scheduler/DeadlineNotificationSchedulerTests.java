package com.cvrce.apraisal.scheduler;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.DeadlineConfig;
import com.cvrce.apraisal.entity.Role;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.DeadlineConfigRepository;
import com.cvrce.apraisal.repo.RoleRepository;
import com.cvrce.apraisal.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class DeadlineNotificationSchedulerTests {

    @Mock
    private DeadlineConfigRepository deadlineConfigRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AppraisalFormRepository appraisalFormRepository;
    @Mock
    private JavaMailSender mailSender;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private DeadlineNotificationScheduler scheduler;

    private Role staffRole;
    private User staffUser1_notSubmitted;
    private User staffUser2_submitted;
    private User staffUser3_draft;

    private final String ACADEMIC_YEAR_CURRENT = "2024-2025";


    @BeforeEach
    void setUp() {
        staffRole = Role.builder().id(1L).name("STAFF").build();
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.of(staffRole));

        staffUser1_notSubmitted = User.builder().id(UUID.randomUUID()).email("unsubmitted@example.com").fullName("Unsubmitted User").roles(Collections.singleton(staffRole)).build();
        staffUser2_submitted = User.builder().id(UUID.randomUUID()).email("submitted@example.com").fullName("Submitted User").roles(Collections.singleton(staffRole)).build();
        staffUser3_draft = User.builder().id(UUID.randomUUID()).email("draftuser@example.com").fullName("Draft User").roles(Collections.singleton(staffRole)).build();
        
        List<User> staffUsers = Arrays.asList(staffUser1_notSubmitted, staffUser2_submitted, staffUser3_draft);
        when(userRepository.findByRolesContains(staffRole)).thenReturn(staffUsers);

        // Mock form states
        // User 1: No form found for the academic year (counts as not submitted)
        when(appraisalFormRepository.findByUserIdAndAcademicYear(staffUser1_notSubmitted.getId(), ACADEMIC_YEAR_CURRENT))
            .thenReturn(Optional.empty());
        
        // User 2: Form submitted
        AppraisalForm submittedForm = AppraisalForm.builder().status(AppraisalStatus.SUBMITTED).build();
        when(appraisalFormRepository.findByUserIdAndAcademicYear(staffUser2_submitted.getId(), ACADEMIC_YEAR_CURRENT))
            .thenReturn(Optional.of(submittedForm));

        // User 3: Form in DRAFT (counts as not submitted)
        AppraisalForm draftForm = AppraisalForm.builder().status(AppraisalStatus.DRAFT).build();
        when(appraisalFormRepository.findByUserIdAndAcademicYear(staffUser3_draft.getId(), ACADEMIC_YEAR_CURRENT))
            .thenReturn(Optional.of(draftForm));
            
        // Default: No mail sending errors
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendDeadlineReminders_NoDeadlinesConfigured_LogsAndSkips() {
        // Arrange
        when(deadlineConfigRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        scheduler.sendDeadlineReminders();

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        // Check logs (requires LogCaptor or similar, or trust the log output for now)
    }

    @Test
    void sendDeadlineReminders_DeadlineFarInFuture_NoEmailsSent() {
        // Arrange
        LocalDate futureDeadline = LocalDate.now().plusDays(30); // More than 7 days away
        DeadlineConfig config = DeadlineConfig.builder().academicYear(ACADEMIC_YEAR_CURRENT).deadlineDate(futureDeadline).build();
        when(deadlineConfigRepository.findAll()).thenReturn(Collections.singletonList(config));

        // Act
        scheduler.sendDeadlineReminders();

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
    
    @Test
    void sendDeadlineReminders_DeadlinePassed_NoEmailsSent() {
        // Arrange
        LocalDate pastDeadline = LocalDate.now().minusDays(5);
        DeadlineConfig config = DeadlineConfig.builder().academicYear(ACADEMIC_YEAR_CURRENT).deadlineDate(pastDeadline).build();
        when(deadlineConfigRepository.findAll()).thenReturn(Collections.singletonList(config));

        // Act
        scheduler.sendDeadlineReminders();

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }


    @Test
    void sendDeadlineReminders_7DaysBeforeDeadline_SendsToUnsubmittedAndDraft() {
        // Arrange
        LocalDate deadline = LocalDate.now().plusDays(7);
        DeadlineConfig config = DeadlineConfig.builder().academicYear(ACADEMIC_YEAR_CURRENT).deadlineDate(deadline).build();
        when(deadlineConfigRepository.findAll()).thenReturn(Collections.singletonList(config));

        // Act
        scheduler.sendDeadlineReminders();

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messageCaptor.capture()); // For staffUser1 and staffUser3

        List<SimpleMailMessage> capturedMessages = messageCaptor.getAllValues();
        // Check for user1 (not submitted)
        assertTrue(capturedMessages.stream().anyMatch(msg -> 
            Arrays.asList(msg.getTo()).contains(staffUser1_notSubmitted.getEmail()) &&
            msg.getText().contains("You have 7 day(s) left")
        ));
        // Check for user3 (draft)
        assertTrue(capturedMessages.stream().anyMatch(msg -> 
            Arrays.asList(msg.getTo()).contains(staffUser3_draft.getEmail()) &&
            msg.getText().contains("You have 7 day(s) left")
        ));
        // Check user2 (submitted) did not receive
        assertFalse(capturedMessages.stream().anyMatch(msg -> 
            Arrays.asList(msg.getTo()).contains(staffUser2_submitted.getEmail())
        ));
    }
    
    @Test
    void sendDeadlineReminders_OnDeadlineDay_SendsToUnsubmittedAndDraft() {
        // Arrange
        LocalDate deadline = LocalDate.now(); // 0 days before
        DeadlineConfig config = DeadlineConfig.builder().academicYear(ACADEMIC_YEAR_CURRENT).deadlineDate(deadline).build();
        when(deadlineConfigRepository.findAll()).thenReturn(Collections.singletonList(config));

        // Act
        scheduler.sendDeadlineReminders();

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(messageCaptor.capture());

        List<SimpleMailMessage> capturedMessages = messageCaptor.getAllValues();
        assertTrue(capturedMessages.stream().anyMatch(msg -> 
            Arrays.asList(msg.getTo()).contains(staffUser1_notSubmitted.getEmail()) &&
            msg.getText().contains("Today is the last day")
        ));
         assertTrue(capturedMessages.stream().anyMatch(msg -> 
            Arrays.asList(msg.getTo()).contains(staffUser3_draft.getEmail()) &&
            msg.getText().contains("Today is the last day")
        ));
    }
    
    @Test
    void sendDeadlineReminders_StaffRoleNotFound_LogsErrorAndNoEmails() {
        // Arrange
        when(roleRepository.findByName("STAFF")).thenReturn(Optional.empty());
        LocalDate deadline = LocalDate.now().plusDays(1);
        DeadlineConfig config = DeadlineConfig.builder().academicYear(ACADEMIC_YEAR_CURRENT).deadlineDate(deadline).build();
        when(deadlineConfigRepository.findAll()).thenReturn(Collections.singletonList(config));

        // Act
        scheduler.sendDeadlineReminders();

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        // Check logs for "STAFF role not found" (conceptual for this test, actual log checking needs tools)
    }
    
    @Test
    void sendDeadlineReminders_MailSendException_LogsErrorAndContinues() {
        // Arrange
        LocalDate deadline = LocalDate.now().plusDays(1); // Reminder day for staffUser1 and staffUser3
        DeadlineConfig config = DeadlineConfig.builder().academicYear(ACADEMIC_YEAR_CURRENT).deadlineDate(deadline).build();
        when(deadlineConfigRepository.findAll()).thenReturn(Collections.singletonList(config));

        // Make mailSender throw an exception for the first user, but not the second
        doThrow(new RuntimeException("Test Mail Send Failure"))
            .doNothing() // Subsequent calls will succeed
            .when(mailSender).send(argThat(msg -> Arrays.asList(msg.getTo()).contains(staffUser1_notSubmitted.getEmail())));
            
        // Act
        scheduler.sendDeadlineReminders();

        // Assert
        // Email should have been attempted for both unsubmitted users (staffUser1, staffUser3)
        // One fails, one succeeds.
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class)); 
        // Check logs for "Failed to send reminder email to unsubmitted@example.com" (conceptual)
    }
}
