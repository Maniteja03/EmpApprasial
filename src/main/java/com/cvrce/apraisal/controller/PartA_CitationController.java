package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.parta.CitationDTO;
import com.cvrce.apraisal.service.PartA_CitationService;
import com.cvrce.apraisal.dto.parta.HodUpdatePartACitationDTO; // Added
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parta/citations")
@RequiredArgsConstructor
@Slf4j
public class PartA_CitationController {

    private final PartA_CitationService citationService;

    @PostMapping
    public ResponseEntity<CitationDTO> add(@Valid @RequestBody CitationDTO dto) {
        log.info("Adding citation for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(citationService.addCitation(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CitationDTO> update(@PathVariable UUID id, @Valid @RequestBody CitationDTO dto) {
        log.info("Updating citation with ID {}", id);
        return ResponseEntity.ok(citationService.updateCitation(id, dto));
    }

    @GetMapping("/form/{formId}")
    public ResponseEntity<List<CitationDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching citations for form {}", formId);
        return ResponseEntity.ok(citationService.getCitationsByFormId(formId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting citation with ID {}", id);
        citationService.deleteCitation(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{citationId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<CitationDTO> hodEditCitation(
            @PathVariable UUID citationId,
            @RequestBody HodUpdatePartACitationDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder for this subtask
        log.info("API: HOD {} editing PartA_Citation {}", hodUserId, citationId); 
        CitationDTO updatedDto = citationService.hodUpdateCitation(citationId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
