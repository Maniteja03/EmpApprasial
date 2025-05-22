package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.parta.PatentDTO;
import com.cvrce.apraisal.service.PartA_PatentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parta/patents")
@RequiredArgsConstructor
@Slf4j
public class PartA_PatentController {

    private final PartA_PatentService patentService;

    @PostMapping
    public ResponseEntity<PatentDTO> add(@Valid @RequestBody PatentDTO dto) {
        log.info("Creating patent for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(patentService.addPatent(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<PatentDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Getting patents for form {}", formId);
        return ResponseEntity.ok(patentService.getPatentsByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatentDTO> update(@PathVariable UUID id, @Valid @RequestBody PatentDTO dto) {
        log.info("Updating patent ID {}", id);
        return ResponseEntity.ok(patentService.updatePatent(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting patent ID {}", id);
        patentService.deletePatent(id);
        return ResponseEntity.noContent().build();
    }
}
