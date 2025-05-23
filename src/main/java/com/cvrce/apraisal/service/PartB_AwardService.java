package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partb.AwardDTO;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBAwardDTO; // Added import
import java.util.List;
import java.util.UUID;

public interface PartB_AwardService {
    AwardDTO addAward(AwardDTO dto);
    List<AwardDTO> getAwardsByFormId(UUID formId);
    AwardDTO updateAward(UUID id, AwardDTO dto);
    void deleteAward(UUID id);
    AwardDTO hodUpdateAward(UUID awardId, HodUpdatePartBAwardDTO dto, UUID hodUserId); // Added method
}
