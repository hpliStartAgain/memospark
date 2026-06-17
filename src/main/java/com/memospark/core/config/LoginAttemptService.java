package com.memospark.core.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for login attempts.
 * Locks an account for {@code lockoutMinutes} after {@code maxAttempts} failures.
 * Uses ConcurrentHashMap — suitable for single-instance deployments; for
 * multi-instance, replace with Redis-backed counter.
 */
@Service
@Slf4j
public class LoginAttemptService {

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lockout-minutes:15}")
    private long lockoutMinutes;

    private record AttemptRecord(int count, Instant lockedUntil) {}

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public void loginSucceeded(String username) {
        attempts.remove(username.toLowerCase());
    }

    public void loginFailed(String username) {
        String key = username.toLowerCase();
        AttemptRecord current = attempts.getOrDefault(key, new AttemptRecord(0, null));
        int newCount = current.count() + 1;
        Instant lockedUntil = newCount >= maxAttempts
                ? Instant.now().plusSeconds(lockoutMinutes * 60)
                : null;
        attempts.put(key, new AttemptRecord(newCount, lockedUntil));
        if (lockedUntil != null) {
            log.warn("Account '{}' locked out after {} failed attempts for {} min",
                    username, maxAttempts, lockoutMinutes);
        }
    }

    public boolean isLocked(String username) {
        AttemptRecord record = attempts.get(username.toLowerCase());
        if (record == null || record.lockedUntil() == null) return false;
        if (Instant.now().isAfter(record.lockedUntil())) {
            attempts.remove(username.toLowerCase());
            return false;
        }
        return true;
    }
}
