package com.memospark.core.service;

import com.memospark.core.dto.DailyStatsDto;
import com.memospark.core.dto.StatsDto;
import com.memospark.core.repository.CardProgressRepository;
import com.memospark.core.repository.CardRepository;
import com.memospark.core.repository.DeckRepository;
import com.memospark.core.repository.ReviewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardProgressRepository cardProgressRepository;
    private final ReviewLogRepository reviewLogRepository;

    public StatsDto getStats(Long userId) {
        LocalDate today = LocalDate.now();

        long totalCards = cardRepository.countByUserId(userId);
        long totalDecks = deckRepository.countByUserId(userId);
        long dueToday = cardProgressRepository.countDueCardsByUserId(userId, today);
        long reviewedToday = reviewLogRepository.countByUserIdAndReviewDate(userId, today);
        long totalReviews = reviewLogRepository.countByUserId(userId);
        double retentionRate = totalReviews > 0
                ? reviewLogRepository.calculateRetentionRateByUserId(userId)
                : 0;
        int streakDays = calculateStreak(userId);

        return new StatsDto(totalCards, totalDecks, dueToday, reviewedToday,
                totalReviews, Math.round(retentionRate * 1000) / 10.0, streakDays);
    }

    public List<DailyStatsDto> getDailyStats(Long userId, int days) {
        LocalDate since = LocalDate.now().minusDays(days - 1);
        List<Object[]> rows = reviewLogRepository.findDailyStatsSinceByUserId(userId, since);

        // Build a map of date -> stats
        Map<LocalDate, DailyStatsDto> map = rows.stream().collect(Collectors.toMap(
                r -> (LocalDate) r[0],
                r -> new DailyStatsDto((LocalDate) r[0], (Long) r[1], Math.round((Double) r[2] * 1000) / 10.0),
                (a, b) -> a
        ));

        // Fill in all dates (including zeros)
        List<DailyStatsDto> result = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate date = since.plusDays(i);
            result.add(map.getOrDefault(date, new DailyStatsDto(date, 0, 0)));
        }
        return result;
    }

    private int calculateStreak(Long userId) {
        LocalDate today = LocalDate.now();
        // Look back up to 365 days
        List<LocalDate> reviewDates = reviewLogRepository.findDistinctReviewDatesSinceByUserId(userId, today.minusDays(365));

        if (reviewDates.isEmpty()) return 0;

        int streak = 0;
        LocalDate expected = today;

        for (LocalDate date : reviewDates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                // Gap found
                break;
            }
        }

        return streak;
    }
}
