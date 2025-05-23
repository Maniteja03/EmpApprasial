package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.review.ReviewerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewerAssignmentRepository extends JpaRepository<ReviewerAssignment, UUID> {
    List<ReviewerAssignment> findByReviewerId(UUID reviewerId);
    List<ReviewerAssignment> findByAppraisalFormId(UUID appraisalFormId);
    long countByAppraisalFormId(UUID appraisalFormId); // Added
}
