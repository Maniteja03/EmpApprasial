package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partB.PartB_Award;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartB_AwardRepository extends JpaRepository<PartB_Award, UUID> {
    List<PartB_Award> findByAppraisalFormId(UUID formId);
}
