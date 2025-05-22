package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.UploadedDocumentDTO;
import com.cvrce.apraisal.entity.document.UploadedDocument;
import com.cvrce.apraisal.repo.UploadedDocumentRepository;
import com.cvrce.apraisal.service.UploadedDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UploadedDocumentServiceImpl implements UploadedDocumentService {

    private final UploadedDocumentRepository documentRepo;

    @Override
    public List<UploadedDocumentDTO> getDocuments(UUID formId, String section) {
        List<UploadedDocument> docs = (section == null || section.isBlank())
                ? documentRepo.findByAppraisalFormId_Id(formId)
                : documentRepo.findByAppraisalFormId_IdAndSectionIgnoreCase(formId, section);

        return docs.stream().map(doc -> UploadedDocumentDTO.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .filePath(doc.getFilePath())
                .fileSize(doc.getFileSize())
                .section(doc.getSection())
                .appraisalFormId(doc.getAppraisalForm().getId())
                .uploadedAt(doc.getUploadedAt())
                .build()
        ).collect(Collectors.toList());
    }
}
