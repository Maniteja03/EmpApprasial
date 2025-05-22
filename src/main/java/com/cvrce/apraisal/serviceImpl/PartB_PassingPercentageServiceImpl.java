package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partb.PassingPercentageDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partB.PartB_PassingPercentage;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartB_PassingPercentageRepository;
import com.cvrce.apraisal.service.PartB_PassingPercentageService;

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
        return dto;
    }
}
