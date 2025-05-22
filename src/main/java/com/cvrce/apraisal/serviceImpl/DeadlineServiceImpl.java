package com.cvrce.apraisal.serviceImpl;

import com.cvrce.apraisal.dto.DeadlineConfigDTO;
import com.cvrce.apraisal.entity.DeadlineConfig;
import com.cvrce.apraisal.exception.ResourceNotFoundException;
import com.cvrce.apraisal.repo.DeadlineConfigRepository;
import com.cvrce.apraisal.service.DeadlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineServiceImpl implements DeadlineService {

    private final DeadlineConfigRepository deadlineRepo;

    @Override
    public void setDeadline(DeadlineConfigDTO dto) {
        DeadlineConfig config = deadlineRepo.findByAcademicYear(dto.getAcademicYear())
                .map(existing -> {
                    existing.setDeadlineDate(dto.getDeadlineDate());
                    return existing;
                }).orElseGet(() -> DeadlineConfig.builder()
                        .academicYear(dto.getAcademicYear())
                        .deadlineDate(dto.getDeadlineDate())
                        .build());

        deadlineRepo.save(config);
        log.info("Set deadline for {} as {}", dto.getAcademicYear(), dto.getDeadlineDate());
    }

    @Override
    public boolean isSubmissionOpen(String academicYear) {
        DeadlineConfig config = deadlineRepo.findByAcademicYear(academicYear)
                .orElseThrow(() -> new ResourceNotFoundException("No deadline set for year"));
        return LocalDate.now().isBefore(config.getDeadlineDate());
    }

    @Override
    public DeadlineConfigDTO getDeadline(String academicYear) {
        DeadlineConfig config = deadlineRepo.findByAcademicYear(academicYear)
                .orElseThrow(() -> new ResourceNotFoundException("Deadline not found"));
        return new DeadlineConfigDTO(config.getAcademicYear(), config.getDeadlineDate());
    }
}
