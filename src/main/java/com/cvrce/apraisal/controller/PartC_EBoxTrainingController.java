package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partc.EBoxTrainingDTO;
import com.cvrce.apraisal.service.PartC_EBoxTrainingService;
import com.cvrce.apraisal.dto.partc.HodUpdatePartCEBoxTrainingDTO; // Added
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partc/ebox-training")
@RequiredArgsConstructor
@Slf4j
public class PartC_EBoxTrainingController {

    private final PartC_EBoxTrainingService eboxService;

    @PostMapping
    public ResponseEntity<EBoxTrainingDTO> add(@RequestBody EBoxTrainingDTO dto) {
        log.info("Adding E-Box Training for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(eboxService.addEBoxTraining(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<EBoxTrainingDTO>> getByForm(@PathVariable UUID formId) {
        return ResponseEntity.ok(eboxService.getEBoxTrainingsByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EBoxTrainingDTO> update(@PathVariable UUID id, @RequestBody EBoxTrainingDTO dto) {
        return ResponseEntity.ok(eboxService.updateEBoxTraining(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eboxService.deleteEBoxTraining(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{eboxTrainingId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<EBoxTrainingDTO> hodEditEBoxTraining(
            @PathVariable UUID eboxTrainingId,
            @RequestBody HodUpdatePartCEBoxTrainingDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder
        log.info("API: HOD {} editing PartC_EBoxTraining {}", hodUserId, eboxTrainingId);
        EBoxTrainingDTO updatedDto = eboxService.hodUpdateEBoxTraining(eboxTrainingId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
