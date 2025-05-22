package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.NotificationDTO;
import com.cvrce.apraisal.entity.Notification;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.NotificationRepository;
import com.cvrce.apraisal.repo.UserRepository;
import com.cvrce.apraisal.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;

    @Override
    public List<NotificationDTO> getUserNotifications(UUID userId) {
        return notificationRepo.findByUserIdOrderByTimestampDesc(userId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        notification.setRead(true);
        notificationRepo.save(notification);
        log.info("Marked notification {} as read", notificationId);
    }

    @Override
    public NotificationDTO sendNotification(NotificationDTO dto) {
        User user = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Notification notification = Notification.builder()
                .title(dto.getTitle())
                .message(dto.getMessage())
                .read(false)
                .timestamp(LocalDateTime.now())
                .user(user)
                .build();

        Notification saved = notificationRepo.save(notification);
        log.info("Sent notification to user {} - {}", user.getEmail(), dto.getTitle());
        return mapToDTO(saved);
    }

    private NotificationDTO mapToDTO(Notification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .timestamp(notification.getTimestamp())
                .read(notification.isRead())
                .userId(notification.getUser().getId())
                .build();
    }

}
