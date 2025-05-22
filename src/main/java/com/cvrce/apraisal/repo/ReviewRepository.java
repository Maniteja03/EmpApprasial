package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByAppraisalFormId(UUID formId);
    List<Review> findByReviewerId(UUID reviewerId);
    Optional<Review> findByReviewerAndAppraisalForm(User reviewer, AppraisalForm appraisalForm);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.level = :level AND r.decision IS NULL")
    long countPendingByLevel(@Param("level") String level);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.decision = 'APPROVED'")
    long countApproved();

    @Query("SELECT COUNT(r) FROM Review r WHERE r.decision = 'REUPLOAD'")
    long countReupload();

}
