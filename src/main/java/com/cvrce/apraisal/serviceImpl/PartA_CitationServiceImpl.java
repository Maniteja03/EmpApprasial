package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.CitationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Citation;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_CitationRepository;
import com.cvrce.apraisal.service.PartA_CitationService;
import com.cvrce.apraisal.dto.parta.HodUpdatePartACitationDTO; // Added
import com.cvrce.apraisal.entity.AppraisalVersion; // Added
import com.cvrce.apraisal.repo.AppraisalVersionRepository; // Added
import com.cvrce.apraisal.repo.UserRepository; // Added
import com.cvrce.apraisal.entity.User; // Added
import com.cvrce.apraisal.enums.AppraisalStatus; // Added
import com.fasterxml.jackson.databind.ObjectMapper; // Added
import com.fasterxml.jackson.core.JsonProcessingException; // Added
import java.time.LocalDateTime; // Added
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartA_CitationServiceImpl implements PartA_CitationService {

    private final PartA_CitationRepository citationRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public CitationDTO addCitation(CitationDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found for ID: " + dto.getAppraisalFormId()));

        PartA_Citation citation = mapToEntity(dto);
        citation.setAppraisalForm(form);

        PartA_Citation saved = citationRepo.save(citation);
        log.info("Added citation with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public CitationDTO updateCitation(UUID id, CitationDTO dto) {
        PartA_Citation citation = citationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Citation not found with ID: " + id));

        citation.setScopusAuthorId(dto.getScopusAuthorId());
        citation.setCitationCount(dto.getCitationCount());
        citation.setCitationYear(dto.getCitationYear());
        citation.setPointsClaimed(dto.getPointsClaimed());
        citation.setProofFilePath(dto.getProofFilePath());

        PartA_Citation updated = citationRepo.save(citation);
        log.info("Updated citation with ID: {}", updated.getId());
        return mapToDTO(updated);
    }

    @Override
    public List<CitationDTO> getCitationsByFormId(UUID formId) {
        return citationRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCitation(UUID id) {
        if (!citationRepo.existsById(id)) {
            throw new ResourceNotFoundException("Citation not found with ID: " + id);
        }
        citationRepo.deleteById(id);
        log.info("Deleted citation with ID: {}", id);
    }

    private CitationDTO mapToDTO(PartA_Citation citation) {
        CitationDTO dto = new CitationDTO();
        dto.setId(citation.getId());
        dto.setAppraisalFormId(citation.getAppraisalForm().getId());
        dto.setScopusAuthorId(citation.getScopusAuthorId());
        dto.setCitationCount(citation.getCitationCount());
        dto.setCitationYear(citation.getCitationYear());
        dto.setPointsClaimed((float) citation.getPointsClaimed());
        dto.setProofFilePath(citation.getProofFilePath());
        return dto;
    }

    private PartA_Citation mapToEntity(CitationDTO dto) {
        return PartA_Citation.builder()
                .scopusAuthorId(dto.getScopusAuthorId())
                .citationCount(dto.getCitationCount())
                .citationYear(dto.getCitationYear())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();
    }

    @Override
    @Transactional
    public CitationDTO hodUpdateCitation(UUID citationId, HodUpdatePartACitationDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartA_Citation citation = citationRepo.findById(citationId)
                .orElseThrow(() -> new ResourceNotFoundException("Citation not found: " + citationId));

        AppraisalForm form = citation.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit citations when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        citation.setScopusAuthorId(dto.getScopusAuthorId());
        citation.setCitationCount(dto.getCitationCount());
        citation.setCitationYear(dto.getCitationYear());
        citation.setPointsClaimed(dto.getPointsClaimed());
        citation.setProofFilePath(dto.getProofFilePath());
        
        PartA_Citation updatedCitation = citationRepo.save(citation);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part A Citation: ScopusID %s, Year %d. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedCitation.getScopusAuthorId(), updatedCitation.getCitationYear());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of citation: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated citation {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, citationId, form.getId());
        return mapToDTO(updatedCitation);
    }
}
