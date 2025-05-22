package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.review.ReviewerAssignmentDTO;
import com.cvrce.apraisal.service.ReviewerAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
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

}
