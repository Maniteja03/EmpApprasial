package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partB.PartB_ResearchGuidance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartB_ResearchGuidanceRepository extends JpaRepository<PartB_ResearchGuidance, UUID> {
    List<PartB_ResearchGuidance> findByAppraisalFormId(UUID formId);
}
