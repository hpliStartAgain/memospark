package com.memospark.core.dto;

import java.time.LocalDate;

public record CreateTargetRequest(
        String title,
        String company,
        String status,
        LocalDate interviewDate,
        String notes
) {}
