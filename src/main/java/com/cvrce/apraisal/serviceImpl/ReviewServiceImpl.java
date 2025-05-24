package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.review.ReviewDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.AppraisalVersion;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.review.Review;
import com.cvrce.apraisal.enums.ReviewDecision;
import com.cvrce.apraisal.enums.ReviewLevel;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.AppraisalVersionRepository;
import com.cvrce.apraisal.repo.ReviewRepository;
import com.cvrce.apraisal.repo.ReviewerAssignmentRepository; // Added import
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.AppraisalFormService; // Added import
import com.cvrce.apraisal.service.NotificationService; // Added
import com.cvrce.apraisal.dto.notification.NotificationDTO; // Added
import com.cvrce.apraisal.service.ReviewService;
import com.cvrce.apraisal.enums.AppraisalStatus; // Added import
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepo;
    private final AppraisalFormRepository formRepo;
    private final AppraisalVersionRepository versionRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;
    private final AppraisalFormService appraisalFormService; // Added AppraisalFormService
    private final ReviewerAssignmentRepository reviewerAssignmentRepository; // Added
    private final NotificationService notificationService; // Added
    private final ReviewerAssignmentService reviewerAssignmentService; // Added

    @Override
    @Transactional
    public ReviewDTO submitReview(ReviewDTO dto) {
        User reviewer = userRepo.findById(dto.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        Optional<Review> existing = reviewRepo.findByReviewerAndAppraisalForm(reviewer, form);
        if (existing.isPresent()) {
            throw new IllegalStateException("Reviewer already reviewed this form");
        }

        Review review = Review.builder()
                .reviewer(reviewer)
                .appraisalForm(form)
                .decision(ReviewDecision.valueOf(dto.getDecision()))
                .remarks(dto.getRemarks())
                .level(ReviewLevel.valueOf(dto.getLevel()))
                .reviewedAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepo.save(review);

        // Workflow logic starts
        AppraisalStatus newStatus = null;
        String reviewRemarksForStatusUpdate = "Review submitted at level " + review.getLevel() + 
                                         " with decision " + review.getDecision() + 
                                         " by " + reviewer.getFullName() + "." +
                                         (review.getRemarks() != null && !review.getRemarks().isEmpty() ? " Reviewer Remarks: " + review.getRemarks() : "");

        switch (review.getLevel()) {
            case DEPARTMENT_REVIEW:
                if (review.getDecision() == ReviewDecision.REUPLOAD) {
                    newStatus = AppraisalStatus.REUPLOAD_REQUIRED;
                    reviewRemarksForStatusUpdate = "Form requires re-upload based on Department Committee review by " + reviewer.getFullName() + 
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? ". Remarks: " + review.getRemarks() : "");
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                    // Check if all assigned department committee members for this form have approved
                    long totalAssigned = reviewerAssignmentRepository.countByAppraisalFormId(form.getId()); 
                    // This count needs to be specific to "Department Committee" members.
                    // Assuming all assignments for a form are for the current active review level's committee.
                    // This might need refinement if a form can have assignments for multiple committee types simultaneously.
                    // For now, let's assume ReviewerAssignments are cleaned up or specific to the active phase.
                    // A more robust way would be to link ReviewerAssignment to a ReviewLevel or Role type.
                    // Let's assume for now: count all assignments on the form as being part of the current committee.

                    List<Review> allReviewsAtLevel = reviewRepo.findByAppraisalFormIdAndLevel(form.getId(), review.getLevel());
                    long approvedCount = allReviewsAtLevel.stream().filter(r -> r.getDecision() == ReviewDecision.APPROVE).count();
                    boolean anyReupload = allReviewsAtLevel.stream().anyMatch(r -> r.getDecision() == ReviewDecision.REUPLOAD);

                    if (!anyReupload && approvedCount == totalAssigned) {
                        newStatus = AppraisalStatus.HOD_REVIEW; // All approved, HOD needs to act
                        reviewRemarksForStatusUpdate = "Department Committee has collectively approved. Pending HOD action.";
                    } else {
                        // Not all have reviewed, or not all approved, or some reupload exists (which should have been caught above)
                        // No status change yet from this individual approval if aggregation conditions not met.
                        // The form remains in DEPARTMENT_REVIEW.
                    }
                }
                break;

            case HOD_REVIEW:
                if (review.getDecision() == ReviewDecision.REUPLOAD) {
                    newStatus = AppraisalStatus.REUPLOAD_REQUIRED; // To be handled by staff
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                    // HOD approves their department's review, ready for Chairperson
                    newStatus = AppraisalStatus.HOD_APPROVED;
                    reviewRemarksForStatusUpdate = "Approved by HOD " + reviewer.getFullName() + "." +
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? " HOD Remarks: " + review.getRemarks() : "");
                } else if (review.getDecision() == ReviewDecision.FORWARD) {
                    UUID verifyingStaffUserId = dto.getVerifyingStaffUserId();
                    if (verifyingStaffUserId == null) {
                        throw new IllegalArgumentException("Verifying Staff User ID must be provided when forwarding for verification by HOD.");
                    }
                    User verifyingStaffUser = userRepo.findById(verifyingStaffUserId)
                            .orElseThrow(() -> new ResourceNotFoundException("Chosen Verifying Staff User not found with ID: " + verifyingStaffUserId));

                    newStatus = AppraisalStatus.PENDING_VERIFICATION;
                    
                    String hodRemarks = review.getRemarks() != null && !review.getRemarks().isEmpty() ? " HOD Remarks: " + review.getRemarks() : "";
                    reviewRemarksForStatusUpdate = "Forwarded for verification by HOD " + reviewer.getFullName() + 
                                                   " to Verifying Staff " + verifyingStaffUser.getFullName() + 
                                                   (verifyingStaffUser.getEmployeeId() != null ? " (ID: " + verifyingStaffUser.getEmployeeId() + ")" : "") + "." +
                                                   hodRemarks;
                    try {
                        reviewerAssignmentService.assignToUserForReview(form.getId(), verifyingStaffUserId, ReviewLevel.VERIFYING_STAFF_REVIEW, reviewer.getId());
                        log.info("Form {} assigned to Verifying Staff {} by HOD {}", form.getId(), verifyingStaffUserId, reviewer.getId());
                    } catch (Exception e) {
                        log.error("Failed to assign form {} to Verifying Staff {}: {}", form.getId(), verifyingStaffUserId, e.getMessage(), e);
                        throw new IllegalStateException("Failed to assign form to Verifying Staff. Please try again or contact support. Form ID: " + form.getId(), e);
                    }
                }
                break;

            case VERIFYING_STAFF_REVIEW: // New Level
                if (review.getDecision() == ReviewDecision.REUPLOAD) {
                    newStatus = AppraisalStatus.RETURNED_TO_HOD; // Verifying staff sends back to HOD
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                    newStatus = AppraisalStatus.HOD_APPROVED; // Verification successful, effectively HOD approved to move to Chairperson
                }
                break;
            
            case COLLEGE_COMMITTEE_REVIEW:
                if (review.getDecision() == ReviewDecision.REUPLOAD) {
                    newStatus = AppraisalStatus.RETURNED_TO_CHAIRPERSON;
                    reviewRemarksForStatusUpdate = "Form returned to Chairperson based on College Committee review by " + reviewer.getFullName() +
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? ". Remarks: " + review.getRemarks() : "");
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                    // Check if all assigned college committee members for this form have approved
                    long totalAssigned = reviewerAssignmentRepository.countByAppraisalFormId(form.getId()); // See note above for DEPARTMENT_REVIEW
                    List<Review> allReviewsAtLevel = reviewRepo.findByAppraisalFormIdAndLevel(form.getId(), review.getLevel());
                    long approvedCount = allReviewsAtLevel.stream().filter(r -> r.getDecision() == ReviewDecision.APPROVE).count();
                    boolean anyReupload = allReviewsAtLevel.stream().anyMatch(r -> r.getDecision() == ReviewDecision.REUPLOAD);

                    if (!anyReupload && approvedCount == totalAssigned) {
                        newStatus = AppraisalStatus.CHAIR_REVIEW; // All approved, Chairperson needs to act
                        reviewRemarksForStatusUpdate = "College Committee has collectively approved. Pending Chairperson action.";
                    } else {
                        // No status change yet from this individual approval if aggregation conditions not met.
                        // The form remains in COLLEGE_REVIEW.
                    }
                }
                break;

            case CHAIRPERSON_REVIEW:
                if (review.getDecision() == ReviewDecision.REUPLOAD) {
                    newStatus = AppraisalStatus.RETURNED_TO_HOD; // Chairperson sends back to HOD
                    reviewRemarksForStatusUpdate = "Returned to HOD by Chairperson " + reviewer.getFullName() +
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? ". Remarks: " + review.getRemarks() : "");
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                    newStatus = AppraisalStatus.PENDING_PRINCIPAL_APPROVAL;
                    reviewRemarksForStatusUpdate = "Approved by Chairperson " + reviewer.getFullName() + ". Pending Principal Approval." +
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? " Chairperson Remarks: " + review.getRemarks() : "");
                } else if (review.getDecision() == ReviewDecision.FORWARD) {
                    // Chairperson forwards to Principal
                    newStatus = AppraisalStatus.PENDING_PRINCIPAL_APPROVAL;
                    reviewRemarksForStatusUpdate = "Forwarded to Principal by Chairperson " + reviewer.getFullName() + "." +
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? " Chairperson Remarks: " + review.getRemarks() : "");
                }
                break;

            case PRINCIPAL_REVIEW: // New Level
                if (review.getDecision() == ReviewDecision.REUPLOAD) {
                    newStatus = AppraisalStatus.RETURNED_TO_CHAIRPERSON; // Principal sends back to Chairperson
                    reviewRemarksForStatusUpdate = "Form returned to Chairperson by Principal " + reviewer.getFullName() +
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? ". Principal's Remarks: " + review.getRemarks() : ".");
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                    newStatus = AppraisalStatus.COMPLETED; // Final approval
                    reviewRemarksForStatusUpdate = "Form approved and completed by Principal " + reviewer.getFullName() +
                                                   (review.getRemarks() != null && !review.getRemarks().isEmpty() ? ". Principal's Remarks: " + review.getRemarks() : ".");
                }
                break;
        }
        
        // If Chairperson approved or forwarded, assign to Principal
        if (review.getLevel() == ReviewLevel.CHAIRPERSON_REVIEW && 
            newStatus == AppraisalStatus.PENDING_PRINCIPAL_APPROVAL) {
            
            User principalUser = userRepo.findFirstByRoles_NameIgnoreCase("PRINCIPAL")
                .orElseThrow(() -> new ResourceNotFoundException("Principal user with role 'PRINCIPAL' not found. Cannot assign form."));
            
            try {
                reviewerAssignmentService.assignToUserForReview(form.getId(), principalUser.getId(), ReviewLevel.PRINCIPAL_REVIEW, reviewer.getId());
                log.info("Form {} assigned to Principal {} for review by Chairperson {}", form.getId(), principalUser.getId(), reviewer.getId());
            } catch (Exception e) {
                log.error("Error assigning form {} to Principal {}: {}", form.getId(), principalUser.getId(), e.getMessage(), e);
                // Depending on policy, we might want to re-throw or handle this so the status update below doesn't happen
                // For now, logging and continuing, but this could leave the form in PENDING_PRINCIPAL_APPROVAL without an assignment.
                // Consider throwing a specific runtime exception to ensure transaction rollback if assignment fails.
                throw new IllegalStateException("Failed to assign form to Principal after Chairperson approval/forwarding. Form ID: " + form.getId(), e);
            }
        }


        if (newStatus != null && newStatus != form.getStatus()) { // Only update if status is actually changing
            appraisalFormService.updateAppraisalStatus(form.getId(), newStatus, reviewRemarksForStatusUpdate, reviewer.getId());
            log.info("AppraisalForm {} status updated to {} due to review by {}", form.getId(), newStatus, reviewer.getId());

            // Send notification to the staff member if status is critical for them
            String notificationTitle = null;
            String notificationMessage = null;

            if (newStatus == AppraisalStatus.REUPLOAD_REQUIRED) {
                notificationTitle = "Action Required on Your Appraisal Form";
                notificationMessage = "Your appraisal form (ID: " + form.getId() + ") for academic year " + form.getAcademicYear() + 
                                      " requires corrections. Please contact your HOD to discuss and have the necessary changes made in the system. Reviewer remarks: " + 
                                      (review.getRemarks() != null && !review.getRemarks().isEmpty() ? review.getRemarks() : "No specific remarks provided by reviewer.");
                
                NotificationDTO staffNotificationForReupload = NotificationDTO.builder()
                    .userId(form.getUser().getId()).title(notificationTitle).message(notificationMessage).build();
                try {
                    notificationService.sendNotification(staffNotificationForReupload);
                    log.info("Sent '{}' notification to staff {} for form {}", notificationTitle, form.getUser().getId(), form.getId());
                } catch (Exception e) {
                    log.error("Failed to send '{}' notification to staff {}: {}", notificationTitle, form.getUser().getId(), e.getMessage(), e);
                }

            } else if (newStatus == AppraisalStatus.COMPLETED && review.getLevel() == ReviewLevel.PRINCIPAL_REVIEW) {
                notificationTitle = "Appraisal Form Approved by Principal";
                notificationMessage = "Congratulations! Your appraisal form (ID: " + form.getId() + ") for academic year " + form.getAcademicYear() + 
                                      " has been finally approved by the Principal." + 
                                      (review.getRemarks() != null && !review.getRemarks().isEmpty() ? " Principal's Remarks: " + review.getRemarks() : "");
                
                NotificationDTO staffNotificationForCompletion = NotificationDTO.builder()
                    .userId(form.getUser().getId()).title(notificationTitle).message(notificationMessage).build();
                try {
                    notificationService.sendNotification(staffNotificationForCompletion);
                    log.info("Sent '{}' notification to staff {} for form {}", notificationTitle, form.getUser().getId(), form.getId());
                } catch (Exception e) {
                    log.error("Failed to send '{}' notification to staff {}: {}", notificationTitle, form.getUser().getId(), e.getMessage(), e);
                }

            } else if (newStatus == AppraisalStatus.RETURNED_TO_CHAIRPERSON && review.getLevel() == ReviewLevel.PRINCIPAL_REVIEW) {
                // Notify Staff Member
                String staffNotifTitle = "Appraisal Form Returned to Chairperson by Principal";
                String staffNotifMessage = "Your appraisal form (ID: " + form.getId() + ") for academic year " + form.getAcademicYear() + 
                                           " has been reviewed by the Principal and returned to the Chairperson. " +
                                           "Principal's Remarks: " + (review.getRemarks() != null && !review.getRemarks().isEmpty() ? review.getRemarks() : "No specific remarks provided.");
                
                NotificationDTO staffNotification = NotificationDTO.builder()
                        .userId(form.getUser().getId())
                        .title(staffNotifTitle)
                        .message(staffNotifMessage)
                        .build();
                try {
                    notificationService.sendNotification(staffNotification);
                    log.info("Sent '{}' notification to staff {} for form {}", staffNotifTitle, form.getUser().getId(), form.getId());
                } catch (Exception e) {
                    log.error("Failed to send '{}' notification to staff {}: {}", staffNotifTitle, form.getUser().getId(), e.getMessage(), e);
                }

                // Notify Chairperson
                User chairperson = userRepo.findFirstByRoles_NameIgnoreCase("CHAIRPERSON")
                        .orElseThrow(() -> new ResourceNotFoundException("Chairperson user with role 'CHAIRPERSON' not found. Cannot send notification."));
                
                String chairpersonNotifTitle = "Action Required: Appraisal Form Returned by Principal";
                String chairpersonNotifMessage = "The appraisal form (ID: " + form.getId() + ") for staff member " + form.getUser().getFullName() + 
                                                 " (Academic Year: " + form.getAcademicYear() + ") has been returned by the Principal. " +
                                                 "Principal's Remarks: " + (review.getRemarks() != null && !review.getRemarks().isEmpty() ? review.getRemarks() : "No specific remarks provided.") +
                                                 " Please review and take necessary action.";

                NotificationDTO chairpersonNotification = NotificationDTO.builder()
                        .userId(chairperson.getId())
                        .title(chairpersonNotifTitle)
                        .message(chairpersonNotifMessage)
                        .build();
                try {
                    notificationService.sendNotification(chairpersonNotification);
                    log.info("Sent '{}' notification to Chairperson {} for form {}", chairpersonNotifTitle, chairperson.getId(), form.getId());
                } catch (Exception e) {
                    log.error("Failed to send '{}' notification to Chairperson {}: {}", chairpersonNotifTitle, chairperson.getId(), e.getMessage(), e);
                }
            }
            // Consider adding other general notifications for RETURNED_TO_HOD if not covered by specific review level logic.
        }
        // Workflow logic ends

        versionRepo.save(
                AppraisalVersion.builder()
                        .appraisalForm(form)
                        .statusAtVersion(form.getStatus()) // This will capture status *before* updateAppraisalStatus if it changed
                        .remarks("Reviewed by " + review.getLevel() + " - " + review.getDecision())
                        .versionTimestamp(LocalDateTime.now())
                        .serializedSnapshot(serializeForm(form))
                        .build()
        );

        log.info("Review saved for form {} by {}", dto.getAppraisalFormId(), dto.getReviewerId());
        return mapToDTO(saved);
    }

    private String serializeForm(AppraisalForm form) {
        try {
            return objectMapper.writeValueAsString(form);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize form", e);
            throw new RuntimeException("Failed to serialize form");
        }
    }

    @Override
    public List<ReviewDTO> getReviewsByFormId(UUID formId) {
        return reviewRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setReviewerId(review.getReviewer().getId());
        dto.setAppraisalFormId(review.getAppraisalForm().getId());
        dto.setDecision(review.getDecision().name());
        dto.setRemarks(review.getRemarks());
        dto.setLevel(review.getLevel().name());
        dto.setReviewedAt(review.getReviewedAt());
        return dto;
    }
}
