package com.cvrce.apraisal.repo;

import com.cvrce.apraisal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.*;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    
    @Query("SELECT COUNT(DISTINCT u.department.name) FROM User u")
    long countDepartments();
    @Query("SELECT u FROM User u WHERE u.department.id = :deptId AND u.deleted = false")
    List<User> findByDepartmentId(Long deptId);

}
