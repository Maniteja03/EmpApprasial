package com.cvrce.apraisal.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.*;

@RestController
@RequestMapping("/api/files")
@Slf4j
public class FileDownloadController {

    private static final String BASE_UPLOAD_DIR = "/mnt/data/uploads";

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(
            @RequestParam("formId") String formId,
            @RequestParam("section") String section,
            @RequestParam("filename") String filename
    ) {
        try {
            String safeSection = section.toUpperCase().replaceAll("[^A-Z0-9_]", "_");
            String fullPath = BASE_UPLOAD_DIR + "/" + formId + "/" + safeSection + "/" + filename;

            File file = new File(fullPath);
            if (!file.exists()) {
                log.warn("File not found: {}", fullPath);
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename(file.getName()).build());
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("File download failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
