package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.parta.PublicationDTO;
import com.cvrce.apraisal.service.PartA_PublicationService;
import com.cvrce.apraisal.dto.parta.HodUpdatePartAPublicationDTO; // Added
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parta/publications")
@RequiredArgsConstructor
@Slf4j
public class PartA_PublicationController {

    private final PartA_PublicationService publicationService;

    @PostMapping
    public ResponseEntity<PublicationDTO> add(@Valid @RequestBody PublicationDTO dto) {
        log.info("Adding publication to form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(publicationService.addPublication(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicationDTO> update(@PathVariable UUID id, @Valid @RequestBody PublicationDTO dto) {
        log.info("Updating publication with ID {}", id);
        return ResponseEntity.ok(publicationService.updatePublication(id, dto));
    }

    @GetMapping("/form/{formId}")
    public ResponseEntity<List<PublicationDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching publications for form {}", formId);
        return ResponseEntity.ok(publicationService.getPublicationsByFormId(formId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting publication with ID {}", id);
        publicationService.deletePublication(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{publicationId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<PublicationDTO> hodEditPublication(
            @PathVariable UUID publicationId,
            @RequestBody HodUpdatePartAPublicationDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder
        log.info("API: HOD {} editing PartA_Publication {}", hodUserId, publicationId);
        PublicationDTO updatedDto = publicationService.hodUpdatePublication(publicationId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
