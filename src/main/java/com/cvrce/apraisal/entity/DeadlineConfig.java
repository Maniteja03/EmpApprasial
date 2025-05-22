package com.cvrce.apraisal.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "deadline_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadlineConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String academicYear;

    @Column(nullable = false)
    private LocalDate deadlineDate;
}
