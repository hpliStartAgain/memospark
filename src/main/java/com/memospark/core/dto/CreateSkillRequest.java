package com.memospark.core.dto;

public record CreateSkillRequest(
        String name,
        String category,
        String description,
        Integer weight
) {}
