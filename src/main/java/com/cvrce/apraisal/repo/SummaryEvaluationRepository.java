package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.SummaryEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SummaryEvaluationRepository extends JpaRepository<SummaryEvaluation, UUID> {
    Optional<SummaryEvaluation> findByAppraisalFormId(UUID formId);
    Optional<SummaryEvaluation> findByAppraisalForm(AppraisalForm form);
}
