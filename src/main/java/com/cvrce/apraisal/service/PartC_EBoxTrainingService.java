package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partc.EBoxTrainingDTO;
import com.cvrce.apraisal.dto.partc.HodUpdatePartCEBoxTrainingDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartC_EBoxTrainingService {
    EBoxTrainingDTO addEBoxTraining(EBoxTrainingDTO dto);
    List<EBoxTrainingDTO> getEBoxTrainingsByFormId(UUID formId);
    EBoxTrainingDTO updateEBoxTraining(UUID id, EBoxTrainingDTO dto);
    void deleteEBoxTraining(UUID id);
    EBoxTrainingDTO hodUpdateEBoxTraining(UUID eboxTrainingId, HodUpdatePartCEBoxTrainingDTO dto, UUID hodUserId); // Added method
}
