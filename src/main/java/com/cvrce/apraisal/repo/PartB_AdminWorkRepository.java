package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partB.PartB_AdminWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartB_AdminWorkRepository extends JpaRepository<PartB_AdminWork, UUID> {
    List<PartB_AdminWork> findByAppraisalFormId(UUID formId);
}
