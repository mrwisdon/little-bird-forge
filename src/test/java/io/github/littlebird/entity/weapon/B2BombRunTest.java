package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the bomb-run policy: zero commands, and where "level enough" starts. */
final class B2BombRunTest {
    @Test
    void commandsAreZeroSoTheModelLevelsItself() {
        assertEquals(0.0F, B2BombRun.pitchCommandDeg(), 1.0E-6F);
        assertEquals(0.0D, B2BombRun.rollCommand(), 1.0E-9D);
    }

    @Test
    void levelWindowIsInclusiveAtTheTolerance() {
        assertTrue(B2BombRun.holdsLevel(0.0D, 0.0D));
        assertTrue(B2BombRun.holdsLevel(3.0D, -4.0D));
        assertTrue(B2BombRun.holdsLevel(-3.0D, 4.0D));
        assertFalse(B2BombRun.holdsLevel(3.01D, 0.0D));
        assertFalse(B2BombRun.holdsLevel(0.0D, -4.01D));
        assertFalse(B2BombRun.holdsLevel(90.0D, 30.0D));
    }

    @Test
    void bankErrorPointsBackToFlat() {
        assertEquals(-25.0D, B2BombRun.bankErrorDeg(25.0D), 1.0E-9D);
        assertEquals(0.0D, B2BombRun.bankErrorDeg(0.0D), 1.0E-9D);
        assertTrue(B2BombRun.isBanked(25.0D, 5.0D));
        assertFalse(B2BombRun.isBanked(5.0D, 5.0D));
        assertTrue(B2BombRun.isBanked(-1.0D, -5.0D),
                "a negative tolerance is clamped to zero, so any bank counts");
        assertFalse(B2BombRun.isBanked(0.0D, -5.0D));
    }
}
