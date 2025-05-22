package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partB.PartB_ProjectGuidance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartB_ProjectGuidanceRepository extends JpaRepository<PartB_ProjectGuidance, UUID> {
    List<PartB_ProjectGuidance> findByAppraisalFormId(UUID formId);
}
