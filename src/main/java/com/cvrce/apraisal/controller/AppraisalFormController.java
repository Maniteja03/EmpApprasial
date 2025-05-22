package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.appraisal.AppraisalFormDTO;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.security.UserPrincipal;
import com.cvrce.apraisal.service.AppraisalFormService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
}
