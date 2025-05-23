package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.appraisal.AppraisalFormDTO;
import com.cvrce.apraisal.dto.notification.NotificationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.Department; // If needed by User builder
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.enums.ReviewLevel;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.repo.AppraisalVersionRepository; // For verify(updateAppraisalStatus)
import com.cvrce.apraisal.service.NotificationService; 
import com.fasterxml.jackson.databind.ObjectMapper; // For verify(updateAppraisalStatus)


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.time.LocalDate; // For AppraisalFormDTO mapping if needed

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppraisalFormServiceImplTests {

    @Mock
    private AppraisalFormRepository formRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AppraisalVersionRepository versionRepository; // Needed by updateAppraisalStatus
    @Mock
    private ObjectMapper objectMapper; // Needed by updateAppraisalStatus

    // Using @Spy to test a method within the same class.
    // We want to verify that hodFinalizeCorrections calls this.updateAppraisalStatus correctly.
    // However, directly spying on @InjectMocks can be tricky.
    // A cleaner way is to extract the core logic of updateAppraisalStatus if it were complex,
    // or verify its side-effects (formRepository.save, versionRepository.save, notificationService.send).
    // For this test, we'll verify the arguments passed to the *dependencies* of updateAppraisalStatus
    // when it's called by hodFinalizeCorrections.
    @InjectMocks
    private AppraisalFormServiceImpl appraisalFormService;

    private UUID formId;
    private UUID hodUserId;
    private User hodUser;
    private AppraisalForm appraisalForm;
    private AppraisalFormDTO appraisalFormDTO; // For mapToDTO mocking

    @BeforeEach
    void setUp() {
        formId = UUID.randomUUID();
        hodUserId = UUID.randomUUID();

        hodUser = User.builder().id(hodUserId).fullName("Test HOD").build();
        // User needs a department for some service methods, mock if necessary
        // hodUser.setDepartment(Department.builder().id(UUID.randomUUID()).name("Test Dept").build());

        appraisalForm = AppraisalForm.builder()
                .id(formId)
                .status(AppraisalStatus.REUPLOAD_REQUIRED)
                .user(User.builder().id(UUID.randomUUID()).fullName("Staff Member").build()) // Staff user for notification
                .academicYear("2023-24")
                .build();
        
        // Mocking for mapToDTO used by updateAppraisalStatus
        appraisalFormDTO = new AppraisalFormDTO();
        appraisalFormDTO.setId(formId);
        // Populate other DTO fields if they are asserted or used in logs
    }

    private void setupMocksForUpdateAppraisalStatus() {
        // This simulates the behavior of the real updateAppraisalStatus's dependencies
        // It's called internally by hodFinalizeCorrections
        when(formRepository.save(any(AppraisalForm.class))).thenAnswer(inv -> inv.getArgument(0));
        // For mapToDTO, if it's complex. If simple, this might not be needed.
        // Let's assume mapToDTO is simple and doesn't need specific mocking beyond its return for now.
        // The actual test will be on what hodFinalizeCorrections passes to updateAppraisalStatus's deps.

        // Mocking dependencies of the *original* updateAppraisalStatus method
        // This is needed because hodFinalizeCorrections calls this.updateAppraisalStatus(...)
        // So we need to ensure that when updateAppraisalStatus is called, its own dependencies are mocked.
        // This setup is for the *internal* call to updateAppraisalStatus.
        lenient().when(userRepository.findById(eq(hodUserId))).thenReturn(Optional.of(hodUser)); // For changedByUser in updateAppraisalStatus
        lenient().when(objectMapper.writeValueAsString(any())).thenReturn("{\"snapshot\":\"dummy\"}");
    }


    @Test
    void hodFinalizeCorrections_Success_MovesToDepartmentReview() {
        // Arrange
        setupMocksForUpdateAppraisalStatus();
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(formRepository.findById(formId)).thenReturn(Optional.of(appraisalForm));
        // Assume mapToDTO will be called inside updateAppraisalStatus, prepare for it.
        // The actual DTO mapping logic is not tested here, just that it's called.

        // Act
        AppraisalFormDTO result = appraisalFormService.hodFinalizeCorrections(formId, hodUserId, ReviewLevel.DEPARTMENT_REVIEW);

        // Assert
        assertNotNull(result);
        
        ArgumentCaptor<AppraisalForm> formCaptor = ArgumentCaptor.forClass(AppraisalForm.class);
        verify(formRepository, times(1)).save(formCaptor.capture()); // one for the updateAppraisalStatus call
        assertEquals(AppraisalStatus.DEPARTMENT_REVIEW, formCaptor.getValue().getStatus());

        ArgumentCaptor<AppraisalVersion> versionCaptor = ArgumentCaptor.forClass(AppraisalVersion.class);
        verify(versionRepository, times(1)).save(versionCaptor.capture()); // from updateAppraisalStatus
        assertTrue(versionCaptor.getValue().getRemarks().contains("HOD Test HOD finalized corrections. Form moved to Department Review."));

        ArgumentCaptor<NotificationDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        // Expect two notifications: one to staff (from updateAppraisalStatus), one to HOD (from hodFinalizeCorrections)
        verify(notificationService, times(2)).sendNotification(notificationCaptor.capture());
        
        assertTrue(notificationCaptor.getAllValues().stream()
            .anyMatch(n -> n.getUserId().equals(appraisalForm.getUser().getId()) && 
                           n.getMessage().contains("has been updated to status: DEPARTMENT_REVIEW")));
        assertTrue(notificationCaptor.getAllValues().stream()
            .anyMatch(n -> n.getUserId().equals(hodUserId) && n.getTitle().equals("Corrections Finalized")));
    }
    
    @Test
    void hodFinalizeCorrections_Success_MovesToHodReview() {
        // Arrange
        setupMocksForUpdateAppraisalStatus();
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(formRepository.findById(formId)).thenReturn(Optional.of(appraisalForm));

        // Act
        appraisalFormService.hodFinalizeCorrections(formId, hodUserId, ReviewLevel.HOD_REVIEW);

        // Assert
        ArgumentCaptor<AppraisalForm> formCaptor = ArgumentCaptor.forClass(AppraisalForm.class);
        verify(formRepository).save(formCaptor.capture());
        assertEquals(AppraisalStatus.HOD_REVIEW, formCaptor.getValue().getStatus());
    }


    @Test
    void hodFinalizeCorrections_ThrowsIllegalStateException_WhenStatusNotReuploadRequired() {
        // Arrange
        appraisalForm.setStatus(AppraisalStatus.SUBMITTED); // Not REUPLOAD_REQUIRED
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(formRepository.findById(formId)).thenReturn(Optional.of(appraisalForm));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            appraisalFormService.hodFinalizeCorrections(formId, hodUserId, ReviewLevel.DEPARTMENT_REVIEW);
        });
        assertTrue(exception.getMessage().contains("Form must be in REUPLOAD_REQUIRED status"));
    }

    @Test
    void hodFinalizeCorrections_ThrowsIllegalArgumentException_ForInvalidRestartLevel() {
        // Arrange
        when(userRepository.findById(hodUserId)).thenReturn(Optional.of(hodUser));
        when(formRepository.findById(formId)).thenReturn(Optional.of(appraisalForm));

        // Act & Assert
        // Example of an invalid level not handled by the switch case in the service method
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            appraisalFormService.hodFinalizeCorrections(formId, hodUserId, ReviewLevel.PRINCIPAL_REVIEW); 
        });
        assertTrue(exception.getMessage().contains("Invalid restart review level: PRINCIPAL_REVIEW"));
    }
}
