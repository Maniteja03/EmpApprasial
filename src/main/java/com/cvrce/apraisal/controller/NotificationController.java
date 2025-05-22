package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.NotificationDTO;
import com.cvrce.apraisal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getMyNotifications() {
        String userIdStr = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        UUID userId = UUID.fromString(userIdStr);
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @PostMapping("/read/{id}")
    public ResponseEntity<String> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok("Notification marked as read");
    }

    @PostMapping("/send")
    public ResponseEntity<NotificationDTO> sendNotification(@RequestBody NotificationDTO dto) {
        return new ResponseEntity<>(notificationService.sendNotification(dto), HttpStatus.CREATED);
    }
}
