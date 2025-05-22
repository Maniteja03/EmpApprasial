package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partA.PartA_Publication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartA_PublicationRepository extends JpaRepository<PartA_Publication, UUID> {
    List<PartA_Publication> findByAppraisalFormId(UUID appraisalFormId);
}
