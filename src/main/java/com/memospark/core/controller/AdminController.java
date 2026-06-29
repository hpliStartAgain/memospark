package com.memospark.core.controller;

import com.memospark.core.dto.AdminDtos;
import com.memospark.core.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/system")
    public AdminDtos.AdminSystemInfoDto systemInfo() {
        log.info("Admin: get system info");
        return adminService.getSystemInfo();
    }

    @GetMapping("/stats")
    public AdminDtos.AdminStatsDto stats() {
        log.info("Admin: get stats");
        return adminService.getStats();
    }

    @GetMapping("/dau")
    public List<AdminDtos.AdminDauPointDto> dauTrend(@RequestParam(defaultValue = "30") int days) {
        log.info("Admin: get DAU trend for {} days", days);
        return adminService.getDauTrend(Math.min(days, 90));
    }

    @GetMapping("/users")
    public AdminDtos.AdminUserListDto users() {
        log.info("Admin: list users");
        return adminService.getUsers();
    }

    @PatchMapping("/users/{userId}/enabled")
    public AdminDtos.AdminUserDto setEnabled(
            @PathVariable Long userId,
            @RequestBody AdminDtos.SetEnabledRequest request) {
        log.info("Admin: set user {} enabled={}", userId, request.enabled());
        return adminService.setEnabled(userId, request.enabled());
    }

    @PatchMapping("/users/{userId}/role")
    public AdminDtos.AdminUserDto setRole(
            @PathVariable Long userId,
            @RequestBody AdminDtos.SetRoleRequest request) {
        log.info("Admin: set user {} role={}", userId, request.role());
        return adminService.setRole(userId, request.role());
    }

    @PostMapping("/users/{userId}/reset-password")
    public Map<String, String> resetPassword(
            @PathVariable Long userId,
            @RequestBody AdminDtos.AdminResetPasswordRequest request) {
        log.info("Admin: reset password for user {}", userId);
        adminService.resetPassword(userId, request.newPassword());
        return Map.of("message", "Password reset successful");
    }
}
