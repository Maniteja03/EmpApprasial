package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.NotificationDTO;

import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDTO> getUserNotifications(UUID userId);
    void markAsRead(UUID notificationId);
    NotificationDTO sendNotification(NotificationDTO dto); // For internal triggers
}
