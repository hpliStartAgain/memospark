package com.memospark.core.dto;

public record UpdateSkillRequest(
        String name,
        String category,
        String description,
        Integer weight,
        Integer selfLevel
) {}
