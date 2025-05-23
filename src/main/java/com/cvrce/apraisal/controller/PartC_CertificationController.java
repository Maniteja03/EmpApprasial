package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partc.CertificationDTO;
import com.cvrce.apraisal.service.PartC_CertificationService;
import com.cvrce.apraisal.dto.partc.HodUpdatePartCCertificationDTO; // Added
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partc/certifications")
@RequiredArgsConstructor
@Slf4j
public class PartC_CertificationController {

    private final PartC_CertificationService certService;

    @PostMapping
    public ResponseEntity<CertificationDTO> add(@RequestBody CertificationDTO dto) {
        log.info("Adding certification for form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(certService.addCertification(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<CertificationDTO>> getByForm(@PathVariable UUID formId) {
        return ResponseEntity.ok(certService.getCertificationsByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CertificationDTO> update(@PathVariable UUID id, @RequestBody CertificationDTO dto) {
        return ResponseEntity.ok(certService.updateCertification(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        certService.deleteCertification(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{certificationId}/hod-edit")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<CertificationDTO> hodEditCertification(
            @PathVariable UUID certificationId,
            @RequestBody HodUpdatePartCCertificationDTO dto
    ) {
        UUID hodUserId = UUID.randomUUID(); // Placeholder
        log.info("API: HOD {} editing PartC_Certification {}", hodUserId, certificationId);
        CertificationDTO updatedDto = certService.hodUpdateCertification(certificationId, dto, hodUserId);
        return ResponseEntity.ok(updatedDto);
    }
}
