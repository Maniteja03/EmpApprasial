package com.cvrce.apraisal.scheduler;

import com.cvrce.apraisal.entity.AppraisalForm;
import com.cvrce.apraisal.entity.DeadlineConfig;
import com.cvrce.apraisal.entity.Role;
import com.cvrce.apraisal.entity.User;
import com.cvrce.apraisal.enums.AppraisalStatus;
import com.cvrce.apraisal.repo.AppraisalFormRepository;
import com.cvrce.apraisal.repo.DeadlineConfigRepository;
import com.cvrce.apraisal.repo.RoleRepository;
import com.cvrce.apraisal.repo.UserRepository;
// import com.cvrce.apraisal.service.DeadlineService; // Not used directly as per refined logic
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime; // For logging current time
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadlineNotificationScheduler {

    private final DeadlineConfigRepository deadlineConfigRepository;
    private final UserRepository userRepository;
    private final AppraisalFormRepository appraisalFormRepository;
    private final JavaMailSender mailSender;
    private final RoleRepository roleRepository;
    // private final DeadlineService deadlineService; // Not directly used as per refined logic

    private static final List<Long> REMINDER_DAYS_BEFORE = Arrays.asList(7L, 3L, 1L, 0L); // 0L for on the day

    @Scheduled(cron = "0 0 9 * * ?") // Daily at 9 AM
    public void sendDeadlineReminders() {
        log.info("Executing scheduled task: sendDeadlineReminders - Current time: {}", LocalDateTime.now());

        List<DeadlineConfig> deadlineConfigs = deadlineConfigRepository.findAll();

        if (deadlineConfigs.isEmpty()) {
            log.info("No deadline configurations found. Skipping reminders.");
            return;
        }

        LocalDate today = LocalDate.now();

        for (DeadlineConfig config : deadlineConfigs) {
            LocalDate deadlineDate = config.getDeadlineDate();
            String academicYear = config.getAcademicYear();

            if (deadlineDate.isBefore(today)) { // Deadline has passed
                log.info("Deadline for academic year {} ({}) has already passed. Skipping.", academicYear, deadlineDate);
                continue;
            }

            for (Long daysBefore : REMINDER_DAYS_BEFORE) {
                if (today.equals(deadlineDate.minusDays(daysBefore))) {
                    log.info("Today is a reminder day ({} days before) for academic year {} deadline ({}).", daysBefore, academicYear, deadlineDate);
                    
                    Role staffRole = roleRepository.findByName("STAFF")
                            .orElse(null); 
                    if (staffRole == null) {
                        log.error("STAFF role not found. Cannot send deadline reminders.");
                        continue; 
                    }

                    List<User> staffUsers = userRepository.findByRolesContains(staffRole);

                    for (User staff : staffUsers) {
                        Optional<AppraisalForm> formOptional = appraisalFormRepository.findByUserIdAndAcademicYear(staff.getId(), academicYear);
                       
                        boolean submitted = false;
                        if (formOptional.isPresent()) {
                            AppraisalStatus currentStatus = formOptional.get().getStatus();
                            if (currentStatus != AppraisalStatus.DRAFT) { // DRAFT is considered not submitted for deadline
                                submitted = true;
                            }
                        }

                        if (!submitted) {
                            log.info("Sending deadline reminder to {} for academic year {}.", staff.getEmail(), academicYear);
                            SimpleMailMessage message = new SimpleMailMessage();
                            message.setTo(staff.getEmail());
                            message.setSubject("Appraisal Submission Deadline Reminder: " + academicYear);
                           
                            String emailMessage = "Dear " + (staff.getFullName() != null ? staff.getFullName() : "Staff Member") + ",\n\n" +
                                                 "This is a reminder that the deadline to submit your staff appraisal for the academic year " + academicYear + 
                                                 " is " + deadlineDate.toString() + ".\n\n";
                            if (daysBefore == 0) {
                                emailMessage += "Today is the last day to submit your appraisal.\n\n";
                            } else {
                                emailMessage += "You have " + daysBefore + " day(s) left to submit.\n\n";
                            }
                            emailMessage += "Please ensure you complete your submission in time.\n\nThank you.";
                           
                            message.setText(emailMessage);
                            try {
                                mailSender.send(message);
                                log.info("Reminder email sent to {}.", staff.getEmail());
                            } catch (Exception e) {
                                log.error("Failed to send reminder email to {}: {}", staff.getEmail(), e.getMessage(), e);
                            }
                        } else {
                            log.info("Staff {} has already submitted for {}. No reminder needed.", staff.getEmail(), academicYear);
                        }
                    }
                    break; 
                }
            }
        }
        log.info("Finished scheduled task: sendDeadlineReminders.");
    }
}
