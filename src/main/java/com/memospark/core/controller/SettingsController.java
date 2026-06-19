package com.memospark.core.controller;

import com.memospark.core.dto.SrsSettingsDto;
import com.memospark.core.service.ApiKeyService;
import com.memospark.core.service.SpacedRepetitionService;
import com.memospark.core.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SpacedRepetitionService srsService;
    private final UserService userService;
    private final ApiKeyService apiKeyService;

    @GetMapping("/srs")
    public SrsSettingsDto getSrsSettings(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserId(userDetails.getUsername());
        return srsService.getSrsSettings(userId);
    }

    @PutMapping("/srs")
    public SrsSettingsDto updateSrsSettings(@RequestBody SrsSettingsDto dto,
                                             @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserId(userDetails.getUsername());
        return srsService.updateSrsSettings(userId, dto);
    }

    @PostMapping("/api-key")
    public java.util.Map<String, String> generateApiKey(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserId(userDetails.getUsername());
        String rawKey = apiKeyService.generateApiKey(userId);
        return java.util.Map.of("apiKey", rawKey);
    }

    @DeleteMapping("/api-key")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void revokeApiKey(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getUserId(userDetails.getUsername());
        apiKeyService.revokeApiKey(userId);
    }
}
