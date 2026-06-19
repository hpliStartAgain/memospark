package com.memospark.core.controller;

import com.memospark.core.config.CurrentUser;
import com.memospark.core.config.LoginAttemptService;
import com.memospark.core.config.UserPrincipal;
import com.memospark.core.dto.LoginRequest;
import com.memospark.core.dto.RegisterRequest;
import com.memospark.core.dto.UserDto;
import com.memospark.core.service.PasswordResetService;
import com.memospark.core.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService loginAttemptService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@RequestBody RegisterRequest req) {
        return userService.register(req);
    }

    @PostMapping("/login")
    public UserDto login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (loginAttemptService.isLocked(req.username())) {
            throw new LockedException("Account temporarily locked due to too many failed attempts");
        }
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.username(), req.password()));
            loginAttemptService.loginSucceeded(req.username());
            SecurityContextHolder.getContext().setAuthentication(auth);
            HttpSession session = request.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
            return userService.getUserDto(req.username());
        } catch (BadCredentialsException ex) {
            loginAttemptService.loginFailed(req.username());
            throw ex;
        }
    }

    @GetMapping("/me")
    public org.springframework.http.ResponseEntity<?> me(@CurrentUser UserPrincipal principal) {
        if (principal == null) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Not authenticated"));
        }
        return org.springframework.http.ResponseEntity.ok(userService.getUserDto(principal.username()));
    }

    @PostMapping("/password-reset/request")
    public Map<String, String> requestPasswordReset(@RequestBody PasswordResetRequest req) {
        String token = passwordResetService.requestReset(req.username());
        if (token == null) {
            // Don't reveal whether user exists — return generic success
            return Map.of("message", "If the user exists, a reset token has been generated.");
        }
        // For self-hosted without email: return the token directly
        return Map.of("message", "Reset token generated.", "token", token);
    }

    @PostMapping("/password-reset/confirm")
    public Map<String, String> confirmPasswordReset(@RequestBody PasswordResetConfirm req) {
        passwordResetService.resetPassword(req.token(), req.newPassword());
        return Map.of("message", "Password reset successful. Please login with your new password.");
    }

    public record PasswordResetRequest(@NotBlank String username) {}

    public record PasswordResetConfirm(
            @NotBlank String token,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {}
}
