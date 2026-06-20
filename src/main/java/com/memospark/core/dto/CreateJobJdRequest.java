package com.memospark.core.dto;

public record CreateJobJdRequest(
        String title,
        String content,
        String source
) {}
