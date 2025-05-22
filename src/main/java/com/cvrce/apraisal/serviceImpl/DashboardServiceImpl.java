package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.*;
import com.cvrce.apraisal.entity.*;
import com.cvrce.apraisal.entity.review.ReviewerAssignment;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.ReviewRepository;
import com.cvrce.apraisal.repo.ReviewerAssignmentRepository;
import com.cvrce.apraisal.repo.UserRepository;
//import com.cvrce.apraisal.repository.*;
import com.cvrce.apraisal.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final AppraisalFormRepository appraisalFormRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewerAssignmentRepository reviewerAssignmentRepository;

    @Override
    public DashboardSummaryDTO getDashboardSummary() {
        long totalSubmissions = appraisalFormRepository.countSubmittedForms();
        long submittedToday = appraisalFormRepository.countSubmittedToday();
        long deptPending = reviewRepository.countPendingByLevel("DEPARTMENT");
        long committeePending = reviewRepository.countPendingByLevel("COMMITTEE");
        long chairPending = reviewRepository.countPendingByLevel("CHAIRPERSON");

        long totalApproved = reviewRepository.countApproved();
        long totalReupload = reviewRepository.countReupload();
        long deptCount = userRepository.countDepartments();

        List<ReviewerLoadDTO> reviewerLoad = reviewerAssignmentRepository.findAll().stream()
        	    .collect(Collectors.groupingBy(ReviewerAssignment::getReviewer))
        	    .entrySet().stream()
        	    .map(entry -> {
        	        User reviewer = entry.getKey();
        	        String reviewerName = reviewer.getFullName();

        	        long pending = entry.getValue().stream()
        	        		.filter(ra -> ra.getAppraisalForm().getStatus() == AppraisalStatus.SUBMITTED)
        	                .filter(ra -> reviewRepository.findByReviewerAndAppraisalForm(reviewer, ra.getAppraisalForm()).isEmpty())
        	                .count();

        	        return new ReviewerLoadDTO(reviewerName, pending);
        	    })
        	    .collect(Collectors.toList());


        return DashboardSummaryDTO.builder()
                .totalSubmissions(totalSubmissions)
                .submittedToday(submittedToday)
                .pendingDepartmentReviews(deptPending)
                .pendingCommitteeReviews(committeePending)
                .pendingChairpersonReviews(chairPending)
                .totalApproved(totalApproved)
                .totalReuploadRequested(totalReupload)
                .totalDepartments(deptCount)
                .reviewerLoad(reviewerLoad)
                .build();
    }
}
