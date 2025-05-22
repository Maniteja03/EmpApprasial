package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.review.ReviewDTO;
import com.cvrce.apraisal.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDTO> submitReview(@RequestBody ReviewDTO dto) {
        log.info("Review submitted for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(reviewService.submitReview(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<ReviewDTO>> getReviews(@PathVariable UUID formId) {
        log.info("Fetching reviews for form {}", formId);
        return ResponseEntity.ok(reviewService.getReviewsByFormId(formId));
    }
}
