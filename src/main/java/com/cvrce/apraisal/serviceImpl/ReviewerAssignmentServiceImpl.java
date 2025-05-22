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
}
