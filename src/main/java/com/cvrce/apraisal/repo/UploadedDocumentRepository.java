package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.document.UploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, UUID> {
    List<UploadedDocument> findByAppraisalFormId_Id(UUID formId);
    List<UploadedDocument> findByAppraisalFormId_IdAndSectionIgnoreCase(UUID formId, String section);
}

