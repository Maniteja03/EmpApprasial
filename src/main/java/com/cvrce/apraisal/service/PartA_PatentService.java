package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.parta.PatentDTO;
import com.cvrce.apraisal.dto.parta.HodUpdatePartAPatentDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartA_PatentService {
    PatentDTO addPatent(PatentDTO dto);
    List<PatentDTO> getPatentsByFormId(UUID formId);
    PatentDTO updatePatent(UUID id, PatentDTO dto);
    void deletePatent(UUID id);
    PatentDTO hodUpdatePatent(UUID patentId, HodUpdatePartAPatentDTO dto, UUID hodUserId); // Added method
}
