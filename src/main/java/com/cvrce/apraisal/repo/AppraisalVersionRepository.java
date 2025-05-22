package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.AppraisalVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AppraisalVersionRepository extends JpaRepository<AppraisalVersion, UUID> {
    List<AppraisalVersion> findByAppraisalFormIdOrderByVersionTimestampDesc(UUID appraisalFormId);
}
