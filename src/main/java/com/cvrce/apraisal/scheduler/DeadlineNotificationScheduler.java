package com.cvrce.apraisal.scheduler;

import com.cvrce.apraisal.entity.*;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.repo.*;
import com.cvrce.apraisal.dto.NotificationDTO;
import com.cvrce.apraisal.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeadlineNotificationScheduler {

    private final DeadlineConfigRepository deadlineRepo;
    private final AppraisalFormRepository appraisalFormRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 0 * * *") // Runs at 12:00 AM daily
    public void checkAndNotifyDeadlineExpiries() {
        LocalDate today = LocalDate.now();
        List<DeadlineConfig> dueDeadlines = deadlineRepo.findAll()
                .stream()
                .filter(config -> config.getDeadlineDate().isEqual(today))
                .collect(Collectors.toList());

        for (DeadlineConfig config : dueDeadlines) {
            String academicYear = config.getAcademicYear();
            log.info("Deadline matched for academic year: {}", academicYear);

            List<UUID> submittedUserIds = appraisalFormRepo.findAll()
                    .stream()
                    .filter(form -> academicYear.equals(form.getAcademicYear()))
                    .filter(form -> form.getStatus() == AppraisalStatus.SUBMITTED)
                    .map(form -> form.getUser().getId())
                    .collect(Collectors.toList());

            List<User> allUsers = userRepo.findAll();
            List<User> pendingUsers = allUsers.stream()
                    .filter(user -> !submittedUserIds.contains(user.getId()))
                    .collect(Collectors.toList());

            for (User user : pendingUsers) {
                NotificationDTO dto = NotificationDTO.builder()
                        .userId(user.getId())
                        .title("⏰ Submission Deadline Reminder")
                        .message("Today is the last day to submit your appraisal for " + academicYear)
                        .build();
                notificationService.sendNotification(dto);
            }

            log.info("Sent deadline reminders to {} users", pendingUsers.size());
        }
    }
}
