package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.AdminWorkDTO;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBAdminWorkDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartB_AdminWorkService {
    AdminWorkDTO add(AdminWorkDTO dto);
    List<AdminWorkDTO> getByFormId(UUID formId);
    AdminWorkDTO update(UUID id, AdminWorkDTO dto);
    void delete(UUID id);
    AdminWorkDTO hodUpdateAdminWork(UUID adminWorkId, HodUpdatePartBAdminWorkDTO dto, UUID hodUserId); // Added method
}
