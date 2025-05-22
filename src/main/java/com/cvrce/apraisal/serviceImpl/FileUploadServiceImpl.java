package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.document.UploadedDocument;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.UploadedDocumentRepository;
import com.cvrce.apraisal.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadServiceImpl implements FileUploadService {

    private static final String BASE_UPLOAD_DIR = "/mnt/data/uploads";

    private final UploadedDocumentRepository documentRepo;
    private final AppraisalFormRepository appraisalFormRepo;

    @Override
    public String uploadProofFile(String section, UUID formId, MultipartFile file) throws IOException {
        String safeSection = section.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        String uploadDir = BASE_UPLOAD_DIR + "/" + formId + "/" + safeSection;

        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadDir, fileName);
        Files.write(path, file.getBytes());

        String relativePath = path.toString().replace(BASE_UPLOAD_DIR, "");

        // 🔹 Save file metadata in DB
        AppraisalForm form = appraisalFormRepo.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("AppraisalForm"));

        UploadedDocument document = UploadedDocument.builder()
                .fileName(file.getOriginalFilename())
                .filePath(relativePath)
                .fileSize(file.getSize())
                .section(section)
                .appraisalForm(form)
                .uploadedAt(LocalDateTime.now())
                .build();

        documentRepo.save(document);

        log.info("File uploaded and metadata saved: {}", path);
        return relativePath;
    }
}
