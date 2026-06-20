package com.memospark.core.dto;

import java.time.LocalDateTime;

public record JobJdDto(
        Long id,
        String title,
        String content,
        String source,
        LocalDateTime createdAt
) {}
