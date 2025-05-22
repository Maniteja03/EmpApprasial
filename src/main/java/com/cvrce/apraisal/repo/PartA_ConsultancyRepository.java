package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partA.PartA_Consultancy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartA_ConsultancyRepository extends JpaRepository<PartA_Consultancy, UUID> {
    List<PartA_Consultancy> findByAppraisalFormId(UUID formId);
}
