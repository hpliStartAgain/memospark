package com.memospark.core.dto;

import java.time.LocalDate;

public record GenerateStudyPlanRequest(
        Integer weeklyHours,
        LocalDate targetDate,
        String language
) {}

