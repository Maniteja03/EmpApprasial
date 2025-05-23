package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.PassingPercentageDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_PassingPercentage;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_PassingPercentageRepository;
import com.cvrce.apraisal.service.PartB_PassingPercentageService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBPassingPercentageDTO; // Added
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartB_PassingPercentageServiceImpl implements PartB_PassingPercentageService {

    private final PartB_PassingPercentageRepository repository;
    private final AppraisalFormRepository formRepo;
    private final UserRepository userRepository; // Added
    private final AppraisalVersionRepository versionRepository; // Added
    private final ObjectMapper objectMapper; // Added

    @Override
    @Transactional
    public PassingPercentageDTO add(PassingPercentageDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found"));

        PartB_PassingPercentage entity = PartB_PassingPercentage.builder()
                .appraisalForm(form)
                .academicYear(dto.getAcademicYear())
                .subject(dto.getSubject())
                .semester(dto.getSemester())
                .section(dto.getSection())
                .registeredStudents(dto.getRegisteredStudents())
                .passedStudents(dto.getPassedStudents())
                .pointsClaimed(dto.getPointsClaimed())
                .build();

        entity = repository.save(entity);
        log.info("Saved passing percentage with ID {}", entity.getId());
        return mapToDTO(entity);
    }

    @Override
    public List<PassingPercentageDTO> getByFormId(UUID formId) {
        return repository.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PassingPercentageDTO update(UUID id, PassingPercentageDTO dto) {
        PartB_PassingPercentage entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Passing percentage not found"));

        entity.setAcademicYear(dto.getAcademicYear());
        entity.setSubject(dto.getSubject());
        entity.setSemester(dto.getSemester());
        entity.setSection(dto.getSection());
        entity.setRegisteredStudents(dto.getRegisteredStudents());
        entity.setPassedStudents(dto.getPassedStudents());
        entity.setPointsClaimed(dto.getPointsClaimed());

        return mapToDTO(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Passing percentage not found");
        }
        repository.deleteById(id);
        log.info("Deleted passing percentage with ID {}", id);
    }

    private PassingPercentageDTO mapToDTO(PartB_PassingPercentage entity) {
        PassingPercentageDTO dto = new PassingPercentageDTO();
        dto.setId(entity.getId());
        dto.setAppraisalFormId(entity.getAppraisalForm().getId());
        dto.setAcademicYear(entity.getAcademicYear());
        dto.setSubject(entity.getSubject());
        dto.setSemester(entity.getSemester());
        dto.setSection(entity.getSection());
        dto.setRegisteredStudents(entity.getRegisteredStudents());
        dto.setPassedStudents(entity.getPassedStudents());
        dto.setPointsClaimed(entity.getPointsClaimed());
        // proofFilePath was missing in the original mapToDTO for this entity
        dto.setProofFilePath(entity.getProofFilePath()); 
        return dto;
    }

    @Override
    @Transactional
    public PassingPercentageDTO hodUpdatePassingPercentage(UUID passingPercentageId, HodUpdatePartBPassingPercentageDTO dto, UUID hodUserId) {
        User hodUser = userRepository.findById(hodUserId)
                .orElseThrow(() -> new ResourceNotFoundException("HOD User not found: " + hodUserId));

        PartB_PassingPercentage passingPercentage = repository.findById(passingPercentageId)
                .orElseThrow(() -> new ResourceNotFoundException("Passing Percentage not found: " + passingPercentageId));

        AppraisalForm form = passingPercentage.getAppraisalForm();
        if (form.getStatus() != AppraisalStatus.REUPLOAD_REQUIRED) {
            throw new IllegalStateException("HOD can only edit passing percentages when the appraisal form status is REUPLOAD_REQUIRED. Current status: " + form.getStatus());
        }

        // Update fields
        passingPercentage.setAcademicYear(dto.getAcademicYear());
        passingPercentage.setSubject(dto.getSubject());
        passingPercentage.setSemester(dto.getSemester());
        passingPercentage.setSection(dto.getSection());
        passingPercentage.setRegisteredStudents(dto.getRegisteredStudents());
        passingPercentage.setPassedStudents(dto.getPassedStudents());
        passingPercentage.setPointsClaimed(dto.getPointsClaimed());
        passingPercentage.setProofFilePath(dto.getProofFilePath());
        
        PartB_PassingPercentage updatedPassingPercentage = repository.save(passingPercentage);

        // Create AppraisalVersion
        String versionRemark = String.format("HOD %s modified Part B Passing Percentage for Subject: %s, Section: %s. Previous status: REUPLOAD_REQUIRED.",
                hodUser.getFullName(), updatedPassingPercentage.getSubject(), updatedPassingPercentage.getSection());
        
        String snapshot = null;
        try {
            snapshot = objectMapper.writeValueAsString(form); // Serialize the whole form
        } catch (JsonProcessingException e) {
            log.error("Error serializing AppraisalForm for versioning during HOD edit of passing percentage: {}", e.getMessage());
        }

        AppraisalVersion version = AppraisalVersion.builder()
                .appraisalForm(form)
                .statusAtVersion(AppraisalStatus.REUPLOAD_REQUIRED) // Status *during* which edit occurred
                .remarks(versionRemark)
                .versionTimestamp(LocalDateTime.now())
                .serializedSnapshot(snapshot)
                .build();
        versionRepository.save(version);

        log.info("HOD {} updated passing percentage {}. Form {} remains REUPLOAD_REQUIRED.", hodUserId, passingPercentageId, form.getId());
        return mapToDTO(updatedPassingPercentage);
    }
}
