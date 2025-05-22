package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partB.PartB_PassingPercentage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartB_PassingPercentageRepository extends JpaRepository<PartB_PassingPercentage, UUID> {
    List<PartB_PassingPercentage> findByAppraisalFormId(UUID formId);
}
