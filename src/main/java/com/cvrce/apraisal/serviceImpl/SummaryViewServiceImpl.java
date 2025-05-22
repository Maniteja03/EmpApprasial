package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.SummaryRowDTO;
import com.cvrce.apraisal.entity.SummaryEvaluation;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.SummaryEvaluationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryViewServiceImpl {

    private final AppraisalFormRepository appraisalFormRepository;
    private final SummaryEvaluationRepository summaryEvaluationRepository;

    public List<SummaryRowDTO> getSummaryRows() {
        return appraisalFormRepository.findAll().stream()
                .map(form -> {
                    SummaryEvaluation eval = summaryEvaluationRepository.findByAppraisalForm(form).orElse(null);

                    return SummaryRowDTO.builder()
                            .staffName(form.getUser().getFullName())
                            .department(form.getUser().getDepartment().getName())
                            .academicYear(form.getAcademicYear())
                            .totalScore(form.getTotalScore())
                            .pointsClaimed(eval != null ? eval.getTotalPointsClaimed() : 0f)
                            .pointsAwarded(eval != null ? eval.getTotalPointsAwarded() : 0f)
                            .remarks(eval != null ? eval.getRemarks() : "Not Reviewed")
                            .recommendation(eval != null ? eval.getFinalRecommendation() : "Pending")
                            .build();
                })
                .collect(Collectors.toList());
    }
}
