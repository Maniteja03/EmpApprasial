package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partc.CertificationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partC.PartC_Certification;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartC_CertificationRepository;
import com.cvrce.apraisal.service.PartC_CertificationService;
import com.cvrce.apraisal.dto.partc.HodUpdatePartCCertificationDTO; // Added
import com.cvrce.apraisal.entity.AppraisalVersion; // Added
import com.cvrce.apraisal.repo.AppraisalVersionRepository; // Added
import com.cvrce.apraisal.repo.UserRepository; // Added
import com.cvrce.apraisal.entity.User; // Added
import com.cvrce.apraisal.enums.AppraisalStatus; // Added
import com.fasterxml.jackson.databind.ObjectMapper; // Added
import com.fasterxml.jackson.core.JsonProcessingException; // Added
import java.time.LocalDateTime; // Added
import jakarta.transaction.Transactional; // Added for @Transactional
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
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

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

    @Override
    @Transactional
    public CertificationDTO hodUpdateCertification(UUID certificationId, HodUpdatePartCCertificationDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartC_Certification certification = certRepo.findById(certificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Certification not found: " + certificationId));

        AppraisalForm form = certification.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit certifications when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        certification.setCertificationTitle(dto.getCertificationTitle());
        certification.setCompany(dto.getCompany());
        certification.setStudentsAllotted(dto.getStudentsAllotted());
        certification.setStudentsCertified(dto.getStudentsCertified());
        certification.setPointsClaimed(dto.getPointsClaimed());
        certification.setProofFilePath(dto.getProofFilePath());
        
        PartC_Certification updatedCertification = certRepo.save(certification);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part C Certification: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedCertification.getCertificationTitle());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of certification: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated certification {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, certificationId, form.getId());
        return mapToDTO(updatedCertification);
    }
}
