package com.memospark.core.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FsrsEngineTest {

    private final FsrsEngine engine = new FsrsEngine();

    @Test
    void firstSuccessfulReview_initializesMemoryAndSchedulesByRetention() {
        FsrsEngine.FsrsResult result = engine.next(
                new FsrsEngine.CardMemory(0.0, 0.0, null),
                5,
                LocalDate.of(2026, 6, 28),
                0.9,
                1);

        assertEquals(4.0, result.stability(), 1e-9);
        assertEquals(3.0, result.difficulty(), 1e-9);
        assertEquals(4, result.interval());
        assertEquals(1.0, result.retrievability(), 1e-9);
    }

    @Test
    void successfulReviewGrowsStabilityAndKeepsDifficultyInRange() {
        FsrsEngine.FsrsResult result = engine.next(
                new FsrsEngine.CardMemory(4.0, 3.0, LocalDate.of(2026, 6, 24)),
                4,
                LocalDate.of(2026, 6, 28),
                0.9,
                1);

        assertTrue(result.stability() > 4.0);
        assertTrue(result.difficulty() >= 1.0 && result.difficulty() <= 10.0);
        assertTrue(result.interval() >= 4);
        assertTrue(result.retrievability() < 1.0);
    }

    @Test
    void failedReviewReducesStabilityAndUsesFallbackInterval() {
        FsrsEngine.FsrsResult result = engine.next(
                new FsrsEngine.CardMemory(8.0, 4.0, LocalDate.of(2026, 6, 20)),
                1,
                LocalDate.of(2026, 6, 28),
                0.9,
                2);

        assertTrue(result.stability() < 8.0);
        assertTrue(result.difficulty() > 4.0);
        assertEquals(2, result.interval());
    }

    @Test
    void higherRetentionShortensInterval() {
        int relaxed = engine.intervalForRetention(10.0, 0.85);
        int strict = engine.intervalForRetention(10.0, 0.95);

        assertTrue(strict < relaxed);
    }

    @Test
    void invalidQualityThrows() {
        assertThrows(IllegalArgumentException.class, () -> engine.next(
                new FsrsEngine.CardMemory(1.0, 5.0, LocalDate.now()),
                6,
                LocalDate.now(),
                0.9,
                1));
    }
}
