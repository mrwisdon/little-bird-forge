package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the rigid cockpit arithmetic: eye point, rigid mapping, frame rectangles. */
final class B2CockpitMathTest {
    @Test
    void eyeSitsAheadOfTheSeatAndAboveIt() {
        double ahead = B2CockpitMath.cockpitEyeAheadBlocks();
        double height = B2CockpitMath.cockpitEyeHeightBlocks(0.4D);
        assertTrue(ahead > 3.0D && ahead < 4.5D, "eye ahead=" + ahead);
        assertTrue(height > 2.5D && height < 3.5D, "eye height=" + height);
        assertEquals(height + 0.5D, B2CockpitMath.cockpitEyeHeightBlocks(0.9D), 1.0E-6D);
        assertEquals(B2CockpitMath.cockpitEyeHeightBlocks(0.0D),
                B2CockpitMath.cockpitEyeHeightBlocks(-3.0D), 1.0E-6D,
                "a negative lift is clamped to zero, so the eye stays on the seat");
    }

    @Test
    void podHangsAheadOfAndBelowTheCockpit() {
        double podAhead = B2CockpitMath.podStationAheadBlocks();
        double podHeight = B2CockpitMath.podStationHeightBlocks();
        assertTrue(podAhead > 4.0D && podAhead < 9.0D, "pod ahead=" + podAhead);
        assertTrue(podHeight < B2CockpitMath.cockpitEyeHeightBlocks(0.0D), "pod height=" + podHeight);
        assertTrue(podHeight > 0.0D);
    }

    @Test
    void rigidMappingIsIdentityAndClamped() {
        assertEquals(123.0F, B2CockpitMath.rigidYawDeg(123.0F), 1.0E-4F);
        assertEquals(-175.0F, B2CockpitMath.rigidRollDeg(185.0F), 1.0E-4F);
        assertEquals(-90.0F, B2CockpitMath.rigidPitchDeg(-140.0F), 1.0E-4F);
        assertEquals(90.0F, B2CockpitMath.rigidPitchDeg(140.0F), 1.0E-4F);
        assertEquals(0.0F, B2CockpitMath.rigidPitchDeg(0.0F), 1.0E-4F);
    }

    @Test
    void frameStaysInsideTheScreen() {
        int[] win = B2CockpitMath.windscreenRect(1920, 1080);
        assertTrue(win[0] > 0 && win[1] > 0 && win[2] < 1920 && win[3] < 1080,
                java.util.Arrays.toString(win));
        int[] spine = B2CockpitMath.spineRect(1920, 1080);
        assertEquals(960, (spine[0] + spine[2]) / 2, 2);
        assertTrue(spine[2] - spine[0] >= 2);
        int[] glare = B2CockpitMath.glareShieldRect(1920, 1080);
        assertTrue(glare[1] > win[1] && glare[3] == win[3]);
        int[] tiny = B2CockpitMath.windscreenRect(64, 48);
        assertTrue(tiny[2] > tiny[0] && tiny[3] > tiny[1], java.util.Arrays.toString(tiny));
    }
}
