package com.memospark.core.service;

import com.memospark.core.domain.PasswordResetToken;
import com.memospark.core.domain.User;
import com.memospark.core.repository.PasswordResetTokenRepository;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${password.reset.token-expiry-minutes:30}")
    private int tokenExpiryMinutes;

    /**
     * Generate a password reset token for the given username.
     * Returns the raw token (only shown once) or null if user not found.
     */
    @Transactional
    public String requestReset(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            log.warn("Password reset requested for unknown user: {}", username);
            return null;
        }

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = "rst_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        PasswordResetToken prt = new PasswordResetToken(
                user, token, LocalDateTime.now().plusMinutes(tokenExpiryMinutes));
        tokenRepository.save(prt);

        log.info("Password reset token generated for user: {}", username);
        return token;
    }

    /**
     * Reset password using a valid token.
     * @throws IllegalArgumentException if token is invalid, expired, or already used.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        if (prt.isUsed()) {
            throw new IllegalArgumentException("Reset token already used");
        }
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reset token expired");
        }

        User user = prt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);

        log.info("Password reset completed for user: {}", user.getUsername());
    }
}
