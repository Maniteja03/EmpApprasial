package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partC.PartC_EBoxTraining;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartC_EBoxTrainingRepository extends JpaRepository<PartC_EBoxTraining, UUID> {
    List<PartC_EBoxTraining> findByAppraisalFormId(UUID formId);
}
