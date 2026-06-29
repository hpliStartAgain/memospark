package com.memospark.core.service;

import com.memospark.core.domain.UserDailyActivity;
import com.memospark.core.repository.UserDailyActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserDailyActivityRepository userDailyActivityRepository;

    @Transactional
    public void recordActivity(Long userId, LocalDate date) {
        try {
            UserDailyActivity activity = userDailyActivityRepository
                    .findByUserIdAndActivityDate(userId, date)
                    .orElseGet(() -> new UserDailyActivity(userId, date));
            activity.setRequestCount(activity.getRequestCount() + 1);
            activity.setLastActiveAt(LocalDateTime.now());
            activity.setUpdatedAt(LocalDateTime.now());
            userDailyActivityRepository.save(activity);
        } catch (Exception e) {
            log.debug("Failed to record daily activity for user {}: {}", userId, e.getMessage());
        }
    }
}
