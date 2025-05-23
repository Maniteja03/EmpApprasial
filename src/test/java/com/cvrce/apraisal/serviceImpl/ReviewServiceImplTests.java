package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.review.ReviewDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.Department;
import com.cvrce.apraisal.entity.review.Review;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.enums.ReviewDecision;
import com.cvrce.apraisal.enums.ReviewLevel;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.ReviewRepository;
import com.cvrce.apraisal.repo.ReviewerAssignmentRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.repo.AppraisalVersionRepository; // Added for completeness, though not directly used in this first test
import com.cvrce.apraisal.service.AppraisalFormService;
import com.cvrce.apraisal.service.NotificationService;
import com.cvrce.apraisal.dto.notification.NotificationDTO; // Required for mocking verify

import com.fasterxml.jackson.databind.ObjectMapper; // Added for completeness
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.Collections; // For empty list in aggregation tests later
import java.util.Arrays; // Added for Arrays.asList

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceImplTests {

    @Mock
    private ReviewRepository reviewRepo;
    @Mock
    private AppraisalFormRepository formRepo; // Though AppraisalFormService handles form updates
    @Mock
    private UserRepository userRepo;
    @Mock
    private ReviewerAssignmentRepository reviewerAssignmentRepository;
    @Mock
    private AppraisalFormService appraisalFormService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AppraisalVersionRepository versionRepo; // Mocked, though not directly verified in this first test
    @Mock
    private ObjectMapper objectMapper; // Mocked as it's a dependency

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testReviewer;
    private User testStaffUser;
    private AppraisalForm testAppraisalForm;
    private ReviewDTO testReviewDTO;
    private UUID formId;
    private UUID reviewerId;
    private UUID staffUserId;

    @BeforeEach
    void setUp() {
        formId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        staffUserId = UUID.randomUUID();

        testStaffUser = User.builder().id(staffUserId).fullName("Staff User").email("staff@example.com").department(new Department()).build();
        testReviewer = User.builder().id(reviewerId).fullName("Test Reviewer").email("reviewer@example.com").build();
        
        testAppraisalForm = AppraisalForm.builder()
                .id(formId)
                .user(testStaffUser)
                .academicYear("2023-24")
                .status(AppraisalStatus.DEPARTMENT_REVIEW) // Initial status for some tests
                .build();

        testReviewDTO = new ReviewDTO();
        testReviewDTO.setAppraisalFormId(formId);
        testReviewDTO.setReviewerId(reviewerId);
        testReviewDTO.setRemarks("Test remarks");
    }

    @Test
    void testSubmitReview_HodApprove_UpdatesStatusToHodApproved() {
        // Arrange
        testReviewDTO.setLevel(ReviewLevel.HOD_REVIEW.name());
        testReviewDTO.setDecision(ReviewDecision.APPROVE.name());
        testAppraisalForm.setStatus(AppraisalStatus.HOD_REVIEW); // Form is ready for HOD review

        when(userRepo.findById(reviewerId)).thenReturn(Optional.of(testReviewer));
        when(formRepo.findById(formId)).thenReturn(Optional.of(testAppraisalForm));
        when(reviewRepo.findByReviewerAndAppraisalForm(testReviewer, testAppraisalForm)).thenReturn(Optional.empty());
        // reviewRepo.save(any(Review.class)) will be called, mock it to return the saved entity
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewDTO result = reviewService.submitReview(testReviewDTO);

        // Assert
        assertNotNull(result);
        // Verify that appraisalFormService.updateAppraisalStatus was called with the correct new status
        verify(appraisalFormService).updateAppraisalStatus(
                eq(formId),
                eq(AppraisalStatus.HOD_APPROVED),
                anyString(), // Remark can be flexible for this test
                eq(reviewerId)
        );
        // Verify no notifications sent to staff for HOD_APPROVED in this specific interaction (only for REUPLOAD/COMPLETED)
        verify(notificationService, never()).sendNotification(argThat(dto -> dto.getUserId().equals(staffUserId)));
    }

    // Test 1: Department Committee member submits REUPLOAD
    @Test
    void testSubmitReview_DeptCommitteeReupload_UpdatesStatusToReuploadRequiredAndNotifiesStaff() {
        // Arrange
        testReviewDTO.setLevel(ReviewLevel.DEPARTMENT_REVIEW.name());
        testReviewDTO.setDecision(ReviewDecision.REUPLOAD.name());
        testAppraisalForm.setStatus(AppraisalStatus.DEPARTMENT_REVIEW);

        when(userRepo.findById(reviewerId)).thenReturn(Optional.of(testReviewer));
        when(formRepo.findById(formId)).thenReturn(Optional.of(testAppraisalForm));
        when(reviewRepo.findByReviewerAndAppraisalForm(testReviewer, testAppraisalForm)).thenReturn(Optional.empty());
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ReviewDTO result = reviewService.submitReview(testReviewDTO);

        // Assert
        assertNotNull(result);
        verify(appraisalFormService).updateAppraisalStatus(
                eq(formId),
                eq(AppraisalStatus.REUPLOAD_REQUIRED),
                anyString(),
                eq(reviewerId)
        );
        // Verify notification to staff
        verify(notificationService).sendNotification(argThat(dto ->
            dto.getUserId().equals(staffUserId) &&
            dto.getTitle().equals("Appraisal Form Requires Re-upload")
        ));
    }

    // Test 2: Department Committee - all members approve
    @Test
    void testSubmitReview_DeptCommitteeAllApprove_UpdatesStatusToHodReview() {
        // Arrange
        testReviewDTO.setLevel(ReviewLevel.DEPARTMENT_REVIEW.name());
        testReviewDTO.setDecision(ReviewDecision.APPROVE.name());
        testAppraisalForm.setStatus(AppraisalStatus.DEPARTMENT_REVIEW);

        // Mock current reviewer and form
        when(userRepo.findById(reviewerId)).thenReturn(Optional.of(testReviewer));
        when(formRepo.findById(formId)).thenReturn(Optional.of(testAppraisalForm));
        when(reviewRepo.findByReviewerAndAppraisalForm(testReviewer, testAppraisalForm)).thenReturn(Optional.empty());
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Mock repository calls for aggregation logic
        // Assume 2 members assigned, this is the second approval, no reuploads
        when(reviewerAssignmentRepository.countByAppraisalFormId(formId)).thenReturn(2L);
        
        Review review1 = Review.builder().reviewer(User.builder().id(UUID.randomUUID()).build()).decision(ReviewDecision.APPROVE).level(ReviewLevel.DEPARTMENT_REVIEW).build();
        // The current review being submitted will be the second one.
        // So, allReviewsAtLevel should simulate one existing approval, and the current one will be added by the service method itself.
        // The actual Review object saved by reviewRepo.save() will be used by the service, so we don't need to explicitly add it to this list.
        // However, the count of approvals will be based on what's *already* in the DB for that level + the current one.
        // For simplicity, let's mock findByAppraisalFormIdAndLevel to return a list that, combined with the current approval, meets the criteria.
        
        // Let's refine: the service calls findByAppraisalFormIdAndLevel *after* saving the current review.
        // So, the list returned should include the current review as if it was already persisted by the time of this call.
        Review currentReviewEntity = Review.builder()
                                        .reviewer(testReviewer)
                                        .appraisalForm(testAppraisalForm)
                                        .decision(ReviewDecision.APPROVE)
                                        .level(ReviewLevel.DEPARTMENT_REVIEW)
                                        .build();
        when(reviewRepo.findByAppraisalFormIdAndLevel(formId, ReviewLevel.DEPARTMENT_REVIEW))
            .thenReturn(java.util.Arrays.asList(review1, currentReviewEntity));


        // Act
        ReviewDTO result = reviewService.submitReview(testReviewDTO);

        // Assert
        assertNotNull(result);
        verify(appraisalFormService).updateAppraisalStatus(
                eq(formId),
                eq(AppraisalStatus.HOD_REVIEW), // Should move to HOD_REVIEW
                anyString(),
                eq(reviewerId)
        );
    }

    // Test 3: Principal approves - completes the form and notifies staff
    @Test
    void testSubmitReview_PrincipalApprove_UpdatesStatusToCompletedAndNotifiesStaff() {
        // Arrange
        testReviewDTO.setLevel(ReviewLevel.PRINCIPAL_REVIEW.name());
        testReviewDTO.setDecision(ReviewDecision.APPROVE.name());
        testAppraisalForm.setStatus(AppraisalStatus.PENDING_PRINCIPAL_APPROVAL);

        when(userRepo.findById(reviewerId)).thenReturn(Optional.of(testReviewer)); // Reviewer is Principal here
        when(formRepo.findById(formId)).thenReturn(Optional.of(testAppraisalForm));
        when(reviewRepo.findByReviewerAndAppraisalForm(testReviewer, testAppraisalForm)).thenReturn(Optional.empty());
        when(reviewRepo.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        ReviewDTO result = reviewService.submitReview(testReviewDTO);

        // Assert
        assertNotNull(result);
        verify(appraisalFormService).updateAppraisalStatus(
                eq(formId),
                eq(AppraisalStatus.COMPLETED),
                anyString(),
                eq(reviewerId)
        );
        // Verify notification to staff
        verify(notificationService).sendNotification(argThat(dto ->
            dto.getUserId().equals(staffUserId) &&
            dto.getTitle().equals("Appraisal Form Approved")
        ));
    }
}
