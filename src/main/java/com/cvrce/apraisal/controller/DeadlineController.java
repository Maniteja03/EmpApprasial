package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.DeadlineConfigDTO;
import com.cvrce.apraisal.service.DeadlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deadline")
@RequiredArgsConstructor
@Slf4j
public class DeadlineController {

    private final DeadlineService deadlineService;

    // ✅ Set/Update deadline
    @PostMapping
    public ResponseEntity<String> setDeadline(@RequestBody DeadlineConfigDTO dto) {
        deadlineService.setDeadline(dto);
        return ResponseEntity.ok("Deadline set/updated");
    }

    // ✅ Check if current date < deadline
    @GetMapping("/is-open/{year}")
    public ResponseEntity<Boolean> isSubmissionOpen(@PathVariable String year) {
        return ResponseEntity.ok(deadlineService.isSubmissionOpen(year));
    }

    // ✅ Get deadline date
    @GetMapping("/{year}")
    public ResponseEntity<DeadlineConfigDTO> getDeadline(@PathVariable String year) {
        return ResponseEntity.ok(deadlineService.getDeadline(year));
    }
}
