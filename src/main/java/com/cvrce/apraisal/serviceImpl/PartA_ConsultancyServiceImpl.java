package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.parta.ConsultancyDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.partA.PartA_Consultancy;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.PartA_ConsultancyRepository;
import com.cvrce.apraisal.service.PartA_ConsultancyService;
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
public class PartA_ConsultancyServiceImpl implements PartA_ConsultancyService {

    private final PartA_ConsultancyRepository consultancyRepo;
    private final AppraisalFormRepository formRepo;

    @Override
    @Transactional
    public ConsultancyDTO addConsultancy(ConsultancyDTO dto) {
        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Appraisal form not found with ID: " + dto.getAppraisalFormId()));

        PartA_Consultancy consultancy = mapToEntity(dto);
        consultancy.setAppraisalForm(form);

        PartA_Consultancy saved = consultancyRepo.save(consultancy);
        log.info("Added consultancy with ID: {}", saved.getId());
        return mapToDTO(saved);
    }

    @Override
    public List<ConsultancyDTO> getConsultanciesByFormId(UUID formId) {
        List<PartA_Consultancy> list = consultancyRepo.findByAppraisalFormId(formId);
        log.info("Fetched {} consultancies for form {}", list.size(), formId);
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConsultancyDTO updateConsultancy(UUID id, ConsultancyDTO dto) {
        PartA_Consultancy consultancy = consultancyRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultancy not found with ID: " + id));

        consultancy.setConsultancyTitle(dto.getConsultancyTitle());
        consultancy.setDescription(dto.getDescription());
        consultancy.setInvestigators(dto.getInvestigators());
        consultancy.setAmount(dto.getAmount());
        consultancy.setSanctionedDate(dto.getSanctionedDate());
        consultancy.setSanctionedYear(dto.getSanctionedYear());
        consultancy.setPointsClaimed(dto.getPointsClaimed());
        consultancy.setProofFilePath(dto.getProofFilePath());

        PartA_Consultancy updated = consultancyRepo.save(consultancy);
        log.info("Updated consultancy with ID: {}", id);
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteConsultancy(UUID id) {
        if (!consultancyRepo.existsById(id)) {
            throw new ResourceNotFoundException("Consultancy not found with ID: " + id);
        }
        consultancyRepo.deleteById(id);
        log.info("Deleted consultancy with ID: {}", id);
    }

    private ConsultancyDTO mapToDTO(PartA_Consultancy c) {
        ConsultancyDTO dto = new ConsultancyDTO();
        dto.setId(c.getId());
        dto.setAppraisalFormId(c.getAppraisalForm().getId());
        dto.setConsultancyTitle(c.getConsultancyTitle());
        dto.setDescription(c.getDescription());
        dto.setInvestigators(c.getInvestigators());
        dto.setAmount(c.getAmount());
        dto.setSanctionedDate(c.getSanctionedDate());
        dto.setSanctionedYear(c.getSanctionedYear());
        dto.setPointsClaimed(c.getPointsClaimed());
        dto.setProofFilePath(c.getProofFilePath());
        return dto;
    }

    private PartA_Consultancy mapToEntity(ConsultancyDTO dto) {
        return PartA_Consultancy.builder()
                .consultancyTitle(dto.getConsultancyTitle())
                .description(dto.getDescription())
                .investigators(dto.getInvestigators())
                .amount(dto.getAmount())
                .sanctionedDate(dto.getSanctionedDate())
                .sanctionedYear(dto.getSanctionedYear())
                .pointsClaimed(dto.getPointsClaimed())
                .proofFilePath(dto.getProofFilePath())
                .build();
    }
}
