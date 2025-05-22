package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partA.PartA_Patent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartA_PatentRepository extends JpaRepository<PartA_Patent, UUID> {
    List<PartA_Patent> findByAppraisalFormId(UUID formId);
}
