package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.partc.EBoxTrainingDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partC.PartC_EBoxTraining;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartC_EBoxTrainingRepository;
import com.cvrce.apraisal.service.PartC_EBoxTrainingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartC_EBoxTrainingServiceImpl implements PartC_EBoxTrainingService {

    private final PartC_EBoxTrainingRepository eboxRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    public EBoxTrainingDTO addEBoxTraining(EBoxTrainingDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal Form not found"));

        PartC_EBoxTraining training = PartC_EBoxTraining.builder()
                .appraisalForm(form)
                .academicYear(dto.getAcademicYear())
                .courseTitle(dto.getCourseTitle())
                .branch(dto.getBranch())
                .semester(dto.getSemester())
                .studentsAllotted(dto.getStudentsAllotted())
                .studentsCompleted(dto.getStudentsCompleted())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();

        return mapToDTO(eboxRepo.save(training));
    }

    @Override
    public List<EBoxTrainingDTO> getEBoxTrainingsByFormId(UUID formId) {
        return eboxRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public EBoxTrainingDTO updateEBoxTraining(UUID id, EBoxTrainingDTO dto) {
        PartC_EBoxTraining training = eboxRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("E-Box Training not found"));

        training.setAcademicYear(dto.getAcademicYear());
        training.setCourseTitle(dto.getCourseTitle());
        training.setBranch(dto.getBranch());
        training.setSemester(dto.getSemester());
        training.setStudentsAllotted(dto.getStudentsAllotted());
        training.setStudentsCompleted(dto.getStudentsCompleted());
        training.setPointsClaimed(dto.getPointsClaimed());
        training.setProofFilePath(dto.getProofFilePath());

        return mapToDTO(eboxRepo.save(training));
    }

    @Override
    public void deleteEBoxTraining(UUID id) {
        if (!eboxRepo.existsById(id)) {
            throw new ResourceNotFoundException("E-Box Training not found");
        }
        eboxRepo.deleteById(id);
    }

    private EBoxTrainingDTO mapToDTO(PartC_EBoxTraining training) {
        EBoxTrainingDTO dto = new EBoxTrainingDTO();
        dto.setId(training.getId());
        dto.setAppraisalFormId(training.getAppraisalForm().getId());
        dto.setAcademicYear(training.getAcademicYear());
        dto.setCourseTitle(training.getCourseTitle());
        dto.setBranch(training.getBranch());
        dto.setSemester(training.getSemester());
        dto.setStudentsAllotted(training.getStudentsAllotted());
        dto.setStudentsCompleted(training.getStudentsCompleted());
        dto.setPointsClaimed(training.getPointsClaimed());
        dto.setProofFilePath(training.getProofFilePath());
        return dto;
    }
}
