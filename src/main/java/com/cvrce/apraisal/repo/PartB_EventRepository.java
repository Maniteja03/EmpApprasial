package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.partB.PartB_Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartB_EventRepository extends JpaRepository<PartB_Event, UUID> {
    List<PartB_Event> findByAppraisalFormId(UUID formId);
}
