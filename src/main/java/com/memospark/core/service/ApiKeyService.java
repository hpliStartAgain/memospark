package com.memospark.core.service;

import com.memospark.core.domain.User;
import com.memospark.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public String generateApiKey(Long userId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawKey = "msk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hashed = hashKey(rawKey);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setApiKey(hashed);
        userRepository.save(user);
        return rawKey;
    }

    @Transactional
    public void revokeApiKey(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setApiKey(null);
        userRepository.save(user);
    }

    public String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }

    public boolean verifyKey(String rawKey, String hashedKey) {
        if (rawKey == null || hashedKey == null) return false;
        return MessageDigest.isEqual(
                hashKey(rawKey).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                hashedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
