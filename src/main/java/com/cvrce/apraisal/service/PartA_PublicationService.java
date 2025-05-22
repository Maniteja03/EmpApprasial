package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.parta.PublicationDTO;

import java.util.List;
import java.util.UUID;

public interface PartA_PublicationService {
    PublicationDTO addPublication(PublicationDTO dto);
    PublicationDTO updatePublication(UUID id, PublicationDTO dto);
    List<PublicationDTO> getPublicationsByFormId(UUID formId);
    void deletePublication(UUID id);
}
