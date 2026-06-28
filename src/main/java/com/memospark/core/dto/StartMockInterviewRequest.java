package com.memospark.core.dto;

public record StartMockInterviewRequest(
        String type,
        Integer count,
        String language
) {}
