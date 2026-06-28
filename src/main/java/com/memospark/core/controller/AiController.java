package com.memospark.core.controller;

import com.memospark.core.service.AiService;
import com.memospark.core.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiController {

    private final AiService aiService;
    private final UserService userService;

    public record HintRequest(String problemDescription, String userCode, int level) {}
    public record GenerateCardsRequest(String topic, int count, String language) {}
    public record GradeRequest(String question, String referenceAnswer, String userAnswer) {}
    public record AnalyzeTLERequest(String problemDescription, String userCode, String language) {}
    public record AnalyzeJdsRequest(List<String> jds, String language) {}
    public record GenerateJdCardsRequest(String deckName, String topic, int count, String language) {}

    @PostMapping("/hint")
    public Map<String, String> getHint(@RequestBody HintRequest req) {
        Long userId = currentUserId();
        String hint = userId != null
                ? aiService.generateHint(req.problemDescription(), req.userCode(), req.level(), userId)
                : aiService.generateHint(req.problemDescription(), req.userCode(), req.level());
        return Map.of("hint", hint);
    }

    @PostMapping(value = "/hint/stream", produces = "text/event-stream")
    public SseEmitter getHintStream(@RequestBody HintRequest req) {
        SseEmitter emitter = new SseEmitter(60_000L);
        Long userId = currentUserId();
        Thread.startVirtualThread(() -> {
            try {
                java.util.function.Consumer<String> onChunk = chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("chunk").data(chunk));
                            } catch (Exception e) {
                                log.debug("SSE send failed", e);
                            }
                        };
                String full = userId != null
                        ? aiService.generateHintStream(
                                req.problemDescription(), req.userCode(), req.level(), userId, onChunk)
                        : aiService.generateHintStream(
                                req.problemDescription(), req.userCode(), req.level(), onChunk);
                emitter.send(SseEmitter.event().name("done").data(full));
                emitter.complete();
            } catch (Exception e) {
                log.error("Streaming hint failed", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @PostMapping("/grade")
    public Map<String, Object> gradeAnswer(@RequestBody GradeRequest req) {
        Long userId = currentUserId();
        return userId != null
                ? aiService.gradeAnswer(req.question(), req.referenceAnswer(), req.userAnswer(), userId)
                : aiService.gradeAnswer(req.question(), req.referenceAnswer(), req.userAnswer());
    }

    @PostMapping("/analyze-tle")
    public Map<String, String> analyzeTLE(@RequestBody AnalyzeTLERequest req) {
        Long userId = currentUserId();
        String analysis = userId != null
                ? aiService.analyzeTLE(req.problemDescription(), req.userCode(), req.language(), userId)
                : aiService.analyzeTLE(req.problemDescription(), req.userCode(), req.language());
        return Map.of("analysis", analysis);
    }

    @PostMapping("/generate-cards")
    public List<Map<String, String>> generateCards(@RequestBody GenerateCardsRequest req) {
        int count = Math.min(req.count() > 0 ? req.count() : 10, 30);
        Long userId = currentUserId();
        return userId != null
                ? aiService.generateCards(req.topic(), count, req.language(), userId)
                : aiService.generateCards(req.topic(), count, req.language());
    }

    @PostMapping("/jd/analyze")
    public Map<String, Object> analyzeJds(@RequestBody AnalyzeJdsRequest req) {
        if (req.jds() == null || req.jds().isEmpty()) {
            throw new IllegalArgumentException("At least one JD is required");
        }
        // Cap to avoid oversized prompts
        List<String> jds = req.jds().stream().limit(20).toList();
        Long userId = currentUserId();
        return userId != null
                ? aiService.analyzeJds(jds, req.language(), userId)
                : aiService.analyzeJds(jds, req.language());
    }

    @PostMapping("/jd/generate-cards")
    public List<Map<String, String>> generateJdCards(@RequestBody GenerateJdCardsRequest req) {
        if (req.topic() == null || req.topic().isBlank()) {
            throw new IllegalArgumentException("topic is required");
        }
        int count = Math.min(req.count() > 0 ? req.count() : 10, 30);
        Long userId = currentUserId();
        return userId != null
                ? aiService.generateCardsForTopic(req.deckName(), req.topic(), count, req.language(), userId)
                : aiService.generateCardsForTopic(req.deckName(), req.topic(), count, req.language());
    }

    private Long currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userService.getUserId(authentication.getName());
    }
}
