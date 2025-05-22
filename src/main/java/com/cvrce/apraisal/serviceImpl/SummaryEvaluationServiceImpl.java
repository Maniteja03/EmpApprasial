package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.SummaryEvaluationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.SummaryEvaluation;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.SummaryEvaluationRepository;
import com.cvrce.apraisal.service.SummaryEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SummaryEvaluationServiceImpl implements SummaryEvaluationService {

    private final SummaryEvaluationRepository summaryRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    public SummaryEvaluationDTO getSummaryByFormId(UUID formId) {
        SummaryEvaluation summary = summaryRepo.findByAppraisalFormId(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Summary not found"));
        return mapToDTO(summary);
    }

    @Override
    public SummaryEvaluationDTO saveOrUpdateSummary(SummaryEvaluationDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal Form not found"));

        SummaryEvaluation summary = summaryRepo.findByAppraisalFormId(dto.getAppraisalFormId())
                .orElse(SummaryEvaluation.builder().appraisalForm(form).build());

        summary.setTotalPointsClaimed(dto.getTotalPointsClaimed());
        summary.setTotalPointsAwarded(dto.getTotalPointsAwarded());
        summary.setFinalRecommendation(dto.getFinalRecommendation());
        summary.setRemarks(dto.getRemarks());
        summary.setReviewedByRole(dto.getReviewedByRole());
        summary.setReviewedByUserId(dto.getReviewedByUserId());

        summary = summaryRepo.save(summary);
        log.info("Saved summary evaluation for form {}", form.getId());
        return mapToDTO(summary);
    }

    private SummaryEvaluationDTO mapToDTO(SummaryEvaluation summary) {
        SummaryEvaluationDTO dto = new SummaryEvaluationDTO();
        dto.setId(summary.getId());
        dto.setAppraisalFormId(summary.getAppraisalForm().getId());
        dto.setTotalPointsClaimed(summary.getTotalPointsClaimed());
        dto.setTotalPointsAwarded(summary.getTotalPointsAwarded());
        dto.setFinalRecommendation(summary.getFinalRecommendation());
        dto.setRemarks(summary.getRemarks());
        dto.setReviewedByRole(summary.getReviewedByRole());
        dto.setReviewedByUserId(summary.getReviewedByUserId());
        return dto;
    }
}
