package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.parta.ProjectDTO;
import com.cvrce.apraisal.dto.parta.HodUpdatePartAProjectDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartA_ProjectService {
    ProjectDTO addProject(ProjectDTO dto);
    List<ProjectDTO> getProjectsByFormId(UUID formId);
    ProjectDTO updateProject(UUID id, ProjectDTO dto);
    void deleteProject(UUID id);
    ProjectDTO hodUpdateProject(UUID projectId, HodUpdatePartAProjectDTO dto, UUID hodUserId); // Added method
}
