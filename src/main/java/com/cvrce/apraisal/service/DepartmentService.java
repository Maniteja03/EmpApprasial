package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.department.*;

import java.util.List;

public interface DepartmentService {
    DepartmentDTO createDepartment(DepartmentCreateDTO dto);
    List<DepartmentDTO> getAllDepartments();
    DepartmentDTO getDepartmentById(Long id);
    DepartmentDTO updateDepartment(Long id, DepartmentCreateDTO dto);
    void deleteDepartment(Long id); // Soft delete optional, if implemented
}
