package com.memospark.core.service;

import org.springframework.stereotype.Component;

/**
 * Shared SM-2 spaced-repetition primitives.
 * <p>
 * Both flashcard reviews ({@link SpacedRepetitionService}) and wrong-problem
 * retries ({@link ProblemNoteService}) build their scheduling on these helpers,
 * eliminating the previously duplicated SM-2 implementations.
 */
@Component
public class SrsEngine {

    public static final double DEFAULT_MIN_EASE_FACTOR = 1.3;

    /**
     * SM-2 ease-factor update, clamped to a minimum.
     *
     * @param current  current ease factor
     * @param quality  recall quality in [0,5]
     * @param minEase  lower bound for the ease factor
     */
    public double nextEaseFactor(double current, int quality, double minEase) {
        requireValidQuality(quality);
        double ef = current + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        return Math.max(minEase, ef);
    }

    /**
     * SM-2 interval progression on a successful recall (quality &gt;= 3).
     *
     * @param repetitions   number of prior consecutive successful recalls (0-based)
     * @param prevInterval  the previous interval in days
     * @param easeFactor    the (updated) ease factor
     * @param firstInterval interval used for the first success
     * @param secondInterval interval used for the second success
     */
    public int nextIntervalOnSuccess(int repetitions, int prevInterval, double easeFactor,
                                     int firstInterval, int secondInterval) {
        if (repetitions <= 0) {
            return firstInterval;
        }
        if (repetitions == 1) {
            return secondInterval;
        }
        return (int) Math.round(prevInterval * easeFactor);
    }

    private void requireValidQuality(int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("Quality must be between 0 and 5");
        }
    }
}
