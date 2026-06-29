package com.memospark.core.service;

import com.memospark.core.domain.User;
import com.memospark.core.domain.UserRole;
import com.memospark.core.dto.AdminDtos.*;
import com.memospark.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final TargetRepository targetRepository;
    private final ReviewLogRepository reviewLogRepository;
    private final UserDailyActivityRepository userDailyActivityRepository;
    private final PasswordEncoder passwordEncoder;

    private final LocalDateTime startupTime = LocalDateTime.now();

    @Transactional(readOnly = true)
    public AdminSystemInfoDto getSystemInfo() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        Runtime r = Runtime.getRuntime();
        long heapUsed = (r.totalMemory() - r.freeMemory()) / (1024 * 1024);
        long heapMax = r.maxMemory() / (1024 * 1024);

        return new AdminSystemInfoDto(
                "MemoSpark",
                "0.0.1-SNAPSHOT",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                org.springframework.boot.SpringBootVersion.getVersion(),
                startupTime,
                runtime.getUptime() / 1000,
                heapUsed,
                heapMax,
                r.availableProcessors(),
                threads.getThreadCount());
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        long totalUsers = userRepository.count();
        long enabledUsers = userRepository.countByEnabledTrue();
        long todayNewUsers = userRepository.countByCreatedAtBetween(todayStart, tomorrowStart);
        long todayActiveUsers = userDailyActivityRepository.countActiveUsersByDate(today);
        long totalDecks = deckRepository.count();
        long totalCards = cardRepository.count();
        long totalReviews = reviewLogRepository.count();
        long totalTargets = targetRepository.count();
        long todayReviews = reviewLogRepository.countByReviewDate(today);
        double retentionRate = reviewLogRepository.calculateOverallRetentionRate();
        long adminCount = userRepository.countByRole(UserRole.ADMIN);

        return new AdminStatsDto(
                totalUsers, enabledUsers, todayNewUsers, todayActiveUsers,
                totalDecks, totalCards, totalReviews, totalTargets,
                todayReviews, Math.round(retentionRate * 1000) / 10.0, adminCount);
    }

    @Transactional(readOnly = true)
    public List<AdminDauPointDto> getDauTrend(int days) {
        LocalDate since = LocalDate.now().minusDays(days - 1);
        List<Object[]> rows = userDailyActivityRepository.findDailyActivitySince(since);
        List<AdminDauPointDto> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = since.plusDays(i);
            result.add(new AdminDauPointDto(date, 0, 0));
        }
        for (Object[] row : rows) {
            LocalDate date = (LocalDate) row[0];
            long activeUsers = (Long) row[1];
            long totalRequests = row[2] != null ? (Long) row[2] : 0;
            int idx = (int) java.time.temporal.ChronoUnit.DAYS.between(since, date);
            if (idx >= 0 && idx < result.size()) {
                result.set(idx, new AdminDauPointDto(date, activeUsers, totalRequests));
            }
        }
        return result;
    }

    @Transactional(readOnly = true)
    public AdminUserListDto getUsers() {
        List<User> users = userRepository.findAllByOrderByCreatedAtDesc();
        List<AdminUserDto> dtos = users.stream().map(this::toUserDto).toList();
        return new AdminUserListDto(dtos, users.size());
    }

    @Transactional
    public AdminUserDto setEnabled(Long userId, boolean enabled) {
        // Delegates to UserService for validation (can't ban admins)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + userId));
        if (user.getRole() == UserRole.ADMIN && !enabled) {
            throw new IllegalArgumentException("不能封禁管理员账户");
        }
        user.setEnabled(enabled);
        log.info("Admin: set user {} enabled={}", userId, enabled);
        return toUserDto(user);
    }

    @Transactional
    public AdminUserDto setRole(Long userId, UserRole role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + userId));
        user.setRole(role);
        log.info("Admin: set user {} role={}", userId, role);
        return toUserDto(user);
    }

    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 3) {
            throw new IllegalArgumentException("Password must be at least 3 characters");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new java.util.NoSuchElementException("User not found: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        log.info("Admin: password reset for user {}", userId);
    }

    private AdminUserDto toUserDto(User user) {
        LocalDateTime lastActive = userDailyActivityRepository
                .findLastActiveAt(user.getId()).orElse(null);
        long deckCount = deckRepository.countByUserId(user.getId());
        long cardCount = cardRepository.countByUserId(user.getId());
        long reviewCount = reviewLogRepository.countByUserId(user.getId());
        return new AdminUserDto(
                user.getId(), user.getUsername(), user.getRole(),
                user.isEnabled(), user.getCreatedAt(), lastActive,
                deckCount, cardCount, reviewCount);
    }
}
