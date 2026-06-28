package com.memospark.core.service;

import com.memospark.core.domain.AiSettings;
import com.memospark.core.domain.User;
import com.memospark.core.dto.AiSettingsDto;
import com.memospark.core.dto.UpdateAiSettingsRequest;
import com.memospark.core.repository.AiSettingsRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class AiSettingsService {

    public static final String DEFAULT_PROVIDER = "SenseNova";
    public static final String DEFAULT_BASE_URL = "https://token.sensenova.cn/v1";
    public static final String DEFAULT_MODEL = "deepseek-v4-flash";

    private final AiSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final AiSecretCipher secretCipher;

    @Value("${ai.api.key:}")
    private String defaultApiKey;

    @Value("${ai.api.url:" + DEFAULT_BASE_URL + "}")
    private String defaultBaseUrl;

    @Value("${ai.api.model:" + DEFAULT_MODEL + "}")
    private String defaultModel;

    @Transactional(readOnly = true)
    public AiSettingsDto get(Long userId) {
        return settingsRepository.findByUserId(userId)
                .map(this::toDto)
                .orElseGet(() -> defaultDto(defaultApiKey != null && !defaultApiKey.isBlank()));
    }

    @Transactional
    public AiSettingsDto update(Long userId, UpdateAiSettingsRequest req) {
        if (req == null) throw new IllegalArgumentException("AI settings are required");
        AiSettings settings = settingsRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            return new AiSettings(user, DEFAULT_PROVIDER, normalizeBaseUrl(defaultBaseUrl), defaultModel);
        });

        settings.setProvider(cleanOrDefault(req.provider(), DEFAULT_PROVIDER));
        settings.setBaseUrl(normalizeBaseUrl(cleanOrDefault(req.baseUrl(), defaultBaseUrl)));
        settings.setModel(cleanOrDefault(req.model(), defaultModel));

        if (Boolean.TRUE.equals(req.clearApiKey())) {
            settings.setApiKeyEncrypted(null);
        } else if (req.apiKey() != null && !req.apiKey().isBlank()) {
            settings.setApiKeyEncrypted(secretCipher.encrypt(req.apiKey().trim()));
        }

        return toDto(settingsRepository.save(settings));
    }

    @Transactional(readOnly = true)
    public AiRuntimeSettings resolve(Long userId) {
        if (userId == null) {
            return defaults();
        }
        return settingsRepository.findByUserId(userId)
                .map(s -> new AiRuntimeSettings(
                        s.getProvider(),
                        normalizeBaseUrl(s.getBaseUrl()),
                        s.getModel(),
                        resolveApiKey(s)))
                .orElseGet(this::defaults);
    }

    private AiRuntimeSettings defaults() {
        return new AiRuntimeSettings(
                DEFAULT_PROVIDER,
                normalizeBaseUrl(defaultBaseUrl),
                defaultModel,
                clean(defaultApiKey));
    }

    private String resolveApiKey(AiSettings settings) {
        if (settings.getApiKeyEncrypted() != null && !settings.getApiKeyEncrypted().isBlank()) {
            return secretCipher.decrypt(settings.getApiKeyEncrypted());
        }
        return clean(defaultApiKey);
    }

    private AiSettingsDto toDto(AiSettings settings) {
        String key = resolveApiKey(settings);
        boolean configured = key != null && !key.isBlank();
        return new AiSettingsDto(
                settings.getProvider(),
                normalizeBaseUrl(settings.getBaseUrl()),
                settings.getModel(),
                configured,
                configured ? mask(key) : null);
    }

    private AiSettingsDto defaultDto(boolean configured) {
        return new AiSettingsDto(
                DEFAULT_PROVIDER,
                normalizeBaseUrl(defaultBaseUrl),
                defaultModel,
                configured,
                configured ? mask(defaultApiKey) : null);
    }

    private String normalizeBaseUrl(String value) {
        String url = cleanOrDefault(value, DEFAULT_BASE_URL);
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid AI base URL");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("AI base URL must use http or https");
        }
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private String mask(String key) {
        if (key == null || key.length() < 8) return "********";
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }

    private String cleanOrDefault(String value, String fallback) {
        String cleaned = clean(value);
        return cleaned != null ? cleaned : fallback;
    }

    private String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record AiRuntimeSettings(String provider, String baseUrl, String model, String apiKey) {}
}
