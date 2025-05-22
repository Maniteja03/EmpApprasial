package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.ResearchGuidanceDTO;

import java.util.List;
import java.util.UUID;

public interface PartB_ResearchGuidanceService {
    ResearchGuidanceDTO add(ResearchGuidanceDTO dto);
    List<ResearchGuidanceDTO> getByFormId(UUID formId);
    ResearchGuidanceDTO update(UUID id, ResearchGuidanceDTO dto);
    void delete(UUID id);
}
