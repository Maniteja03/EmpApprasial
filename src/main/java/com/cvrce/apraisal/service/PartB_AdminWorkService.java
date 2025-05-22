package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.AdminWorkDTO;

import java.util.List;
import java.util.UUID;

public interface PartB_AdminWorkService {
    AdminWorkDTO add(AdminWorkDTO dto);
    List<AdminWorkDTO> getByFormId(UUID formId);
    AdminWorkDTO update(UUID id, AdminWorkDTO dto);
    void delete(UUID id);
}
