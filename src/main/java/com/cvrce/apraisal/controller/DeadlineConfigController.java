package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.DeadlineConfigDTO;
import com.cvrce.apraisal.service.DeadlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deadline-config")
@RequiredArgsConstructor
@Slf4j
public class DeadlineConfigController {

    private final DeadlineService deadlineService;

    // ✅ Admin sets deadline
    @PostMapping
    public ResponseEntity<String> setDeadline(@RequestBody DeadlineConfigDTO dto) {
        log.info("Setting deadline for academic year {}", dto.getAcademicYear());
        deadlineService.setDeadline(dto);
        return ResponseEntity.ok("Deadline configured successfully");
    }

    // ✅ View deadline by year
    @GetMapping("/{academicYear}")
    public ResponseEntity<DeadlineConfigDTO> getDeadline(@PathVariable String academicYear) {
        return ResponseEntity.ok(deadlineService.getDeadline(academicYear));
    }
}
