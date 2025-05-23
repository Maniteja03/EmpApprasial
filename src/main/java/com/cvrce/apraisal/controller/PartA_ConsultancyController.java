package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.parta.ConsultancyDTO;
import com.cvrce.apraisal.service.PartA_ConsultancyService;

import com.cvrce.apraisal.dto.parta.HodUpdatePartAConsultancyDTO; // Added
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parta/consultancies")
@RequiredArgsConstructor
@Slf4j
public class PartA_ConsultancyController {

    private final PartA_ConsultancyService consultancyService;

    @PostMapping
    public ResponseEntity<ConsultancyDTO> add(@Valid @RequestBody ConsultancyDTO dto) {
        log.info("Adding consultancy to form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(consultancyService.addConsultancy(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<ConsultancyDTO>> getByForm(@PathVariable UUID formId) {
        return ResponseEntity.ok(consultancyService.getConsultanciesByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsultancyDTO> update(@PathVariable UUID id,@Valid @RequestBody ConsultancyDTO dto) {
        return ResponseEntity.ok(consultancyService.updateConsultancy(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        consultancyService.deleteConsultancy(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{consultancyId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<ConsultancyDTO> hodEditConsultancy(
            @PathVariable UUID consultancyId,
            @RequestBody HodUpdatePartAConsultancyDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder
        log.info("API: HOD {} editing PartA_Consultancy {}", hodUserId, consultancyId);
        ConsultancyDTO updatedDto = consultancyService.hodUpdateConsultancy(consultancyId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
