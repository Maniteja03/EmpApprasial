package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.appraisal.AppraisalVersionDTO;
import com.cvrce.apraisal.service.AppraisalVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appraisal-versions")
@RequiredArgsConstructor
@Slf4j
public class AppraisalVersionController {

    private final AppraisalVersionService versionService;

    @PostMapping
    public ResponseEntity<AppraisalVersionDTO> add(@RequestBody AppraisalVersionDTO dto) {
        log.info("Adding version for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(versionService.addVersion(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<AppraisalVersionDTO>> getVersions(@PathVariable UUID formId) {
        log.info("Fetching versions for form {}", formId);
        return ResponseEntity.ok(versionService.getVersionsByForm(formId));
    }
}
