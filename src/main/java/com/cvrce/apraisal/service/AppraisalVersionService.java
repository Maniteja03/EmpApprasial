package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.appraisal.AppraisalVersionDTO;

import java.util.List;
import java.util.UUID;

public interface AppraisalVersionService {
    AppraisalVersionDTO addVersion(AppraisalVersionDTO dto);
    List<AppraisalVersionDTO> getVersionsByForm(UUID formId);
}
