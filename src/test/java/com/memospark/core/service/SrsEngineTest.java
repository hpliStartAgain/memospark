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

    @Test
    void nextEaseFactor_quality0_dropsToMinimum() {
        double ef = engine.nextEaseFactor(2.5, 0, 1.3);
        // 2.5 + (0.1 - 5*(0.08 + 5*0.02)) = 2.5 + (0.1 - 0.9) = 1.7 -> clamped to 1.3? No: 1.7 > 1.3
        assertEquals(1.7, ef, 1e-9);
    }

    @Test
    void nextEaseFactor_quality1_dropsSignificantly() {
        double ef = engine.nextEaseFactor(2.5, 1, 1.3);
        // 2.5 + (0.1 - 4*(0.08 + 4*0.02)) = 2.5 + (0.1 - 0.64) = 1.96
        assertEquals(1.96, ef, 1e-9);
    }

    @Test
    void nextEaseFactor_quality2_dropsModerately() {
        double ef = engine.nextEaseFactor(2.5, 2, 1.3);
        // 2.5 + (0.1 - 3*(0.08 + 3*0.02)) = 2.5 + (0.1 - 0.42) = 2.18
        assertEquals(2.18, ef, 1e-9);
    }

    @Test
    void nextEaseFactor_quality4_slightIncrease() {
        double ef = engine.nextEaseFactor(2.5, 4, 1.3);
        // 2.5 + (0.1 - 1*(0.08 + 1*0.02)) = 2.5 + (0.1 - 0.1) = 2.5
        assertEquals(2.5, ef, 1e-9);
    }

    @Test
    void nextEaseFactor_alreadyAtMin_staysAtMin() {
        double ef = engine.nextEaseFactor(1.3, 0, 1.3);
        assertEquals(1.3, ef, 1e-9);
    }

    @Test
    void nextIntervalOnSuccess_thirdIntervalScalesByEaseFactor() {
        // repetitions=2, prevInterval=6, ef=2.5 -> 6*2.5=15
        assertEquals(15, engine.nextIntervalOnSuccess(2, 6, 2.5, 1, 6));
    }

    @Test
    void nextIntervalOnSuccess_customIntervals() {
        assertEquals(3, engine.nextIntervalOnSuccess(0, 0, 2.5, 3, 10));
        assertEquals(10, engine.nextIntervalOnSuccess(1, 3, 2.5, 3, 10));
    }
}
