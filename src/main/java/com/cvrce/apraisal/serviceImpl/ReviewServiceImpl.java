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
                } else if (review.getDecision() == ReviewDecision.FORWARD) {
                    // HOD forwards to Verifying Staff
                    newStatus = AppraisalStatus.PENDING_VERIFICATION;
                    // Later, add logic to assign to Verifying Staff (Step 4)
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
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                     // This means Chairperson has reviewed and approved (e.g. after college committee)
                     // but is not yet forwarding to Principal. Or it's an intermediate approval.
                     // For the workflow: Chairperson forwards to Principal is a FORWARD decision.
                    newStatus = AppraisalStatus.CHAIR_REVIEW; // Stays in CHAIR_REVIEW, or a new PENDING_CHAIRPERSON_FORWARD_TO_PRINCIPAL
                } else if (review.getDecision() == ReviewDecision.FORWARD) {
                    // Chairperson forwards to Principal
                    newStatus = AppraisalStatus.PENDING_PRINCIPAL_APPROVAL;
                    // Later, add logic to assign to Principal (Step 4)
                }
                break;

            case PRINCIPAL_REVIEW: // New Level
                if (review.getDecision() == ReviewDecision.REUPLOAD) {
                    newStatus = AppraisalStatus.RETURNED_TO_CHAIRPERSON; // Principal sends back to Chairperson
                } else if (review.getDecision() == ReviewDecision.APPROVE) {
                    newStatus = AppraisalStatus.COMPLETED; // Final approval
                }
                break;
        }

        if (newStatus != null && newStatus != form.getStatus()) { // Only update if status is actually changing
            appraisalFormService.updateAppraisalStatus(form.getId(), newStatus, reviewRemarksForStatusUpdate, reviewer.getId());
            log.info("AppraisalForm {} status updated to {} due to review by {}", form.getId(), newStatus, reviewer.getId());

            // Send notification to the staff member if status is critical for them
            String notificationTitle = null;
            String notificationMessage = null;

            if (newStatus == AppraisalStatus.REUPLOAD_REQUIRED) {
                notificationTitle = "Action Required on Your Appraisal Form"; // New Title
                notificationMessage = "Your appraisal form (ID: " + form.getId() + ") for academic year " + form.getAcademicYear() + 
                                      " requires corrections. Please contact your HOD to discuss and have the necessary changes made in the system. Reviewer remarks: " + 
                                      (review.getRemarks() != null && !review.getRemarks().isEmpty() ? review.getRemarks() : "No specific remarks provided by reviewer."); // New Message
            } else if (newStatus == AppraisalStatus.COMPLETED) {
                notificationTitle = "Appraisal Form Approved";
                notificationMessage = "Congratulations! Your appraisal form (ID: " + form.getId() + ") for academic year " + form.getAcademicYear() + 
                                      " has been approved and completed.";
            } else if (newStatus == AppraisalStatus.RETURNED_TO_HOD || newStatus == AppraisalStatus.RETURNED_TO_CHAIRPERSON) {
                // These might be relevant for the staff to know their form is moving back in the chain, though not directly actionable by them.
                // For now, as per prompt, focusing on REUPLOAD_REQUIRED and COMPLETED.
                // Consider adding these if comprehensive staff visibility is desired.
            }


            if (notificationTitle != null && notificationMessage != null) {
                NotificationDTO staffNotification = NotificationDTO.builder()
                        .userId(form.getUser().getId()) // Notify the original staff member
                        .title(notificationTitle)
                        .message(notificationMessage)
                        .build();
                try {
                    notificationService.sendNotification(staffNotification);
                    log.info("Sent '{}' notification to staff {} for form {}", notificationTitle, form.getUser().getId(), form.getId());
                } catch (Exception e) {
                    log.error("Failed to send '{}' notification to staff {}: {}", notificationTitle, form.getUser().getId(), e.getMessage());
                }
            }
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
