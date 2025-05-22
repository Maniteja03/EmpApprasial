package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.partc.CertificationDTO;

import java.util.List;
import java.util.UUID;

public interface PartC_CertificationService {
    CertificationDTO addCertification(CertificationDTO dto);
    List<CertificationDTO> getCertificationsByFormId(UUID formId);
    CertificationDTO updateCertification(UUID id, CertificationDTO dto);
    void deleteCertification(UUID id);
}
