package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.AwardDTO;
import com.cvrce.apraisal.service.PartB_AwardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partb/awards")
@RequiredArgsConstructor
@Slf4j
public class PartB_AwardController {

    private final PartB_AwardService awardService;

    @PostMapping
    public ResponseEntity<AwardDTO> add(@RequestBody AwardDTO dto) {
        log.info("Adding award to form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(awardService.addAward(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<AwardDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching awards for form {}", formId);
        return ResponseEntity.ok(awardService.getAwardsByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AwardDTO> update(@PathVariable UUID id, @RequestBody AwardDTO dto) {
        log.info("Updating award {}", id);
        return ResponseEntity.ok(awardService.updateAward(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        awardService.deleteAward(id);
        return ResponseEntity.noContent().build();
    }
}
