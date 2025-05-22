package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.SummaryEvaluationDTO;
import com.cvrce.apraisal.service.SummaryEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Slf4j
public class SummaryEvaluationController {

    private final SummaryEvaluationService summaryService;

    @GetMapping("/{formId}")
    public ResponseEntity<SummaryEvaluationDTO> getSummary(@PathVariable UUID formId) {
        log.info("Fetching summary for form {}", formId);
        return ResponseEntity.ok(summaryService.getSummaryByFormId(formId));
    }

    @PostMapping
    public ResponseEntity<SummaryEvaluationDTO> saveOrUpdate(@RequestBody SummaryEvaluationDTO dto) {
        log.info("Saving/updating summary for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(summaryService.saveOrUpdateSummary(dto), HttpStatus.CREATED);
    }
}
