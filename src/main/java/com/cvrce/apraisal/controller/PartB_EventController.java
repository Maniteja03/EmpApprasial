package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.partb.EventDTO;
import com.cvrce.apraisal.service.PartB_EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/partb/events")
@RequiredArgsConstructor
@Slf4j
public class PartB_EventController {

    private final PartB_EventService eventService;

    @PostMapping
    public ResponseEntity<EventDTO> add(@RequestBody EventDTO dto) {
        log.info("Adding event to form {}", dto.getAppraisalFormId());
        return new ResponseEntity<>(eventService.addEvent(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{formId}")
    public ResponseEntity<List<EventDTO>> getByForm(@PathVariable UUID formId) {
        log.info("Fetching events for form {}", formId);
        return ResponseEntity.ok(eventService.getEventsByFormId(formId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> update(@PathVariable UUID id, @RequestBody EventDTO dto) {
        log.info("Updating event {}", id);
        return ResponseEntity.ok(eventService.updateEvent(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
