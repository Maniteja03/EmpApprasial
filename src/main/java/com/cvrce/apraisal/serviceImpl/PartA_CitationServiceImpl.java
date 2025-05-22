package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.CitationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Citation;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_CitationRepository;
import com.cvrce.apraisal.service.PartA_CitationService;
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
}
