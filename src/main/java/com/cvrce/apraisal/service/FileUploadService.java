package com.cvrce.apraisal.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

public interface FileUploadService {
    String uploadProofFile(String section, UUID formId, MultipartFile file) throws IOException;
}
