package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.parta.PublicationDTO;
import com.cvrce.apraisal.dto.parta.HodUpdatePublicationDTO; // Added import

import java.util.List;
import java.util.UUID;

public interface PartA_PublicationService {
    PublicationDTO addPublication(PublicationDTO dto);
    PublicationDTO updatePublication(UUID id, PublicationDTO dto);
    List<PublicationDTO> getPublicationsByFormId(UUID formId);
    void deletePublication(UUID id);
    PublicationDTO hodUpdatePublication(UUID publicationId, HodUpdatePublicationDTO dto, UUID hodUserId); // Added method
}
