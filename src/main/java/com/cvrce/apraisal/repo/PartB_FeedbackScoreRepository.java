package com.cvrce.apraisal.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cvrce.apraisal.entity.partB.PartB_FeedbackScore;

import java.util.UUID;

public interface PartB_FeedbackScoreRepository extends JpaRepository<PartB_FeedbackScore, UUID> {
    PartB_FeedbackScore findByAppraisalFormId(UUID formId);
}
