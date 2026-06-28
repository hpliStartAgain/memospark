package com.memospark.core.dto;

import java.time.LocalDate;
import java.util.List;

public record StudyPlanDayDto(
        LocalDate date,
        List<StudyPlanItemDto> items
) {}

