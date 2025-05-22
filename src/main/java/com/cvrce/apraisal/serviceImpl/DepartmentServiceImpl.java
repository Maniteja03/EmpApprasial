package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.department.*;
import com.cvrce.apraisal.entity.Department;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.DepartmentRepository;
import com.cvrce.apraisal.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    @Override
    public DepartmentDTO createDepartment(DepartmentCreateDTO dto) {
        Department department = new Department();
        department.setName(dto.getName());
        Department saved = departmentRepository.save(department);
        log.info("Created department: {}", saved.getName());
        return mapToDTO(saved);
    }

    @Override
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        return mapToDTO(department);
    }

    @Override
    public DepartmentDTO updateDepartment(Long id, DepartmentCreateDTO dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        department.setName(dto.getName());
        Department updated = departmentRepository.save(department);
        log.info("Updated department ID {} to name '{}'", id, dto.getName());
        return mapToDTO(updated);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        departmentRepository.delete(department);
        log.info("Deleted department ID {}", id);
    }

    private DepartmentDTO mapToDTO(Department dept) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(dept.getId());
        dto.setName(dept.getName());
        return dto;
    }
}
