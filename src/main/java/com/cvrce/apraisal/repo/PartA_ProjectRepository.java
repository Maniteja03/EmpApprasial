package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partA.PartA_Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartA_ProjectRepository extends JpaRepository<PartA_Project, UUID> {
    List<PartA_Project> findByAppraisalFormId(UUID formId);
}
