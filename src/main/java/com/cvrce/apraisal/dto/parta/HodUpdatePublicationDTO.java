package com.cvrce.apraisal.dto.parta;

import com.cvrce.apraisal.enums.PublicationType;
import lombok.Data;
import java.time.LocalDate;

@Data
public class HodUpdatePublicationDTO {
    private String title;
    private String orcidId;
    private String doiNumber;
    private String authors;
    private int cvrAuthorCount;
    private PublicationType publicationType;
    private LocalDate publicationDate;
    private LocalDate indexedInScopusDate;
    private double pointsClaimed;
    // proofFilePath might be updated differently (e.g. if HOD helps upload a new file)
    // For now, let's assume HOD can also correct the path string if needed.
    private String proofFilePath;
}
