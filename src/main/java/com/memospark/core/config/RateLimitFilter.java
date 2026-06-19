package com.memospark.core.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory sliding-window rate limiter for AI and Judge endpoints.
 * Keyed by user principal name (or IP for anonymous).
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${rate.limit.ai.per-min:10}")
    private int aiLimitPerMin;

    @Value("${rate.limit.judge.per-min:5}")
    private int judgeLimitPerMin;

    private final Map<String, RateBucket> aiBuckets = new ConcurrentHashMap<>();
    private final Map<String, RateBucket> judgeBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        boolean isAi = path.startsWith("/api/ai/");
        boolean isJudge = path.matches("^/api/practice/problems/\\d+/submit$");

        if (!isAi && !isJudge) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);
        int limit = isAi ? aiLimitPerMin : judgeLimitPerMin;
        Map<String, RateBucket> buckets = isAi ? aiBuckets : judgeBuckets;

        RateBucket bucket = buckets.computeIfAbsent(key, k -> new RateBucket(limit));
        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for key={} path={}", key, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveKey(HttpServletRequest request) {
        var principal = request.getUserPrincipal();
        if (principal != null && principal.getName() != null) {
            return principal.getName();
        }
        return request.getRemoteAddr();
    }

    /**
     * Simple fixed-window rate bucket: resets every 60 seconds.
     */
    private static class RateBucket {
        private final int maxRequests;
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        RateBucket(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60_000) {
                synchronized (this) {
                    if (now - windowStart > 60_000) {
                        windowStart = now;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet() <= maxRequests;
        }
    }
}
