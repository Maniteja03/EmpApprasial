package com.cvrce.apraisal.service;


import java.util.List;
import java.util.UUID;

import com.cvrce.apraisal.dto.parta.CitationDTO;
import com.cvrce.apraisal.dto.parta.HodUpdatePartACitationDTO; // Added import

public interface PartA_CitationService {
    CitationDTO addCitation(CitationDTO dto);
    CitationDTO updateCitation(UUID id, CitationDTO dto);
    List<CitationDTO> getCitationsByFormId(UUID formId);
    void deleteCitation(UUID id);
    CitationDTO hodUpdateCitation(UUID citationId, HodUpdatePartACitationDTO dto, UUID hodUserId); // Added method
}
