package com.cvrce.apraisal.repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.enums.AppraisalStatus;

import java.util.*;

public interface AppraisalFormRepository extends JpaRepository<AppraisalForm, UUID> {
	List<AppraisalForm> findByUserId(UUID userId);

	Optional<AppraisalForm> findByUserIdAndAcademicYear(UUID userId, String academicYear);

	@Query("SELECT COUNT(a) FROM AppraisalForm a WHERE a.status = 'SUBMITTED'")
	long countSubmittedForms();


	@Query("SELECT COUNT(a) FROM AppraisalForm a WHERE a.status = 'SUBMITTED' AND a.submittedDate >= CURRENT_DATE")
	long countSubmittedToday();
	
	List<AppraisalForm> findByStatus(AppraisalStatus status);
	List<AppraisalForm> findByAcademicYear(String academicYear);




}
