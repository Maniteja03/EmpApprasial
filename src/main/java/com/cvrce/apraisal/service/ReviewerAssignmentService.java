package com.cvrce.apraisal.service;



import java.util.List;
import java.util.UUID;

import com.cvrce.apraisal.dto.review.ReviewerAssignmentDTO;

import com.cvrce.apraisal.enums.ReviewLevel; // Added import

public interface ReviewerAssignmentService {
    ReviewerAssignmentDTO assignReviewer(ReviewerAssignmentDTO dto);
    List<ReviewerAssignmentDTO> getAssignmentsForReviewer(UUID reviewerId);
    void reassignReviewer(UUID assignmentId, UUID newReviewerId);

    List<ReviewerAssignmentDTO> assignToDepartmentCommittee(UUID formId, List<UUID> memberIds, UUID assignedByUserId);
    List<ReviewerAssignmentDTO> assignToCollegeCommittee(UUID formId, List<UUID> memberIds, UUID assignedByUserId);
    ReviewerAssignmentDTO assignToUserForReview(UUID formId, UUID reviewerId, ReviewLevel level, UUID assignedByUserId);
}
