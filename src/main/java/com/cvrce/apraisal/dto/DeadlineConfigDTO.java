package com.cvrce.apraisal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeadlineConfigDTO {
    private String academicYear;
    private LocalDate deadlineDate;
}
