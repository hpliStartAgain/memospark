package com.memospark.core.dto;

public record AiSettingsDto(
        String provider,
        String baseUrl,
        String model,
        boolean apiKeyConfigured,
        String apiKeyMasked
) {}
