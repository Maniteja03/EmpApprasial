package com.cvrce.apraisal.service;

import java.io.IOException;
import java.util.UUID;

public interface PdfExportService {
    String generateAppraisalPdf(UUID appraisalFormId) throws IOException;
}
