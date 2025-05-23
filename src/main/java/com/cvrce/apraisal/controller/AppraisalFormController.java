package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.appraisal.AppraisalFormDTO;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.enums.ReviewLevel; // Added
import com.cvrce.apraisal.security.UserPrincipal;
import com.cvrce.apraisal.service.AppraisalFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize; // Added
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException; // Added

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appraisals")
@RequiredArgsConstructor
@Slf4j
public class AppraisalFormController {

    private final AppraisalFormService formService;

    private UUID getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        } else {
            throw new IllegalStateException("User not authenticated or invalid principal");
        }
    }

    @PostMapping("/draft")
    public ResponseEntity<AppraisalFormDTO> createDraft(@RequestParam String year) {
        UUID userId = getCurrentUserId();
        log.info("Creating draft form for user {} for year {}", userId, year);
        AppraisalFormDTO draft = formService.createDraftForm(year, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(draft);
    }

    @GetMapping("/my")
    public ResponseEntity<List<AppraisalFormDTO>> getMyForms() {
        UUID userId = getCurrentUserId();
        List<AppraisalFormDTO> forms = formService.getMySubmissions(userId);
        return ResponseEntity.ok(forms);
    }

    @PostMapping("/submit/{formId}")
    public ResponseEntity<String> submitForm(@PathVariable UUID formId) {
        formService.submit(formId);
        return ResponseEntity.ok("Form submitted");
    }

    @GetMapping("/filter")
    public ResponseEntity<List<AppraisalFormDTO>> filterByStatus(@RequestParam AppraisalStatus status) {
        List<AppraisalFormDTO> filteredForms = formService.filterByStatus(status);
        return ResponseEntity.ok(filteredForms);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppraisalFormDTO> getById(@PathVariable UUID id) {
        AppraisalFormDTO form = formService.getById(id);
        return ResponseEntity.ok(form);
    }

    @PostMapping("/{formId}/hod-finalize-corrections")
    @PreAuthorize("hasAuthority('ROLE_HOD')")
    public ResponseEntity<AppraisalFormDTO> hodFinalizeCorrections(
            @PathVariable UUID formId,
            @RequestParam String restartLevel // e.g., "DEPARTMENT_REVIEW", "HOD_REVIEW", "VERIFYING_STAFF_REVIEW"
            // @AuthenticationPrincipal CustomUserDetails currentUser // Ideal
    ) {
        // UUID hodUserId = currentUser.getId(); // Ideal
        UUID hodUserId = UUID.randomUUID(); // Placeholder for this subtask
        
        ReviewLevel reviewLevelEnum;
        try {
            reviewLevelEnum = ReviewLevel.valueOf(restartLevel.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Consider returning a 400 Bad Request with a more user-friendly error
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid restart review level provided: " + restartLevel);
        }

        log.info("API: HOD {} finalizing corrections for form {}, moving to {}", hodUserId, formId, reviewLevelEnum);
        AppraisalFormDTO updatedForm = formService.hodFinalizeCorrections(formId, hodUserId, reviewLevelEnum);
        return ResponseEntity.ok(updatedForm);
    }
}
