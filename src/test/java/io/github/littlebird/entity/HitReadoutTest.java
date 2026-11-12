package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The shooter's hit readout: remaining out of full, and how it degrades when it cannot know. */
class HitReadoutTest {

    @Test
    void theMaximumIsRecoveredFromTheRemainingPointsAndTheFraction() {
        assertEquals(120.0F, HitReadout.maxHealthFrom(60.0F, 0.5F), 1.0E-3F);
        assertEquals(120.0F, HitReadout.maxHealthFrom(1.0F, 1.0F / 120.0F), 0.5F);
        assertEquals(90.0F, HitReadout.maxHealthFrom(90.0F, 1.0F), 1.0E-3F);
        assertEquals(110.0F, HitReadout.maxHealthFrom(11.0F, 0.1F), 1.0E-2F);
    }

    @Test
    void anUnusablePairReadsAsUnknown() {
        assertEquals(0.0F, HitReadout.maxHealthFrom(0.0F, 0.0F), 1.0E-6F);
        assertEquals(0.0F, HitReadout.maxHealthFrom(-5.0F, 0.5F), 1.0E-6F);
        assertEquals(0.0F, HitReadout.maxHealthFrom(60.0F, 0.0F), 1.0E-6F);
        assertEquals(0.0F, HitReadout.maxHealthFrom(Float.NaN, 0.5F), 1.0E-6F);
        assertEquals(0.0F, HitReadout.maxHealthFrom(60.0F, Float.NaN), 1.0E-6F);
        assertEquals(0.0F, HitReadout.maxHealthFrom(60.0F, Float.POSITIVE_INFINITY), 1.0E-6F);
    }

    @Test
    void pointsAreRoundedAndUnusableValuesReadAsZero() {
        assertEquals(60, HitReadout.points(59.6F));
        assertEquals(120, HitReadout.points(120.0F));
        assertEquals(0, HitReadout.points(0.0F));
        assertEquals(0, HitReadout.points(-3.0F));
        assertEquals(0, HitReadout.points(Float.NaN));
    }

    @Test
    void theReadoutIsRemainingOutOfFull() {
        assertEquals("60 / 120", HitReadout.healthText(60.0F, 120.0F, false));
        assertEquals("1 / 120", HitReadout.healthText(0.6F, 120.0F, false));
        assertEquals("120 / 120", HitReadout.healthText(120.0F, 120.0F, false));
    }

    @Test
    void aKillAndAnUnknownMaximumBothReadAsNothing() {
        assertEquals("", HitReadout.healthText(0.0F, 120.0F, true));
        assertEquals("", HitReadout.healthText(60.0F, 120.0F, true));
        assertEquals("", HitReadout.healthText(60.0F, 0.0F, false));
        assertEquals("", HitReadout.healthText(Float.NaN, Float.NaN, false));
    }

    @Test
    void theNameIsTrimmedButNeverInvented() {
        assertEquals("M1A5", HitReadout.targetName("  M1A5  "));
        assertEquals("", HitReadout.targetName(null));
        assertEquals("", HitReadout.targetName("   "));
        String longName = "x".repeat(80);
        assertEquals(HitReadout.MAX_NAME_LENGTH, HitReadout.targetName(longName).length());
        assertTrue(HitReadout.targetName(longName).chars().allMatch(c -> c == 'x'));
    }
}
