package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partC.PartC_Certification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartC_CertificationRepository extends JpaRepository<PartC_Certification, UUID> {
    List<PartC_Certification> findByAppraisalFormId(UUID formId);
}
