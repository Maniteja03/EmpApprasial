package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.DeadlineConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeadlineConfigRepository extends JpaRepository<DeadlineConfig, Long> {
    Optional<DeadlineConfig> findByAcademicYear(String academicYear);
}
