package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.FeedbackScoreDTO;
import com.cvrce.apraisal.service.PartB_FeedbackScoreService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/partb/feedback")
@RequiredArgsConstructor
@Slf4j
public class PartB_FeedbackScoreController {

    private final PartB_FeedbackScoreService feedbackService;

    @PostMapping
    public ResponseEntity<FeedbackScoreDTO> addOrUpdate(@Valid @RequestBody FeedbackScoreDTO dto) {
        log.info("Saving feedback score for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(feedbackService.addOrUpdateFeedbackScore(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<FeedbackScoreDTO> getByForm(@PathVariable UUID formId) {
        log.info("Fetching feedback score for form {}", formId);
        return ResponseEntity.ok(feedbackService.getByFormId(formId));
    }
}
