package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.PublicationDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Publication;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_PublicationRepository;
import com.cvrce.apraisal.service.PartA_PublicationService;
import com.cvrce.apraisal.dto.parta.HodUpdatePublicationDTO; // Added
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
public class PartA_PublicationServiceImpl implements PartA_PublicationService {

    private final PartA_PublicationRepository publicationRepo;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

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

    @Override
    @Transactional
    public PublicationDTO hodUpdatePublication(UUID publicationId, HodUpdatePublicationDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));
        // Basic check: In a real system, ensure hodUser actually has HOD role for the department of the form.
        // This might involve fetching the form, then staff user, then department, then checking HOD for that dept.
        // For now, we assume hodUserId is a validated HOD for this context.

        PartA_Publication publication = publicationRepo.findById(publicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Publication not found: " + publicationId));

        AppraisalForm form = publication.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit publications when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        publication.setTitle(dto.getTitle());
        publication.setOrcidId(dto.getOrcidId());
        publication.setDoiNumber(dto.getDoiNumber());
        publication.setAuthors(dto.getAuthors());
        publication.setCvrAuthorCount(dto.getCvrAuthorCount());
        publication.setPublicationType(dto.getPublicationType());
        publication.setPublicationDate(dto.getPublicationDate());
        publication.setIndexedInScopusDate(dto.getIndexedInScopusDate());
        publication.setPointsClaimed(dto.getPointsClaimed());
        publication.setProofFilePath(dto.getProofFilePath());

        PartA_Publication updatedPublication = publicationRepo.save(publication);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part A Publication: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedPublication.getTitle());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit: {}", e.getMessage());
            // Potentially throw a custom exception or handle, for now log and proceed with null snapshot
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated publication {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, publicationId, form.getId());
        return mapToDTO(updatedPublication); // Using existing mapToDTO method
    }
}
