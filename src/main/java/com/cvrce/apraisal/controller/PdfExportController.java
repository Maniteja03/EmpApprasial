package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfExportController {

    private final PdfExportService pdfExportService;

    @GetMapping("/download/{formId}")
    public ResponseEntity<Resource> downloadAppraisalPdf(@PathVariable UUID formId) throws IOException {
        log.info("Downloading PDF for form ID: {}", formId);

        String filePath = pdfExportService.generateAppraisalPdf(formId);
        File file = new File(filePath);

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        FileSystemResource resource = new FileSystemResource(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(file.getName())
                .build());
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(file.length());

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }
}
