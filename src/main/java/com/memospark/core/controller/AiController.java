package com.memospark.core.controller;

import com.memospark.core.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    public record HintRequest(String problemDescription, String userCode, int level) {}
    public record GenerateCardsRequest(String topic, int count, String language) {}
    public record GradeRequest(String question, String referenceAnswer, String userAnswer) {}
    public record AnalyzeTLERequest(String problemDescription, String userCode, String language) {}
    public record AnalyzeJdsRequest(List<String> jds, String language) {}
    public record GenerateJdCardsRequest(String deckName, String topic, int count, String language) {}

    @PostMapping("/hint")
    public Map<String, String> getHint(@RequestBody HintRequest req,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        String hint = aiService.generateHint(req.problemDescription(), req.userCode(), req.level());
        return Map.of("hint", hint);
    }

    @PostMapping("/grade")
    public Map<String, Object> gradeAnswer(@RequestBody GradeRequest req,
                                            @AuthenticationPrincipal UserDetails userDetails) {
        return aiService.gradeAnswer(req.question(), req.referenceAnswer(), req.userAnswer());
    }

    @PostMapping("/analyze-tle")
    public Map<String, String> analyzeTLE(@RequestBody AnalyzeTLERequest req,
                                           @AuthenticationPrincipal UserDetails userDetails) {
        String analysis = aiService.analyzeTLE(req.problemDescription(), req.userCode(), req.language());
        return Map.of("analysis", analysis);
    }

    @PostMapping("/generate-cards")
    public List<Map<String, String>> generateCards(@RequestBody GenerateCardsRequest req,
                                                    @AuthenticationPrincipal UserDetails userDetails) {
        int count = Math.min(req.count() > 0 ? req.count() : 10, 30);
        return aiService.generateCards(req.topic(), count, req.language());
    }

    @PostMapping("/jd/analyze")
    public Map<String, Object> analyzeJds(@RequestBody AnalyzeJdsRequest req) {
        if (req.jds() == null || req.jds().isEmpty()) {
            throw new IllegalArgumentException("At least one JD is required");
        }
        // Cap to avoid oversized prompts
        List<String> jds = req.jds().stream().limit(20).toList();
        return aiService.analyzeJds(jds, req.language());
    }

    @PostMapping("/jd/generate-cards")
    public List<Map<String, String>> generateJdCards(@RequestBody GenerateJdCardsRequest req) {
        if (req.topic() == null || req.topic().isBlank()) {
            throw new IllegalArgumentException("topic is required");
        }
        int count = Math.min(req.count() > 0 ? req.count() : 10, 30);
        return aiService.generateCardsForTopic(req.deckName(), req.topic(), count, req.language());
    }
}
