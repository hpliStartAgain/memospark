package com.memospark.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SrsEngineTest {

    private final SrsEngine engine = new SrsEngine();

    @Test
    void nextEaseFactor_perfectRecall_increasesByPointOne() {
        double ef = engine.nextEaseFactor(2.5, 5, 1.3);
        assertEquals(2.6, ef, 1e-9);
    }

    @Test
    void nextEaseFactor_poorRecall_decreases() {
        double ef = engine.nextEaseFactor(2.5, 3, 1.3);
        // 2.5 + (0.1 - 2*(0.08 + 2*0.02)) = 2.5 + (0.1 - 0.24) = 2.36
        assertEquals(2.36, ef, 1e-9);
    }

    @Test
    void nextEaseFactor_clampedToMinimum() {
        double ef = engine.nextEaseFactor(1.3, 0, 1.3);
        assertEquals(1.3, ef, 1e-9);
    }

    @Test
    void nextEaseFactor_invalidQuality_throws() {
        assertThrows(IllegalArgumentException.class, () -> engine.nextEaseFactor(2.5, 6, 1.3));
        assertThrows(IllegalArgumentException.class, () -> engine.nextEaseFactor(2.5, -1, 1.3));
    }

    @Test
    void nextIntervalOnSuccess_firstAndSecondUseConfiguredIntervals() {
        assertEquals(1, engine.nextIntervalOnSuccess(0, 0, 2.5, 1, 6));
        assertEquals(6, engine.nextIntervalOnSuccess(1, 1, 2.5, 1, 6));
    }

    @Test
    void nextIntervalOnSuccess_subsequentScalesByEaseFactor() {
        assertEquals(15, engine.nextIntervalOnSuccess(2, 6, 2.5, 1, 6));
    }

    @Test
    void nextIntervalOnSuccess_negativeRepetitionsTreatedAsFirst() {
        assertTrue(engine.nextIntervalOnSuccess(-1, 0, 2.5, 1, 6) == 1);
    }
}
