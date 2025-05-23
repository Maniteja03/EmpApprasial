package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.PassingPercentageDTO;
import com.cvrce.apraisal.service.PartB_PassingPercentageService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBPassingPercentageDTO; // Added
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/partb/passing-percentage")
@RequiredArgsConstructor
@Slf4j
public class PartB_PassingPercentageController {

    private final PartB_PassingPercentageService service;

    @PostMapping
    public ResponseEntity<PassingPercentageDTO> add(@RequestBody PassingPercentageDTO dto) {
        log.info("Adding passing percentage for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(service.add(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<PassingPercentageDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching passing percentage records for form {}", formId);
        return ResponseEntity.ok(service.getByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PassingPercentageDTO> update(@PathVariable UUID id, @RequestBody PassingPercentageDTO dto) {
        log.info("Updating passing percentage with ID {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting passing percentage with ID {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{passingPercentageId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<PassingPercentageDTO> hodEditPassingPercentage(
            @PathVariable UUID passingPercentageId,
            @RequestBody HodUpdatePartBPassingPercentageDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder
        log.info("API: HOD {} editing PartB_PassingPercentage {}", hodUserId, passingPercentageId);
        PassingPercentageDTO updatedDto = service.hodUpdatePassingPercentage(passingPercentageId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
