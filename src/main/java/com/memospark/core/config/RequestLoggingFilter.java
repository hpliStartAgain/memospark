package com.memospark.core.config;

import com.memospark.core.domain.User;
import com.memospark.core.repository.UserRepository;
import com.memospark.core.service.UserActivityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logs every API request (method, path, status, duration) and records
 * daily user activity for DAU metrics.
 *
 * Dependencies are injected via {@link ObjectProvider} so the filter can be
 * instantiated in web-slice tests (@WebMvcTest) where service/repository
 * beans are not on the classpath — activity recording is simply skipped.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private final ObjectProvider<UserActivityService> userActivityServiceProvider;
    private final ObjectProvider<UserRepository> userRepositoryProvider;

    // Lightweight username→id cache to avoid a DB hit on every request.
    private final ConcurrentHashMap<String, Long> userIdCache = new ConcurrentHashMap<>();

    public RequestLoggingFilter(ObjectProvider<UserActivityService> userActivityServiceProvider,
                                ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userActivityServiceProvider = userActivityServiceProvider;
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            String method = request.getMethod();
            String query = request.getQueryString();
            String full = query != null ? path + "?" + query : path;

            if (status >= 400) {
                log.warn("{} {} -> {} ({}ms)", method, full, status, duration);
            } else {
                log.info("{} {} -> {} ({}ms)", method, full, status, duration);
            }

            if (status < 400) {
                recordDailyActivity();
            }
        }
    }

    private void recordDailyActivity() {
        UserActivityService userActivityService = userActivityServiceProvider.getIfAvailable();
        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
        if (userActivityService == null || userRepository == null) {
            return;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return;
        }
        if (!(auth.getPrincipal() instanceof UserDetails ud)) {
            return;
        }
        String username = ud.getUsername();
        Long userId = userIdCache.get(username);
        if (userId == null) {
            userId = userRepository.findByUsername(username).map(User::getId).orElse(null);
            if (userId != null) {
                userIdCache.put(username, userId);
            }
        }
        if (userId == null) {
            return;
        }
        userActivityService.recordActivity(userId, LocalDate.now());
    }
}
