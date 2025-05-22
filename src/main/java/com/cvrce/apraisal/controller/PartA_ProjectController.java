package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.parta.ProjectDTO;
import com.cvrce.apraisal.service.PartA_ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parta/projects")
@RequiredArgsConstructor
@Slf4j
public class PartA_ProjectController {

    private final PartA_ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectDTO> add(@RequestBody ProjectDTO dto) {
        log.info("Adding project to form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(projectService.addProject(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<ProjectDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching projects for form {}", formId);
        return ResponseEntity.ok(projectService.getProjectsByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> update(@PathVariable UUID id, @RequestBody ProjectDTO dto) {
        log.info("Updating project {}", id);
        return ResponseEntity.ok(projectService.updateProject(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
