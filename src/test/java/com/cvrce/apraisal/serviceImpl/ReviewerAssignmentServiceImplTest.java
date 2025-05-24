package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.notification.NotificationDTO;
import com.cvrce.apraisal.dto.review.ReviewerAssignmentDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.Department;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.review.ReviewerAssignment;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.ReviewerAssignmentRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.AppraisalFormService;
import com.cvrce.apraisal.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReviewerAssignmentServiceImplTest {

    @Mock
    private ReviewerAssignmentRepository assignmentRepo;

    @Mock
    private AppraisalFormRepository formRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private AppraisalFormService appraisalFormService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReviewerAssignmentServiceImpl reviewerAssignmentService;

    private Department deptA;
    private Department deptB;
    private User submitterDeptA;
    private User member1DeptA;
    private User member2DeptA;
    private User member1DeptB;
    private User assignerUser;
    private AppraisalForm formFromDeptAUser;
    private UUID formId;
    private UUID assignerId;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        deptA = Department.builder().id(1L).name("Department A").build();
        deptB = Department.builder().id(2L).name("Department B").build();

        submitterDeptA = User.builder().id(UUID.randomUUID()).fullName("Submitter User").department(deptA).employeeId("S001").build();
        formFromDeptAUser = AppraisalForm.builder().id(UUID.randomUUID()).user(submitterDeptA).academicYear("2023-24").status(AppraisalStatus.SUBMITTED).build();
        formId = formFromDeptAUser.getId();

        member1DeptA = User.builder().id(UUID.randomUUID()).fullName("Member One Dept A").department(deptA).employeeId("M001A").build();
        member2DeptA = User.builder().id(UUID.randomUUID()).fullName("Member Two Dept A").department(deptA).employeeId("M002A").build();
        member1DeptB = User.builder().id(UUID.randomUUID()).fullName("Member One Dept B").department(deptB).employeeId("M001B").build();

        assignerUser = User.builder().id(UUID.randomUUID()).fullName("HOD User").department(deptA).employeeId("HOD001").build();
        assignerId = assignerUser.getId();

        // Common repository mocks
        when(formRepo.findById(formId)).thenReturn(Optional.of(formFromDeptAUser));
        when(userRepo.findById(assignerId)).thenReturn(Optional.of(assignerUser));
        when(userRepo.findById(submitterDeptA.getId())).thenReturn(Optional.of(submitterDeptA)); // for form.getUser()
    }

    @Test
    void testAssignToDepartmentCommittee_ValidAssignment_ShouldSucceed() {
        // Arrange
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId(), member2DeptA.getId());

        when(userRepo.findById(member1DeptA.getId())).thenReturn(Optional.of(member1DeptA));
        when(userRepo.findById(member2DeptA.getId())).thenReturn(Optional.of(member2DeptA));
        when(assignmentRepo.save(any(ReviewerAssignment.class))).thenAnswer(invocation -> {
            ReviewerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID()); // Mock saving by assigning an ID
            return assignment;
        });

        // Act
        List<ReviewerAssignmentDTO> result = reviewerAssignmentService.assignToDepartmentCommittee(formId, memberIds, assignerId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(member1DeptA.getId(), result.get(0).getReviewerId());
        assertEquals(member2DeptA.getId(), result.get(1).getReviewerId());

        verify(assignmentRepo, times(2)).save(any(ReviewerAssignment.class));
        verify(appraisalFormService).updateAppraisalStatus(eq(formId), eq(AppraisalStatus.DEPARTMENT_REVIEW), anyString(), eq(assignerId));
        verify(notificationService, times(2)).sendNotification(any(NotificationDTO.class));
    }

    @Test
    void testAssignToDepartmentCommittee_InvalidAssignment_DifferentDepartment_ShouldThrowIllegalArgumentException() {
        // Arrange
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId(), member1DeptB.getId()); // member1DeptB is from Dept B

        when(userRepo.findById(member1DeptA.getId())).thenReturn(Optional.of(member1DeptA));
        when(userRepo.findById(member1DeptB.getId())).thenReturn(Optional.of(member1DeptB));
        
        // Mock saving for the first member, as it's valid and processed before the invalid one
        when(assignmentRepo.save(argThat(ra -> ra.getReviewer().equals(member1DeptA)))).thenAnswer(invocation -> {
            ReviewerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID()); 
            return assignment;
        });

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reviewerAssignmentService.assignToDepartmentCommittee(formId, memberIds, assignerId);
        });

        assertTrue(exception.getMessage().contains(member1DeptB.getFullName()));
        assertTrue(exception.getMessage().contains(member1DeptB.getEmployeeId()));
        assertTrue(exception.getMessage().contains(deptA.getName())); // Expected department name

        // Verify save was called for the valid member before the exception
        verify(assignmentRepo, times(1)).save(argThat(ra -> ra.getReviewer().equals(member1DeptA)));
        // Verify save was NOT called for the invalid member
        verify(assignmentRepo, never()).save(argThat(ra -> ra.getReviewer().equals(member1DeptB)));
        
        // Verify status update and notifications are NOT called because the transaction should fail
        verify(appraisalFormService, never()).updateAppraisalStatus(any(), any(), anyString(), any());
        verify(notificationService, never()).sendNotification(any(NotificationDTO.class));
    }

    @Test
    void testAssignToDepartmentCommittee_SubmitterDepartmentNull_ShouldThrowIllegalStateException() {
        // Arrange
        User submitterWithNullDept = User.builder().id(UUID.randomUUID()).fullName("Null Dept Submitter").department(null).build();
        AppraisalForm formWithNullDeptSubmitter = AppraisalForm.builder().id(UUID.randomUUID()).user(submitterWithNullDept).build();
        UUID nullDeptFormId = formWithNullDeptSubmitter.getId();
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId());

        when(formRepo.findById(nullDeptFormId)).thenReturn(Optional.of(formWithNullDeptSubmitter));
        // No need to mock userRepo for members as it should fail before that

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reviewerAssignmentService.assignToDepartmentCommittee(nullDeptFormId, memberIds, assignerId);
        });

        assertTrue(exception.getMessage().contains("Department not found for submitting user"));
        assertTrue(exception.getMessage().contains(submitterWithNullDept.getFullName()));
        verify(assignmentRepo, never()).save(any());
    }
    
    @Test
    void testAssignToDepartmentCommittee_FormSubmitterNull_ShouldThrowIllegalStateException() {
        // Arrange
        AppraisalForm formWithNullSubmitter = AppraisalForm.builder().id(UUID.randomUUID()).user(null).build();
        UUID nullSubmitterFormId = formWithNullSubmitter.getId();
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId());

        when(formRepo.findById(nullSubmitterFormId)).thenReturn(Optional.of(formWithNullSubmitter));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reviewerAssignmentService.assignToDepartmentCommittee(nullSubmitterFormId, memberIds, assignerId);
        });

        assertTrue(exception.getMessage().contains("Submitting user not found for form"));
        verify(assignmentRepo, never()).save(any());
    }


    @Test
    void testAssignToDepartmentCommittee_CommitteeMemberDepartmentNull_ShouldThrowIllegalStateException() {
        // Arrange
        User memberWithNullDept = User.builder().id(UUID.randomUUID()).fullName("Null Dept Member").department(null).employeeId("MNULL").build();
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId(), memberWithNullDept.getId());

        when(userRepo.findById(member1DeptA.getId())).thenReturn(Optional.of(member1DeptA));
        when(userRepo.findById(memberWithNullDept.getId())).thenReturn(Optional.of(memberWithNullDept));
        
        when(assignmentRepo.save(argThat(ra -> ra.getReviewer().equals(member1DeptA)))).thenAnswer(invocation -> {
            ReviewerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });


        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reviewerAssignmentService.assignToDepartmentCommittee(formId, memberIds, assignerId);
        });

        assertTrue(exception.getMessage().contains("Department not found for committee member"));
        assertTrue(exception.getMessage().contains(memberWithNullDept.getFullName()));
        
        // Verify save was called for the valid member before the exception
        verify(assignmentRepo, times(1)).save(argThat(ra -> ra.getReviewer().equals(member1DeptA)));
        verify(assignmentRepo, never()).save(argThat(ra -> ra.getReviewer().equals(memberWithNullDept)));
        verify(appraisalFormService, never()).updateAppraisalStatus(any(), any(), anyString(), any());
        verify(notificationService, never()).sendNotification(any(NotificationDTO.class));
    }

    @Test
    void testAssignToDepartmentCommittee_FormNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        UUID nonExistentFormId = UUID.randomUUID();
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId());
        when(formRepo.findById(nonExistentFormId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewerAssignmentService.assignToDepartmentCommittee(nonExistentFormId, memberIds, assignerId);
        });
        assertEquals("Form not found: " + nonExistentFormId, exception.getMessage());
        verify(assignmentRepo, never()).save(any());
    }

    @Test
    void testAssignToDepartmentCommittee_AssignerNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        UUID nonExistentAssignerId = UUID.randomUUID();
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId());
        when(userRepo.findById(nonExistentAssignerId)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewerAssignmentService.assignToDepartmentCommittee(formId, memberIds, nonExistentAssignerId);
        });
        assertEquals("Assigner User not found: " + nonExistentAssignerId, exception.getMessage());
        verify(assignmentRepo, never()).save(any());
    }

    @Test
    void testAssignToDepartmentCommittee_CommitteeMemberNotFound_ShouldThrowResourceNotFoundException() {
        // Arrange
        UUID nonExistentMemberId = UUID.randomUUID();
        List<UUID> memberIds = Arrays.asList(member1DeptA.getId(), nonExistentMemberId);

        when(userRepo.findById(member1DeptA.getId())).thenReturn(Optional.of(member1DeptA));
        when(userRepo.findById(nonExistentMemberId)).thenReturn(Optional.empty());
         when(assignmentRepo.save(argThat(ra -> ra.getReviewer().equals(member1DeptA)))).thenAnswer(invocation -> {
            ReviewerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });


        // Act & Assert
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            reviewerAssignmentService.assignToDepartmentCommittee(formId, memberIds, assignerId);
        });
        assertEquals("Committee member User not found: " + nonExistentMemberId, exception.getMessage());
        
        // Verify save was called for the valid member before the exception
        verify(assignmentRepo, times(1)).save(argThat(ra -> ra.getReviewer().equals(member1DeptA)));
        verify(appraisalFormService, never()).updateAppraisalStatus(any(), any(), anyString(), any());
    }

    // --- Tests for assignToCollegeCommittee ---

    @Test
    void testAssignToCollegeCommittee_ValidAssignment_DifferentDepartments_ShouldSucceed() {
        // Arrange
        // Submitter is from deptA (formFromDeptAUser)
        User member1DeptB = User.builder().id(UUID.randomUUID()).fullName("Member One Dept B").department(deptB).employeeId("M001B").build();
        User member2DeptB = User.builder().id(UUID.randomUUID()).fullName("Member Two Dept B").department(deptB).employeeId("M002B").build();
        List<UUID> memberIds = Arrays.asList(member1DeptB.getId(), member2DeptB.getId());

        when(userRepo.findById(member1DeptB.getId())).thenReturn(Optional.of(member1DeptB));
        when(userRepo.findById(member2DeptB.getId())).thenReturn(Optional.of(member2DeptB));
        when(assignmentRepo.save(any(ReviewerAssignment.class))).thenAnswer(invocation -> {
            ReviewerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID()); // Mock saving by assigning an ID
            return assignment;
        });

        // Act
        List<ReviewerAssignmentDTO> result = reviewerAssignmentService.assignToCollegeCommittee(formId, memberIds, assignerId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(member1DeptB.getId(), result.get(0).getReviewerId());
        assertEquals(member2DeptB.getId(), result.get(1).getReviewerId());

        verify(assignmentRepo, times(2)).save(any(ReviewerAssignment.class));
        verify(appraisalFormService).updateAppraisalStatus(eq(formId), eq(AppraisalStatus.COLLEGE_REVIEW), anyString(), eq(assignerId));
        verify(notificationService, times(2)).sendNotification(any(NotificationDTO.class));
    }

    @Test
    void testAssignToCollegeCommittee_InvalidAssignment_SameDepartment_ShouldThrowIllegalArgumentException() {
        // Arrange
        // Submitter is from deptA (formFromDeptAUser)
        // member1DeptB is from Dept B (valid)
        // member1DeptA is from Dept A (invalid, same as submitter)
        List<UUID> memberIds = Arrays.asList(member1DeptB.getId(), member1DeptA.getId());

        when(userRepo.findById(member1DeptB.getId())).thenReturn(Optional.of(member1DeptB));
        when(userRepo.findById(member1DeptA.getId())).thenReturn(Optional.of(member1DeptA));

        // Mock saving for the first member (member1DeptB), as it's valid and processed before the invalid one
        when(assignmentRepo.save(argThat(ra -> ra.getReviewer().equals(member1DeptB)))).thenAnswer(invocation -> {
            ReviewerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            reviewerAssignmentService.assignToCollegeCommittee(formId, memberIds, assignerId);
        });

        assertTrue(exception.getMessage().contains("College Committee member " + member1DeptA.getFullName() + " (" + member1DeptA.getEmployeeId() + ") must be from a different department"));
        assertTrue(exception.getMessage().contains(submitterDeptA.getDepartment().getName())); // submitter's department name

        // Verify save was called for the valid member (member1DeptB) before the exception
        verify(assignmentRepo, times(1)).save(argThat(ra -> ra.getReviewer().equals(member1DeptB)));
        // Verify save was NOT called for the invalid member (member1DeptA)
        verify(assignmentRepo, never()).save(argThat(ra -> ra.getReviewer().equals(member1DeptA)));

        // Verify status update and notifications are NOT called because the transaction should fail
        verify(appraisalFormService, never()).updateAppraisalStatus(any(), any(), anyString(), any());
        verify(notificationService, never()).sendNotification(any(NotificationDTO.class));
    }
    
    @Test
    void testAssignToCollegeCommittee_SubmitterDepartmentNull_ShouldThrowIllegalStateException() {
        // Arrange
        User submitterWithNullDept = User.builder().id(UUID.randomUUID()).fullName("Null Dept Submitter").department(null).build();
        AppraisalForm formWithNullDeptSubmitter = AppraisalForm.builder().id(UUID.randomUUID()).user(submitterWithNullDept).build();
        UUID nullDeptFormId = formWithNullDeptSubmitter.getId();
        List<UUID> memberIds = Arrays.asList(member1DeptB.getId()); // member1DeptB is from a different dept

        when(formRepo.findById(nullDeptFormId)).thenReturn(Optional.of(formWithNullDeptSubmitter));
        when(userRepo.findById(member1DeptB.getId())).thenReturn(Optional.of(member1DeptB));
        // No need to mock assigner user separately if it fails before that point.
        // Ensure assigner is mocked if it's fetched before the submitter's department check.
        // Based on current impl, assigner is fetched before submitter user's department.
        User assignerForThisTest = User.builder().id(UUID.randomUUID()).fullName("Assigner").build();
        when(userRepo.findById(assignerForThisTest.getId())).thenReturn(Optional.of(assignerForThisTest));


        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reviewerAssignmentService.assignToCollegeCommittee(nullDeptFormId, memberIds, assignerForThisTest.getId());
        });

        assertTrue(exception.getMessage().contains("Department not found for submitting user"));
        assertTrue(exception.getMessage().contains(submitterWithNullDept.getFullName()));
        verify(assignmentRepo, never()).save(any());
    }

    @Test
    void testAssignToCollegeCommittee_CommitteeMemberDepartmentNull_ShouldThrowIllegalStateException() {
        // Arrange
        // Submitter is from deptA (formFromDeptAUser)
        User memberWithNullDept = User.builder().id(UUID.randomUUID()).fullName("Null Dept Member").department(null).employeeId("MNULL").build();
        // member1DeptB is from Dept B (valid, different from submitter's Dept A)
        List<UUID> memberIds = Arrays.asList(member1DeptB.getId(), memberWithNullDept.getId());

        when(userRepo.findById(member1DeptB.getId())).thenReturn(Optional.of(member1DeptB));
        when(userRepo.findById(memberWithNullDept.getId())).thenReturn(Optional.of(memberWithNullDept));

        // Mock saving for the first member (member1DeptB), as it's valid and processed before the invalid one
        when(assignmentRepo.save(argThat(ra -> ra.getReviewer().equals(member1DeptB)))).thenAnswer(invocation -> {
            ReviewerAssignment assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            reviewerAssignmentService.assignToCollegeCommittee(formId, memberIds, assignerId);
        });

        assertTrue(exception.getMessage().contains("Department not found for committee member"));
        assertTrue(exception.getMessage().contains(memberWithNullDept.getFullName()));

        // Verify save was called for the valid member (member1DeptB) before the exception
        verify(assignmentRepo, times(1)).save(argThat(ra -> ra.getReviewer().equals(member1DeptB)));
        verify(assignmentRepo, never()).save(argThat(ra -> ra.getReviewer().equals(memberWithNullDept)));
        verify(appraisalFormService, never()).updateAppraisalStatus(any(), any(), anyString(), any());
        verify(notificationService, never()).sendNotification(any(NotificationDTO.class));
    }
}
