package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.SummaryEvaluation;
import com.cvrce.apraisal.entity.partA.*;
import com.cvrce.apraisal.entity.partB.*;
import com.cvrce.apraisal.entity.partC.*;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.*;
import com.cvrce.apraisal.service.PdfExportService;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfExportServiceImpl implements PdfExportService {

    private final AppraisalFormRepository formRepo;
    private final SummaryEvaluationRepository summaryRepo;

    private final PartA_PublicationRepository publicationRepo;
    private final PartA_CitationRepository citationRepo;
    private final PartA_ConsultancyRepository consultancyRepo;
    private final PartA_PatentRepository patentRepo;
    private final PartA_ProjectRepository projectRepo;

    private final PartB_AwardRepository awardRepo;
    private final PartB_AdminWorkRepository adminWorkRepo;
    private final PartB_EventRepository eventRepo;
    private final PartB_ResearchGuidanceRepository researchRepo;
    private final PartB_FeedbackScoreRepository feedbackRepo;
    private final PartB_PassingPercentageRepository passingRepo;
    private final PartB_ProjectGuidanceRepository guidanceRepo;

    private final PartC_CertificationRepository certRepo;
    private final PartC_EBoxTrainingRepository eboxRepo;

    private static final String SAVE_DIR = "/mnt/data/appraisal_pdfs";

    @Override
    public String generateAppraisalPdf(UUID formId) throws IOException {
        AppraisalForm form = formRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        // Ensure directory exists
        File dir = new File(SAVE_DIR);
        if (!dir.exists()) dir.mkdirs();

        // Filename and path
        String safeEmpId = form.getUser().getEmployeeId().replaceAll("[^a-zA-Z0-9_-]", "_");
        String fileName = "Appraisal_" + safeEmpId + "_" + form.getAcademicYear() + ".pdf";
        String filePath = SAVE_DIR + "/" + fileName;

        // Setup PDF
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf);
        var font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        doc.setFont(font);

        // Header
        doc.add(new Paragraph("CVR College of Engineering")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(16));

        doc.add(new Paragraph("Staff Appraisal Form - " + form.getAcademicYear())
                .setTextAlignment(TextAlignment.CENTER).setFontSize(14).setBold());

        doc.add(new Paragraph("Name: " + form.getUser().getFullName()));
        doc.add(new Paragraph("Emp ID: " + form.getUser().getEmployeeId()));
        doc.add(new Paragraph("Department: " + form.getUser().getDepartment().getName()));
        doc.add(new Paragraph(" ")); // spacing

        // Part A
        doc.add(new Paragraph("Part A: Research Contributions").setBold().setFontSize(13));
        addSection(doc, "Publications", publicationRepo.findByAppraisalFormId(formId));
        addSection(doc, "Citations", citationRepo.findByAppraisalFormId(formId));
        addSection(doc, "Patents", patentRepo.findByAppraisalFormId(formId));
        addSection(doc, "Consultancies", consultancyRepo.findByAppraisalFormId(formId));
        addSection(doc, "Projects", projectRepo.findByAppraisalFormId(formId));

        // Part B
        doc.add(new Paragraph("Part B: Academic Activities").setBold().setFontSize(13));
        addSection(doc, "Awards", awardRepo.findByAppraisalFormId(formId));
        addSection(doc, "Admin Work", adminWorkRepo.findByAppraisalFormId(formId));
        addSection(doc, "Events", eventRepo.findByAppraisalFormId(formId));
        addSection(doc, "Research Guidance", researchRepo.findByAppraisalFormId(formId));
        var feedback = feedbackRepo.findByAppraisalFormId(formId);
        addSection(doc, "Feedback Scores", feedback != null ? List.of(feedback) : List.of());
        addSection(doc, "Passing %", passingRepo.findByAppraisalFormId(formId));
        addSection(doc, "Project Guidance", guidanceRepo.findByAppraisalFormId(formId));

        // Part C
        doc.add(new Paragraph("Part C: Certifications and Training").setBold().setFontSize(13));
        addSection(doc, "Certifications", certRepo.findByAppraisalFormId(formId));
        addSection(doc, "EBox Training", eboxRepo.findByAppraisalFormId(formId));

        // Summary
        doc.add(new Paragraph("Summary Evaluation").setBold().setFontSize(13));
        SummaryEvaluation summary = summaryRepo.findByAppraisalFormId(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Summary not available"));
        doc.add(new Paragraph("Points Claimed: " + summary.getTotalPointsClaimed()));
        doc.add(new Paragraph("Points Awarded: " + summary.getTotalPointsAwarded()));
        doc.add(new Paragraph("Final Recommendation: " + summary.getFinalRecommendation()));
        doc.add(new Paragraph("Remarks: " + summary.getRemarks()));

        doc.close();
        log.info("Generated PDF saved at {}", filePath);
        return filePath;
    }

    private void addSection(Document doc, String title, List<?> items) {
        doc.add(new Paragraph(title).setUnderline());
        if (items.isEmpty()) {
            doc.add(new Paragraph("  - No records."));
        } else {
            for (Object item : items) {
                doc.add(new Paragraph("  • " + item.toString()));
            }
        }
        doc.add(new Paragraph(" "));
    }
}
