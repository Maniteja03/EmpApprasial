package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.PublicationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Publication;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_PublicationRepository;
import com.cvrce.apraisal.service.PartA_PublicationService;
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
public class PartA_PublicationServiceImpl implements PartA_PublicationService {

    private final PartA_PublicationRepository publicationRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    @Transactional
    public PublicationDTO addPublication(PublicationDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found for ID: " + dto.getAppraisalFormId()));

        PartA_Publication publication = mapToEntity(dto);
        publication.setAppraisalForm(form);

        PartA_Publication saved = publicationRepo.save(publication);
        log.info("Added publication with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public PublicationDTO updatePublication(UUID id, PublicationDTO dto) {
        PartA_Publication pub = publicationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Publication not found with ID: " + id));

        pub.setTitle(dto.getTitle());
        pub.setAuthors(dto.getAuthors());
        pub.setCvrAuthorCount(dto.getCvrAuthorCount());
        pub.setPublicationType(dto.getPublicationType());
        pub.setPublicationDate(dto.getPublicationDate());
        pub.setIndexedInScopusDate(dto.getIndexedInScopusDate());
        pub.setDoiNumber(dto.getDoiNumber());
        pub.setOrcidId(dto.getOrcidId());
        pub.setPointsClaimed(dto.getPointsClaimed());
        pub.setProofFilePath(dto.getProofFilePath());

        log.info("Updated publication ID: {}", id);
        return mapToDTO(publicationRepo.save(pub));
    }

    @Override
    public List<PublicationDTO> getPublicationsByFormId(UUID formId) {
        return publicationRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePublication(UUID id) {
        if (!publicationRepo.existsById(id)) {
            throw new ResourceNotFoundException("Publication not found with ID: " + id);
        }
        publicationRepo.deleteById(id);
        log.info("Deleted publication ID: {}", id);
    }

    private PublicationDTO mapToDTO(PartA_Publication pub) {
        PublicationDTO dto = new PublicationDTO();
        dto.setId(pub.getId());
        dto.setAppraisalFormId(pub.getAppraisalForm().getId());
        dto.setTitle(pub.getTitle());
        dto.setAuthors(pub.getAuthors());
        dto.setCvrAuthorCount(pub.getCvrAuthorCount());
        dto.setPublicationType(pub.getPublicationType());
        dto.setPublicationDate(pub.getPublicationDate());
        dto.setIndexedInScopusDate(pub.getIndexedInScopusDate());
        dto.setDoiNumber(pub.getDoiNumber());
        dto.setOrcidId(pub.getOrcidId());
        dto.setProofFilePath(pub.getProofFilePath());
        dto.setPointsClaimed((float) pub.getPointsClaimed());
        return dto;
    }

    private PartA_Publication mapToEntity(PublicationDTO dto) {
        return PartA_Publication.builder()
                .title(dto.getTitle())
                .authors(dto.getAuthors())
                .cvrAuthorCount(dto.getCvrAuthorCount())
                .publicationType(dto.getPublicationType())
                .publicationDate(dto.getPublicationDate())
                .indexedInScopusDate(dto.getIndexedInScopusDate())
                .doiNumber(dto.getDoiNumber())
                .orcidId(dto.getOrcidId())
                .proofFilePath(dto.getProofFilePath())
                .pointsClaimed(dto.getPointsClaimed())
                .build();
    }
}
