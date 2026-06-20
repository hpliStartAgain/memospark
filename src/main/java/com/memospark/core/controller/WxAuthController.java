package com.memospark.core.controller;

import com.memospark.core.config.JwtService;
import com.memospark.core.domain.User;
import com.memospark.core.domain.UserRole;
import com.memospark.core.dto.UserDto;
import com.memospark.core.service.UserService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Stateless (JWT) authentication endpoints for WeChat Mini-Program and other API clients.
 * Registers under /api/auth alongside the session-based AuthController.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class WxAuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RestTemplate restTemplate;

    @Value("${wx.app-id:}")
    private String wxAppId;

    @Value("${wx.app-secret:}")
    private String wxAppSecret;

    /**
     * WeChat Mini-Program login.
     * Client calls wx.login() → gets a temporary `code` → sends here.
     * Backend exchanges code for openid via WeChat API, finds or creates the user,
     * and returns a JWT.
     */
    @PostMapping("/wx-login")
    public ResponseEntity<?> wxLogin(@RequestBody WxLoginRequest req) {
        if (wxAppId == null || wxAppId.isBlank() || wxAppSecret == null || wxAppSecret.isBlank()) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "WeChat login is not configured on this server"));
        }

        String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wxAppId, wxAppSecret, req.code());

        Map<String, Object> wxResp;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
            wxResp = resp;
        } catch (RestClientException ex) {
            return ResponseEntity.status(502)
                    .body(Map.of("error", "Failed to reach WeChat: " + ex.getMessage()));
        }

        if (wxResp == null || wxResp.containsKey("errcode")) {
            Object code = wxResp != null ? wxResp.get("errcode") : "null";
            Object msg  = wxResp != null ? wxResp.get("errmsg")  : "no response";
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "WeChat auth failed: " + code + " — " + msg));
        }

        String openid = (String) wxResp.get("openid");
        User user = userService.findOrCreateByWxOpenid(openid);
        boolean isAdmin = user.getRole() == UserRole.ADMIN;
        String token = jwtService.generateToken(user.getId(), user.getUsername(), isAdmin);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", new UserDto(user.getId(), user.getUsername(), user.getRole())
        ));
    }

    /**
     * Stateless token login: username + password → JWT.
     * For mini-program clients that cannot use session cookies.
     */
    @PostMapping("/token")
    public ResponseEntity<?> tokenLogin(@RequestBody TokenLoginRequest req) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            String username = auth.getName();
            User user = userService.getUserByUsername(username);
            boolean isAdmin = user.getRole() == UserRole.ADMIN;
            String token = jwtService.generateToken(user.getId(), user.getUsername(), isAdmin);
            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "user", new UserDto(user.getId(), user.getUsername(), user.getRole())
            ));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
    }

    public record WxLoginRequest(@NotBlank String code) {}
    public record TokenLoginRequest(
            @NotBlank @Size(max = 50) String username,
            @NotBlank String password) {}
}
