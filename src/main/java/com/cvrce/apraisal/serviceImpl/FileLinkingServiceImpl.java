package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.entity.partA.*;
import com.cvrce.apraisal.entity.partB.*;
import com.cvrce.apraisal.repo.*;
import com.cvrce.apraisal.service.FileLinkingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileLinkingServiceImpl implements FileLinkingService {

    private final PartA_PublicationRepository publicationRepo;
    private final PartA_PatentRepository patentRepo;
    private final PartA_ProjectRepository projectRepo;
    private final PartA_ConsultancyRepository consultancyRepo;
    private final PartA_CitationRepository citationRepo;

    private final PartB_AwardRepository awardRepo;
    private final PartB_AdminWorkRepository adminRepo;
    private final PartB_EventRepository eventRepo;
    private final PartB_ResearchGuidanceRepository researchRepo;
    private final PartB_PassingPercentageRepository passingRepo;
    private final PartB_ProjectGuidanceRepository projectGuidanceRepo;

    @Override
    public void linkFileToEntity(UUID formId, String section, String filePath) {
        switch (section.toUpperCase()) {
            case "PUBLICATION" -> publicationRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        publicationRepo.save(e);
                    });
            case "PATENT" -> patentRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        patentRepo.save(e);
                    });
            case "PROJECT" -> projectRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        projectRepo.save(e);
                    });
            case "CONSULTANCY" -> consultancyRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        consultancyRepo.save(e);
                    });
            case "CITATION" -> citationRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        citationRepo.save(e);
                    });

            // Part B
            case "AWARD" -> awardRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        awardRepo.save(e);
                    });
            case "ADMIN_WORK" -> adminRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        adminRepo.save(e);
                    });
            case "EVENT" -> eventRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        eventRepo.save(e);
                    });
            case "RESEARCH_GUIDANCE" -> researchRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        researchRepo.save(e);
                    });
            case "PASSING_PERCENTAGE" -> passingRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        passingRepo.save(e);
                    });
            case "PROJECT_GUIDANCE" -> projectGuidanceRepo.findByAppraisalFormId(formId).stream()
                    .findFirst().ifPresent(e -> {
                        e.setProofFilePath(filePath);
                        projectGuidanceRepo.save(e);
                    });
            default -> log.warn("Unknown section '{}' for file linking", section);
        }

        log.info("File linked to section {} for form {}", section, formId);
    }
}
