package com.cvrce.apraisal.service;

import java.util.UUID;
import java.util.*;

import com.cvrce.apraisal.dto.appraisal.AppraisalFormDTO;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.enums.ReviewLevel; // Added import

public interface AppraisalFormService {
    AppraisalFormDTO createDraftForm(String academicYear, UUID userId);
    List<AppraisalFormDTO> getMySubmissions(UUID userId);
    AppraisalFormDTO submit(UUID formId); // locks and submits
    List<AppraisalFormDTO> filterByStatus(AppraisalStatus status);
    AppraisalFormDTO getById(UUID formId);
    AppraisalFormDTO updateAppraisalStatus(UUID formId, AppraisalStatus newStatus, String remark, UUID changedByUserId);
    AppraisalFormDTO hodFinalizeCorrections(UUID formId, UUID hodUserId, ReviewLevel restartReviewLevel); // Added method


}

