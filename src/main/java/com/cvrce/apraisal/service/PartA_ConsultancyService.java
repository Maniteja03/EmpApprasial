package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.parta.ConsultancyDTO;
import com.cvrce.apraisal.dto.parta.HodUpdatePartAConsultancyDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartA_ConsultancyService {
    ConsultancyDTO addConsultancy(ConsultancyDTO dto);
    List<ConsultancyDTO> getConsultanciesByFormId(UUID formId);
    ConsultancyDTO updateConsultancy(UUID id, ConsultancyDTO dto);
    void deleteConsultancy(UUID id);
    ConsultancyDTO hodUpdateConsultancy(UUID consultancyId, HodUpdatePartAConsultancyDTO dto, UUID hodUserId); // Added method
}
