package com.memospark.core.dto;

import java.time.LocalDate;
import java.util.List;

public record TargetDetailDto(
        Long id,
        String title,
        String company,
        String status,
        LocalDate interviewDate,
        Long daysUntilInterview,
        String notes,
        List<JobJdDto> jds,
        List<TargetSkillDto> skills,
        ReadinessDto readiness
) {}
