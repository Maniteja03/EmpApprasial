package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.review.ReviewerAssignmentDTO;
import com.cvrce.apraisal.service.ReviewerAssignmentService;
import com.cvrce.apraisal.enums.ReviewLevel; // Added import
import lombok.Data; // Added for DTOs
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added import
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviewer-assignments")
@RequiredArgsConstructor
@Slf4j
public class ReviewerAssignmentController {

    private final ReviewerAssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<ReviewerAssignmentDTO> assign(@RequestBody ReviewerAssignmentDTO dto) {
        log.info("Assigning reviewer {} to form {}", dto.getReviewerId(), dto.getAppraisalFormId());
        return new ResponseEntity<>(assignmentService.assignReviewer(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{reviewerId}")
    public ResponseEntity<List<ReviewerAssignmentDTO>> getAssignments(@PathVariable UUID reviewerId) {
        log.info("Fetching assignments for reviewer {}", reviewerId);
        return ResponseEntity.ok(assignmentService.getAssignmentsForReviewer(reviewerId));
    }
    @PutMapping("/reassign")
    public ResponseEntity<String> reassignReviewer(@RequestParam UUID assignmentId,
                                                   @RequestParam UUID newReviewerId) {
    	assignmentService.reassignReviewer(assignmentId, newReviewerId);
        return ResponseEntity.ok("Reviewer reassigned successfully");
    }

    // DTOs as static inner classes
    @Data
    static class CommitteeAssignmentRequest {
        private UUID formId;
        private List<UUID> memberIds;
    }

    @Data
    static class UserAssignmentRequest {
        private UUID formId;
        private UUID reviewerId;
        private String level; // Will be converted to ReviewLevel enum
    }

    // Endpoint for assigning to Department Committee
    @PostMapping("/department-committee")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<List<ReviewerAssignmentDTO>> assignDeptCommittee(
            @RequestBody CommitteeAssignmentRequest request
    ) {
        UUID assignedByUserId = UUID.randomUUID(); // Placeholder
        log.info("API: Assigning form {} to Department Committee by user {}", request.getFormId(), assignedByUserId);
        List<ReviewerAssignmentDTO> assignments = assignmentService.assignToDepartmentCommittee(request.getFormId(), request.getMemberIds(), assignedByUserId);
        return ResponseEntity.ok(assignments);
    }

    // Endpoint for assigning to College Committee
    @PostMapping("/college-committee")
    @PreAuthorize("hasAuthority('ROLE_CHAIRPERSON')")
    public ResponseEntity<List<ReviewerAssignmentDTO>> assignCollegeCommittee(
            @RequestBody CommitteeAssignmentRequest request
    ) {
        UUID assignedByUserId = UUID.randomUUID(); // Placeholder
        log.info("API: Assigning form {} to College Committee by user {}", request.getFormId(), assignedByUserId);
        List<ReviewerAssignmentDTO> assignments = assignmentService.assignToCollegeCommittee(request.getFormId(), request.getMemberIds(), assignedByUserId);
        return ResponseEntity.ok(assignments);
    }

    // Endpoint for assigning a single user for a specific review level
    @PostMapping("/user-review")
    @PreAuthorize("hasAnyAuthority('ROLE_HOD', 'ROLE_CHAIRPERSON')")
    public ResponseEntity<ReviewerAssignmentDTO> assignUserForReview(
            @RequestBody UserAssignmentRequest request
    ) {
        UUID assignedByUserId = UUID.randomUUID(); // Placeholder
        ReviewLevel reviewLevel = ReviewLevel.valueOf(request.getLevel().toUpperCase());

        log.info("API: Assigning user {} to form {} for level {} by user {}", request.getReviewerId(), request.getFormId(), request.getLevel(), assignedByUserId);
        ReviewerAssignmentDTO assignment = assignmentService.assignToUserForReview(request.getFormId(), request.getReviewerId(), reviewLevel, assignedByUserId);
        return ResponseEntity.ok(assignment);
    }
}
