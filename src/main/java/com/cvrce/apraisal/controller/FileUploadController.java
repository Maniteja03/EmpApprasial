package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.UploadedDocumentDTO;
import com.cvrce.apraisal.service.FileUploadService;
import com.cvrce.apraisal.service.UploadedDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    private final FileUploadService fileUploadService;
    private final UploadedDocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadProofFile(
            @RequestParam("formId") UUID formId,
            @RequestParam("section") String section,
            @RequestParam("file") MultipartFile file
    ) {
        try {
            log.info("Uploading file for form {} in section {}", formId, section);
            String filePath = fileUploadService.uploadProofFile(section, formId, file);
            return ResponseEntity.ok("File uploaded and linked at: " + filePath);
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }
    

    @GetMapping("/metadata")
    public ResponseEntity<List<UploadedDocumentDTO>> getUploadedFiles(
            @RequestParam("formId") UUID formId,
            @RequestParam(value = "section", required = false) String section
    ) {
        List<UploadedDocumentDTO> result = documentService.getDocuments(formId, section);
        return ResponseEntity.ok(result);
    }
}
