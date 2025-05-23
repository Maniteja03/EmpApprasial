package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.FeedbackScoreDTO;
import com.cvrce.apraisal.service.PartB_FeedbackScoreService;

import com.cvrce.apraisal.dto.partb.HodUpdatePartBFeedbackScoreDTO; // Added
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
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

    @PutMapping("/{feedbackScoreId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<FeedbackScoreDTO> hodEditFeedbackScore(
            @PathVariable UUID feedbackScoreId,
            @RequestBody HodUpdatePartBFeedbackScoreDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder
        log.info("API: HOD {} editing PartB_FeedbackScore {}", hodUserId, feedbackScoreId);
        FeedbackScoreDTO updatedDto = feedbackService.hodUpdateFeedbackScore(feedbackScoreId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
