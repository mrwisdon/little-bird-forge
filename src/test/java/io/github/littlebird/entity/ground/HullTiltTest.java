package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The hull tilt smoother: follows a real grade, ignores a noisy one, never snaps. */
final class HullTiltTest {
    private static final double MAX_PITCH = 30.0D;
    private static final double MAX_RATE = 2.0D;
    private static final double DEADBAND = 1.0D;
    private static final double SMOOTHING = 0.2D;

    private static HullTilt tilt() {
        return new HullTilt(MAX_PITCH, MAX_RATE, DEADBAND, SMOOTHING);
    }

    @Test
    void aSteadyUphillSettlesAtNoseUp() {
        HullTilt tilt = tilt();
        double pitch = 0.0D;
        for (int tick = 0; tick < 200; tick++) {
            pitch = tilt.update(20.0D);
        }
        assertEquals(-20.0D, pitch, 0.5D, "positive grade is nose up, so MC pitch is negative");
    }

    @Test
    void thePitchNeverMovesFasterThanTheRateLimit() {
        HullTilt tilt = tilt();
        double previous = 0.0D;
        for (int tick = 0; tick < 40; tick++) {
            double pitch = tilt.update(45.0D);
            assertTrue(Math.abs(pitch - previous) <= MAX_RATE + 1.0E-9D,
                    "step " + (pitch - previous));
            previous = pitch;
        }
    }

    @Test
    void tinyGradesAreIgnored() {
        HullTilt tilt = tilt();
        for (int tick = 0; tick < 40; tick++) {
            assertEquals(0.0D, tilt.update(0.5D), 1.0E-9D);
        }
    }

    @Test
    void aFlippingProbeKeepsTheHullCalm() {
        HullTilt tilt = tilt();
        double peak = 0.0D;
        for (int tick = 0; tick < 60; tick++) {
            double raw = (tick % 2 == 0) ? 8.0D : -8.0D;
            peak = Math.max(peak, Math.abs(tilt.update(raw)));
        }
        // A raw +/-8 flip would swing a raw feed by eight degrees; the smoother must stay tiny.
        assertTrue(peak < 3.0D, "peak pitch " + peak);
    }

    @Test
    void resetLevelsTheHull() {
        HullTilt tilt = tilt();
        for (int tick = 0; tick < 60; tick++) {
            tilt.update(20.0D);
        }
        tilt.reset();
        assertEquals(0.0D, tilt.pitch(), 1.0E-9D);
    }
}
