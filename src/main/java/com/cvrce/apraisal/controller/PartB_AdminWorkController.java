package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.AdminWorkDTO;
import com.cvrce.apraisal.service.PartB_AdminWorkService;
import com.cvrce.apraisal.dto.partb.HodUpdatePartBAdminWorkDTO; // Added
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/partb/admin-work")
@RequiredArgsConstructor
@Slf4j
public class PartB_AdminWorkController {

    private final PartB_AdminWorkService adminWorkService;

    @PostMapping
    public ResponseEntity<AdminWorkDTO> add(@RequestBody AdminWorkDTO dto) {
        log.info("Adding admin work to form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(adminWorkService.add(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<AdminWorkDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching admin work for form {}", formId);
        return ResponseEntity.ok(adminWorkService.getByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminWorkDTO> update(@PathVariable UUID id, @RequestBody AdminWorkDTO dto) {
        log.info("Updating admin work with ID {}", id);
        return ResponseEntity.ok(adminWorkService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        log.info("Deleting admin work with ID {}", id);
        adminWorkService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{adminWorkId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<AdminWorkDTO> hodEditAdminWork(
            @PathVariable UUID adminWorkId,
            @RequestBody HodUpdatePartBAdminWorkDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder
        log.info("API: HOD {} editing PartB_AdminWork {}", hodUserId, adminWorkId);
        AdminWorkDTO updatedDto = adminWorkService.hodUpdateAdminWork(adminWorkId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
