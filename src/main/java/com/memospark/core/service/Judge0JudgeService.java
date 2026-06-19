package com.memospark.core.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Judge0 cloud-based code execution backend.
 * Active when judge.type=judge0 (default).
 */
@Service
@ConditionalOnProperty(name = "judge.type", havingValue = "judge0", matchIfMissing = true)
public class Judge0JudgeService implements JudgeBackend {

    private static final int JAVA_LANGUAGE_ID = 62;
    private static final int PYTHON_LANGUAGE_ID = 71;

    private final RestClient restClient;

    public Judge0JudgeService(
            @Value("${judge0.api.url}") String apiUrl,
            @Value("${judge0.api.key:}") String apiKey,
            @Value("${judge0.api.host:}") String apiHost
    ) {
        RestClient.Builder builder = RestClient.builder().baseUrl(apiUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("X-RapidAPI-Key", apiKey);
            builder.defaultHeader("X-RapidAPI-Host", apiHost);
        }
        this.restClient = builder.build();
    }

    @Override
    public JudgeResult execute(String sourceCode, String language, String stdin) {
        int languageId = "java".equals(language) ? JAVA_LANGUAGE_ID : PYTHON_LANGUAGE_ID;

        String encodedSource = Base64.getEncoder()
                .encodeToString(sourceCode.getBytes(StandardCharsets.UTF_8));
        String encodedStdin = Base64.getEncoder()
                .encodeToString(stdin.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> body = Map.of(
                "source_code", encodedSource,
                "language_id", languageId,
                "stdin", encodedStdin,
                "cpu_time_limit", 5.0,
                "memory_limit", 256000
        );

        Map<String, Object> response = restClient.post()
                .uri("/submissions?base64_encoded=true&wait=true")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null) {
            return new JudgeResult(13, "", "No response from judge", "");
        }

        String stdout = decodeBase64(response.get("stdout"));
        String stderr = decodeBase64(response.get("stderr"));
        String compileOutput = decodeBase64(response.get("compile_output"));

        @SuppressWarnings("unchecked")
        Map<String, Object> statusMap = (Map<String, Object>) response.get("status");
        int statusId = statusMap != null ? ((Number) statusMap.get("id")).intValue() : 13;

        return new JudgeResult(statusId, stdout, stderr, compileOutput);
    }

    private String decodeBase64(Object val) {
        if (val == null) return "";
        try {
            return new String(Base64.getDecoder().decode(val.toString()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return val.toString();
        }
    }
}
