package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.review.ReviewDTO;
import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.AppraisalVersion;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.entity.review.Review;
import com.cvrce.apraisal.enums.ReviewDecision;
import com.cvrce.apraisal.enums.ReviewLevel;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.AppraisalVersionRepository;
import com.cvrce.apraisal.repo.ReviewRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.ReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepo;
    private final AppraisalFormRepository formRepo;
    private final AppraisalVersionRepository versionRepo;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReviewDTO submitReview(ReviewDTO dto) {
        User reviewer = userRepo.findById(dto.getReviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found"));

        AppraisalForm form = formRepo.findById(dto.getAppraisalFormId())
                .orElseThrow(() -> new ResourceNotFoundException("Form not found"));

        Optional<Review> existing = reviewRepo.findByReviewerAndAppraisalForm(reviewer, form);
        if (existing.isPresent()) {
            throw new IllegalStateException("Reviewer already reviewed this form");
        }

        Review review = Review.builder()
                .reviewer(reviewer)
                .appraisalForm(form)
                .decision(ReviewDecision.valueOf(dto.getDecision()))
                .remarks(dto.getRemarks())
                .level(ReviewLevel.valueOf(dto.getLevel()))
                .reviewedAt(LocalDateTime.now())
                .build();

        Review saved = reviewRepo.save(review);

        versionRepo.save(
                AppraisalVersion.builder()
                        .appraisalForm(form)
                        .statusAtVersion(form.getStatus())
                        .remarks("Reviewed by " + review.getLevel() + " - " + review.getDecision())
                        .versionTimestamp(LocalDateTime.now())
                        .serializedSnapshot(serializeForm(form))
                        .build()
        );

        log.info("Review saved for form {} by {}", dto.getAppraisalFormId(), dto.getReviewerId());
        return mapToDTO(saved);
    }

    private String serializeForm(AppraisalForm form) {
        try {
            return objectMapper.writeValueAsString(form);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize form", e);
            throw new RuntimeException("Failed to serialize form");
        }
    }

    @Override
    public List<ReviewDTO> getReviewsByFormId(UUID formId) {
        return reviewRepo.findByAppraisalFormId(formId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private ReviewDTO mapToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setReviewerId(review.getReviewer().getId());
        dto.setAppraisalFormId(review.getAppraisalForm().getId());
        dto.setDecision(review.getDecision().name());
        dto.setRemarks(review.getRemarks());
        dto.setLevel(review.getLevel().name());
        dto.setReviewedAt(review.getReviewedAt());
        return dto;
    }
}
