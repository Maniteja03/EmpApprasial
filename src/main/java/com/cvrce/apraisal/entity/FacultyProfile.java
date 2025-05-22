package com.cvrce.apraisal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "faculty_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FacultyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int experienceAtCVR;

    private int industryExperience;

    private int teachingExperienceOutsideCVR;

    private String contactNumber;
}
