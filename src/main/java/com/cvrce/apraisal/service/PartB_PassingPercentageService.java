package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.PassingPercentageDTO;

import java.util.List;
import java.util.UUID;

public interface PartB_PassingPercentageService {
    PassingPercentageDTO add(PassingPercentageDTO dto);
    List<PassingPercentageDTO> getByFormId(UUID formId);
    PassingPercentageDTO update(UUID id, PassingPercentageDTO dto);
    void delete(UUID id);
}
