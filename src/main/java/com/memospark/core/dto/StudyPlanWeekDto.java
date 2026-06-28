package com.memospark.core.dto;

import java.time.LocalDate;
import java.util.List;

public record StudyPlanWeekDto(
        int weekNumber,
        LocalDate startDate,
        LocalDate endDate,
        String objective,
        String stage,
        List<StudyPlanDayDto> days
) {}

