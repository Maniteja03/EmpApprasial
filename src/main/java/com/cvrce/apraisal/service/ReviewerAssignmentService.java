package com.cvrce.apraisal.service;



import java.util.List;
import java.util.UUID;

import com.cvrce.apraisal.dto.review.ReviewerAssignmentDTO;

public interface ReviewerAssignmentService {
    ReviewerAssignmentDTO assignReviewer(ReviewerAssignmentDTO dto);
    List<ReviewerAssignmentDTO> getAssignmentsForReviewer(UUID reviewerId);
    void reassignReviewer(UUID assignmentId, UUID newReviewerId);

}
