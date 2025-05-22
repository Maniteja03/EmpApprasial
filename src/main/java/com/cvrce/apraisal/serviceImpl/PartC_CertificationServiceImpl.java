package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partc.CertificationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partC.PartC_Certification;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartC_CertificationRepository;
import com.cvrce.apraisal.service.PartC_CertificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartC_CertificationServiceImpl implements PartC_CertificationService {

    private final PartC_CertificationRepository certRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    public CertificationDTO addCertification(CertificationDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal Form not found"));

        PartC_Certification cert = PartC_Certification.builder()
                .appraisalForm(form)
                .certificationTitle(dto.getCertificationTitle())
                .company(dto.getCompany())
                .studentsAllotted(dto.getStudentsAllotted())
                .studentsCertified(dto.getStudentsCertified())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();

        return mapToDTO(certRepo.save(cert));
    }

    @Override
    public List<CertificationDTO> getCertificationsByFormId(UUID formId) {
        return certRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public CertificationDTO updateCertification(UUID id, CertificationDTO dto) {
        PartC_Certification cert = certRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found"));

        cert.setCertificationTitle(dto.getCertificationTitle());
        cert.setCompany(dto.getCompany());
        cert.setStudentsAllotted(dto.getStudentsAllotted());
        cert.setStudentsCertified(dto.getStudentsCertified());
        cert.setPointsClaimed(dto.getPointsClaimed());
        cert.setProofFilePath(dto.getProofFilePath());

        return mapToDTO(certRepo.save(cert));
    }

    @Override
    public void deleteCertification(UUID id) {
        if (!certRepo.existsById(id)) {
            throw new ResourceNotFoundException("Certification not found");
        }
        certRepo.deleteById(id);
    }

    private CertificationDTO mapToDTO(PartC_Certification cert) {
        CertificationDTO dto = new CertificationDTO();
        dto.setId(cert.getId());
        dto.setAppraisalFormId(cert.getAppraisalForm().getId());
        dto.setCertificationTitle(cert.getCertificationTitle());
        dto.setCompany(cert.getCompany());
        dto.setStudentsAllotted(cert.getStudentsAllotted());
        dto.setStudentsCertified(cert.getStudentsCertified());
        dto.setPointsClaimed(cert.getPointsClaimed());
        dto.setProofFilePath(cert.getProofFilePath());
        return dto;
    }
}
