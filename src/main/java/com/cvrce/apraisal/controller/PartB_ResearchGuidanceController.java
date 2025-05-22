package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.ResearchGuidanceDTO;
import com.cvrce.apraisal.service.PartB_ResearchGuidanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/partb/research-guidance")
@RequiredArgsConstructor
@Slf4j
public class PartB_ResearchGuidanceController {

    private final PartB_ResearchGuidanceService service;

    @PostMapping
    public ResponseEntity<ResearchGuidanceDTO> add(@RequestBody ResearchGuidanceDTO dto) {
        log.info("Adding research guidance for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(service.add(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<ResearchGuidanceDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching research guidance for form {}", formId);
        return ResponseEntity.ok(service.getByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResearchGuidanceDTO> update(@PathVariable UUID id, @RequestBody ResearchGuidanceDTO dto) {
        log.info("Updating research guidance with ID {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting research guidance with ID {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
