package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.ProjectGuidanceDTO;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBProjectGuidanceDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartB_ProjectGuidanceService {
    ProjectGuidanceDTO add(ProjectGuidanceDTO dto);
    List<ProjectGuidanceDTO> getByFormId(UUID formId);
    ProjectGuidanceDTO update(UUID id, ProjectGuidanceDTO dto);
    void delete(UUID id);
    ProjectGuidanceDTO hodUpdateProjectGuidance(UUID projectGuidanceId, HodUpdatePartBProjectGuidanceDTO dto, UUID hodUserId); // Added method
}
