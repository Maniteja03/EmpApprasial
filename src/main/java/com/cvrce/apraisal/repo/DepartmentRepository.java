package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.Department;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
	Optional<Department> findByName(String name);
}
