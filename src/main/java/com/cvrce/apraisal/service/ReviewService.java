package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.review.ReviewDTO;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ReviewDTO submitReview(ReviewDTO dto);
    List<ReviewDTO> getReviewsByFormId(UUID formId);
}
