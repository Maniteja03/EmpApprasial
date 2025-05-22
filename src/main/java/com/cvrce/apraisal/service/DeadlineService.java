package com.cvrce.apraisal.service;

import com.cvrce.apraisal.dto.DeadlineConfigDTO;

public interface DeadlineService {
    void setDeadline(DeadlineConfigDTO dto);
    boolean isSubmissionOpen(String academicYear);
    DeadlineConfigDTO getDeadline(String academicYear);
}
