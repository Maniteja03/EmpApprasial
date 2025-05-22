package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.ProjectGuidanceDTO;
import com.cvrce.apraisal.service.PartB_ProjectGuidanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/partb/project-guidance")
@RequiredArgsConstructor
@Slf4j
public class PartB_ProjectGuidanceController {

    private final PartB_ProjectGuidanceService service;

    @PostMapping
    public ResponseEntity<ProjectGuidanceDTO> add(@RequestBody ProjectGuidanceDTO dto) {
        log.info("Adding project guidance to form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(service.add(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<ProjectGuidanceDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching project guidance records for form {}", formId);
        return ResponseEntity.ok(service.getByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectGuidanceDTO> update(@PathVariable UUID id, @RequestBody ProjectGuidanceDTO dto) {
        log.info("Updating project guidance record with ID {}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting project guidance with ID {}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
