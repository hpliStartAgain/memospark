package com.memospark.core.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Local FSRS/DSR scheduler for flashcards.
 *
 * <p>MemoSpark keeps the old SM-2 fields for compatibility, but card scheduling
 * now uses stability, difficulty, and target retention. Grades still use the
 * app's existing 0-5 quality scale, where 3+ means a successful recall.</p>
 */
@Component
public class FsrsEngine {

    public static final double DEFAULT_DIFFICULTY = 5.0;
    public static final double DEFAULT_STABILITY = 0.0;
    public static final double DEFAULT_DESIRED_RETENTION = 0.9;

    private static final double DECAY = -0.5;
    private static final double FACTOR = Math.pow(0.9, 1.0 / DECAY) - 1.0;
    private static final double MIN_RETENTION = 0.7;
    private static final double MAX_RETENTION = 0.98;

    public FsrsResult next(CardMemory memory, int quality, LocalDate reviewDate,
                           double desiredRetention, int fallbackInterval) {
        requireValidQuality(quality);
        LocalDate today = reviewDate != null ? reviewDate : LocalDate.now();
        double retention = clamp(desiredRetention, MIN_RETENTION, MAX_RETENTION);

        boolean hasHistory = memory.stability() > 0.0 && memory.lastReviewDate() != null;
        long elapsedDays = hasHistory
                ? Math.max(0, ChronoUnit.DAYS.between(memory.lastReviewDate(), today))
                : 0;
        double retrievability = hasHistory ? retrievability(memory.stability(), elapsedDays) : 1.0;

        double nextDifficulty = hasHistory
                ? nextDifficulty(memory.difficulty(), quality)
                : initialDifficulty(quality);
        double nextStability = hasHistory
                ? nextStability(memory.stability(), nextDifficulty, retrievability, quality)
                : initialStability(quality);

        int interval = quality >= 3
                ? intervalForRetention(nextStability, retention)
                : Math.max(1, fallbackInterval);

        return new FsrsResult(nextStability, nextDifficulty, interval, retrievability);
    }

    public double retrievability(double stability, long elapsedDays) {
        if (stability <= 0.0) {
            return elapsedDays <= 0 ? 1.0 : 0.0;
        }
        return Math.pow(1.0 + FACTOR * Math.max(0, elapsedDays) / stability, DECAY);
    }

    public int intervalForRetention(double stability, double desiredRetention) {
        if (stability <= 0.0) {
            return 1;
        }
        double retention = clamp(desiredRetention, MIN_RETENTION, MAX_RETENTION);
        double days = stability / FACTOR * (Math.pow(retention, 1.0 / DECAY) - 1.0);
        return Math.max(1, (int) Math.round(days));
    }

    private double initialStability(int quality) {
        return switch (quality) {
            case 0 -> 0.25;
            case 1 -> 0.4;
            case 2 -> 0.6;
            case 3 -> 1.0;
            case 4 -> 2.5;
            case 5 -> 4.0;
            default -> throw new IllegalArgumentException("Quality must be between 0 and 5");
        };
    }

    private double initialDifficulty(int quality) {
        return clamp(8.0 - quality, 1.0, 10.0);
    }

    private double nextDifficulty(double currentDifficulty, int quality) {
        double base = currentDifficulty > 0.0 ? currentDifficulty : DEFAULT_DIFFICULTY;
        double delta = switch (quality) {
            case 0 -> 1.2;
            case 1 -> 1.0;
            case 2 -> 0.8;
            case 3 -> 0.3;
            case 4 -> -0.15;
            case 5 -> -0.6;
            default -> throw new IllegalArgumentException("Quality must be between 0 and 5");
        };
        double withRecallSignal = base + delta;
        double meanReverted = withRecallSignal * 0.9 + DEFAULT_DIFFICULTY * 0.1;
        return clamp(meanReverted, 1.0, 10.0);
    }

    private double nextStability(double currentStability, double difficulty,
                                 double retrievability, int quality) {
        if (quality < 3) {
            double lapseFactor = switch (quality) {
                case 0 -> 0.35;
                case 1 -> 0.5;
                case 2 -> 0.65;
                default -> 0.65;
            };
            return Math.max(0.25, currentStability * lapseFactor);
        }

        double gradeBonus = switch (quality) {
            case 3 -> 0.8;
            case 4 -> 1.0;
            case 5 -> 1.25;
            default -> throw new IllegalArgumentException("Quality must be between 0 and 5");
        };
        double difficultyFactor = Math.max(0.1, (11.0 - difficulty) / 10.0);
        double retrievabilityFactor = 1.0 + Math.max(0.0, 1.0 - retrievability) * 2.5;
        double stabilitySaturation = Math.pow(Math.max(currentStability, 0.1), -0.35);
        double growth = 1.0 + gradeBonus * difficultyFactor * retrievabilityFactor * stabilitySaturation;
        return Math.max(currentStability + 0.1, currentStability * growth);
    }

    private void requireValidQuality(int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5");
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public record CardMemory(double stability, double difficulty, LocalDate lastReviewDate) {}

    public record FsrsResult(double stability, double difficulty, int interval, double retrievability) {}
}
