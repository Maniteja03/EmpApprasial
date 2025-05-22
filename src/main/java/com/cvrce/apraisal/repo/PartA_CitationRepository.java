package com.cvrce.apraisal.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cvrce.apraisal.entity.partA.PartA_Citation;

import java.util.List;
import java.util.UUID;

public interface PartA_CitationRepository extends JpaRepository<PartA_Citation, UUID> {
    List<PartA_Citation> findByAppraisalFormId(UUID formId);
}
