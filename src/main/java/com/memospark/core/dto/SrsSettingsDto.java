package com.memospark.core.dto;

public record SrsSettingsDto(
        double initialEaseFactor,
        double minEaseFactor,
        int firstInterval,
        int secondInterval,
        double desiredRetention
) {}
