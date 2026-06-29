package com.memospark.core.dto;

import com.memospark.core.domain.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminDtos {

    public record AdminSystemInfoDto(
            String appName,
            String appVersion,
            String javaVersion,
            String javaVendor,
            String osName,
            String osArch,
            String springBootVersion,
            LocalDateTime startupTime,
            long uptimeSeconds,
            long heapUsedMb,
            long heapMaxMb,
            int availableProcessors,
            long threadCount) {}

    public record AdminStatsDto(
            long totalUsers,
            long enabledUsers,
            long todayNewUsers,
            long todayActiveUsers,
            long totalDecks,
            long totalCards,
            long totalReviews,
            long totalTargets,
            long todayReviews,
            double overallRetentionRate,
            long adminCount) {}

    public record AdminDauPointDto(
            LocalDate date,
            long activeUsers,
            long totalRequests) {}

    public record AdminUserDto(
            Long id,
            String username,
            UserRole role,
            boolean enabled,
            LocalDateTime createdAt,
            LocalDateTime lastActiveAt,
            long deckCount,
            long cardCount,
            long reviewCount) {}

    public record AdminUserListDto(
            java.util.List<AdminUserDto> users,
            long total) {}

    public record SetEnabledRequest(boolean enabled) {}

    public record SetRoleRequest(UserRole role) {}

    public record AdminResetPasswordRequest(String newPassword) {}
}
