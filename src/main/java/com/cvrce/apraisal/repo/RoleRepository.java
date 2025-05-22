package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name); // e.g., "STAFF", "HOD"
    
    boolean existsByName(String name);
}
