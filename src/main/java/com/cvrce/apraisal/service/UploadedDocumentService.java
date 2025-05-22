package com.cvrce.apraisal.service;


import java.util.List;
import java.util.UUID;

import com.cvrce.apraisal.dto.UploadedDocumentDTO;

public interface UploadedDocumentService {
    List<UploadedDocumentDTO> getDocuments(UUID formId, String section);
}
