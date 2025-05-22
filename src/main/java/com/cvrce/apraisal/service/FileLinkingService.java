package com.cvrce.apraisal.service;

import java.util.UUID;

public interface FileLinkingService {
    void linkFileToEntity(UUID formId, String section, String filePath);
}
