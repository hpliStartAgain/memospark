package com.memospark.core.repository;

import com.memospark.core.domain.UserDailyActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UserDailyActivityRepository extends JpaRepository<UserDailyActivity, Long> {

    Optional<UserDailyActivity> findByUserIdAndActivityDate(Long userId, LocalDate date);

    @Query("SELECT a.activityDate, COUNT(DISTINCT a.userId), COALESCE(SUM(a.requestCount), 0) " +
           "FROM UserDailyActivity a WHERE a.activityDate >= :since " +
           "GROUP BY a.activityDate ORDER BY a.activityDate")
    List<Object[]> findDailyActivitySince(@Param("since") LocalDate since);

    @Query("SELECT COUNT(DISTINCT a.userId) FROM UserDailyActivity a WHERE a.activityDate = :date")
    long countActiveUsersByDate(@Param("date") LocalDate date);

    @Query("SELECT MAX(a.lastActiveAt) FROM UserDailyActivity a WHERE a.userId = :userId")
    Optional<java.time.LocalDateTime> findLastActiveAt(@Param("userId") Long userId);
}
