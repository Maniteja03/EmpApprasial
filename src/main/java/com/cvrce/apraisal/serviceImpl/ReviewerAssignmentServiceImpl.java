package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.review.ReviewerAssignmentDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.review.ReviewerAssignment;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.ReviewerAssignmentRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.ReviewerAssignmentService;
import com.cvrce.apraisal.service.AppraisalFormService; // Added
import com.cvrce.apraisal.enums.AppraisalStatus; // Added
import com.cvrce.apraisal.enums.ReviewLevel; // Added
import com.cvrce.apraisal.service.NotificationService; // Added
import com.cvrce.apraisal.dto.notification.NotificationDTO; // Added

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewerAssignmentServiceImpl implements ReviewerAssignmentService {

    private final ReviewerAssignmentRepository assignmentRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepo;
    private final AppraisalFormService appraisalFormService; // Added
    private final NotificationService notificationService; // Added

    @Override
    public ReviewerAssignmentDTO assignReviewer(ReviewerAssignmentDTO dto) {
        User reviewer = userRepo.findById(dto.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found: " + dto.getReviewerId()));

        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + dto.getAppraisalFormId()));

        ReviewerAssignment assignment = ReviewerAssignment.builder()
                .reviewer(reviewer)
                .appraisalForm(form)
                .build();

        assignment = assignmentRepo.save(assignment);
        log.info("Assigned reviewer {} ({}) to form {}", reviewer.getFullName(), reviewer.getId(), form.getId());

        return mapToDTO(assignment);
    }

    @Override
    public List<ReviewerAssignmentDTO> getAssignmentsForReviewer(UUID reviewerId) {
        return assignmentRepo.findByReviewerId(reviewerId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void reassignReviewer(UUID assignmentId, UUID newReviewerId) {
        ReviewerAssignment assignment = assignmentRepo.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        User newReviewer = userRepo.findById(newReviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("New reviewer not found: " + newReviewerId));

        // Optional: ensure same department
        if (!assignment.getAppraisalForm().getUser().getDepartment().equals(newReviewer.getDepartment())) {
            throw new IllegalStateException("Reviewer must be from the same department");
        }

        assignment.setReviewer(newReviewer);
        assignmentRepo.save(assignment);
        log.info("Reassigned reviewer for form {} to {}", assignment.getAppraisalForm().getId(), newReviewer.getId());
    }

    private ReviewerAssignmentDTO mapToDTO(ReviewerAssignment assignment) {
        ReviewerAssignmentDTO dto = new ReviewerAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setReviewerId(assignment.getReviewer().getId());
        dto.setReviewerName(assignment.getReviewer().getFullName());
        dto.setAppraisalFormId(assignment.getAppraisalForm().getId());
        return dto;
    }

    @Override
    @Transactional
    public List<ReviewerAssignmentDTO> assignToDepartmentCommittee(UUID formId, List<UUID> memberIds, UUID assignedByUserId) {
        AppraisalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formId));
        User assigner = userRepo.findById(assignedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner User not found: " + assignedByUserId));

        User submittingUser = form.getUser();
        if (submittingUser == null) {
            throw new IllegalStateException("Submitting user not found for form: " + formId);
        }
        Department submittingUserDepartment = submittingUser.getDepartment();
        if (submittingUserDepartment == null) {
            throw new IllegalStateException("Department not found for submitting user: " + submittingUser.getFullName());
        }

        List<ReviewerAssignment> assignments = new ArrayList<>();
        for (UUID memberId : memberIds) {
            User committeeMember = userRepo.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Committee member User not found: " + memberId));

            Department committeeMemberDepartment = committeeMember.getDepartment();
            if (committeeMemberDepartment == null) {
                throw new IllegalStateException("Department not found for committee member: " + committeeMember.getFullName());
            }

            if (!submittingUserDepartment.getId().equals(committeeMemberDepartment.getId())) {
                throw new IllegalArgumentException("Committee member " + committeeMember.getFullName() +
                        " (" + committeeMember.getEmployeeId() + ") must be from the same department (" +
                        submittingUserDepartment.getName() + ") as the form submitter.");
            }
            
            ReviewerAssignment assignment = ReviewerAssignment.builder()
                    .reviewer(committeeMember)
                    .appraisalForm(form)
                    .build();
            assignments.add(assignmentRepo.save(assignment));
            log.info("Assigned Dept. Committee member {} to form {}", memberId, formId);
        }
        
        String remark = "Assigned to Department Committee by " + assigner.getFullName();
        appraisalFormService.updateAppraisalStatus(formId, AppraisalStatus.DEPARTMENT_REVIEW, remark, assignedByUserId);
        
        // Notify committee members
        String committeeNotificationTitle = "New Appraisal Form Assignment";
        String committeeNotificationMessageBase = "You have been assigned to the Department Committee for appraisal form ID: " + formId + 
                                               " (Academic Year: " + form.getAcademicYear() + 
                                               " for staff member: " + form.getUser().getFullName() + ").";

        for (ReviewerAssignment currentAssignment : assignments) {
            NotificationDTO memberNotification = NotificationDTO.builder()
                    .userId(currentAssignment.getReviewer().getId())
                    .title(committeeNotificationTitle)
                    .message(committeeNotificationMessageBase)
                    .build();
            try {
                notificationService.sendNotification(memberNotification);
            } catch (Exception e) {
                log.error("Failed to send assignment notification to dept committee member {}: {}", currentAssignment.getReviewer().getId(), e.getMessage());
            }
        }
        
        return assignments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<ReviewerAssignmentDTO> assignToCollegeCommittee(UUID formId, List<UUID> memberIds, UUID assignedByUserId) {
        AppraisalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formId));
        User assigner = userRepo.findById(assignedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner User not found: " + assignedByUserId));

        User submittingUser = form.getUser();
        if (submittingUser == null) {
            throw new IllegalStateException("Submitting user not found for form: " + formId);
        }
        Department submittingUserDepartment = submittingUser.getDepartment();
        if (submittingUserDepartment == null) {
            throw new IllegalStateException("Department not found for submitting user: " + submittingUser.getFullName());
        }

        List<ReviewerAssignment> assignments = new ArrayList<>();
        for (UUID memberId : memberIds) {
            User committeeMember = userRepo.findById(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException("Committee member User not found: " + memberId));
            
            Department committeeMemberDepartment = committeeMember.getDepartment();
            if (committeeMemberDepartment == null) {
                throw new IllegalStateException("Department not found for committee member: " + committeeMember.getFullName());
            }

            if (submittingUserDepartment.getId().equals(committeeMemberDepartment.getId())) {
                throw new IllegalArgumentException("College Committee member " + committeeMember.getFullName() +
                        " (" + committeeMember.getEmployeeId() + ") must be from a different department than the form submitter's department (" +
                        submittingUserDepartment.getName() + ").");
            }

            ReviewerAssignment assignment = ReviewerAssignment.builder()
                    .reviewer(committeeMember)
                    .appraisalForm(form)
                    .build();
            assignments.add(assignmentRepo.save(assignment));
            log.info("Assigned College Committee member {} to form {}", memberId, formId);
        }

        String remark = "Assigned to College Committee by " + assigner.getFullName();
        appraisalFormService.updateAppraisalStatus(formId, AppraisalStatus.COLLEGE_REVIEW, remark, assignedByUserId);

        // Notify committee members
        String collegeCommitteeNotificationTitle = "New Appraisal Form Assignment";
        String collegeCommitteeNotificationMessageBase = "You have been assigned to the College Committee for appraisal form ID: " + formId +
                                                      " (Academic Year: " + form.getAcademicYear() +
                                                      " for staff member: " + form.getUser().getFullName() + ").";
                                                      
        for (ReviewerAssignment currentAssignment : assignments) {
            NotificationDTO memberNotification = NotificationDTO.builder()
                    .userId(currentAssignment.getReviewer().getId())
                    .title(collegeCommitteeNotificationTitle)
                    .message(collegeCommitteeNotificationMessageBase)
                    .build();
            try {
                notificationService.sendNotification(memberNotification);
            } catch (Exception e) {
                log.error("Failed to send assignment notification to college committee member {}: {}", currentAssignment.getReviewer().getId(), e.getMessage());
            }
        }

        return assignments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReviewerAssignmentDTO assignToUserForReview(UUID formId, UUID reviewerId, ReviewLevel level, UUID assignedByUserId) {
        AppraisalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formId));
        User reviewer = userRepo.findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer User not found: " + reviewerId));
        User assigner = userRepo.findById(assignedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigner User not found: " + assignedByUserId));

        ReviewerAssignment assignment = ReviewerAssignment.builder()
                .reviewer(reviewer)
                .appraisalForm(form)
                .build();
        ReviewerAssignment savedAssignment = assignmentRepo.save(assignment);
        log.info("Assigned user {} for {} to form {}", reviewerId, level, formId);

        AppraisalStatus newStatus;
        String remark = "Assigned for " + level + " by " + assigner.getFullName();

        switch (level) {
            case VERIFYING_STAFF_REVIEW:
                newStatus = AppraisalStatus.PENDING_VERIFICATION;
                break;
            case PRINCIPAL_REVIEW:
                newStatus = AppraisalStatus.PENDING_PRINCIPAL_APPROVAL;
                break;
            default:
                 throw new IllegalArgumentException("Review level " + level + " is not configured for direct status update in assignToUserForReview.");
        }

        appraisalFormService.updateAppraisalStatus(formId, newStatus, remark, assignedByUserId);
        
        // Notify the assigned reviewer
        String userReviewNotificationTitle = "New Appraisal Form Review Assignment";
        String userReviewNotificationMessage = "You have been assigned to review appraisal form ID: " + formId +
                                               " (Academic Year: " + form.getAcademicYear() +
                                               " for staff member: " + form.getUser().getFullName() + ")" +
                                               " at review level: " + level + ".";

        NotificationDTO reviewerNotification = NotificationDTO.builder()
                .userId(reviewerId)
                .title(userReviewNotificationTitle)
                .message(userReviewNotificationMessage)
                .build();
        try {
            notificationService.sendNotification(reviewerNotification);
        } catch (Exception e) {
            log.error("Failed to send assignment notification to user {}: {}", reviewerId, e.getMessage());
        }
        
        return mapToDTO(savedAssignment);
    }
}
