package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.ResearchGuidanceDTO;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBResearchGuidanceDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartB_ResearchGuidanceService {
    ResearchGuidanceDTO add(ResearchGuidanceDTO dto);
    List<ResearchGuidanceDTO> getByFormId(UUID formId);
    ResearchGuidanceDTO update(UUID id, ResearchGuidanceDTO dto);
    void delete(UUID id);
    ResearchGuidanceDTO hodUpdateResearchGuidance(UUID researchGuidanceId, HodUpdatePartBResearchGuidanceDTO dto, UUID hodUserId); // Added method
}
