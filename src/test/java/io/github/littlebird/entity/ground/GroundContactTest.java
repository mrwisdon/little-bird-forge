package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The contact plane fit: flat, nose up, rolled, and every way a probe can be unusable. */
final class GroundContactTest {
    private static final double HALF_LENGTH = 3.5D;
    private static final double HALF_WIDTH = 1.75D;

    /** Corner heights of a plane with the given pitch (nose up) and roll (right lower). */
    private static GroundContact.Sample fit(double pitchDeg, double rollDeg) {
        double rise = HALF_LENGTH * Math.tan(Math.toRadians(pitchDeg));
        double drop = HALF_WIDTH * Math.tan(Math.toRadians(rollDeg));
        return GroundContact.fit(rise + drop, rise - drop, -rise + drop, -rise - drop,
                HALF_LENGTH, HALF_WIDTH);
    }

    @Test
    void flatGroundIsLevelAndValid() {
        GroundContact.Sample sample = GroundContact.fit(64.0D, 64.0D, 64.0D, 64.0D,
                HALF_LENGTH, HALF_WIDTH);
        assertTrue(sample.valid());
        assertEquals(0.0D, sample.pitchDegrees(), 1.0E-9D);
        assertEquals(0.0D, sample.rollDegrees(), 1.0E-9D);
    }

    @Test
    void anUphillPlaneReadsNoseUp() {
        GroundContact.Sample sample = fit(30.0D, 0.0D);
        assertTrue(sample.valid());
        assertEquals(30.0D, sample.pitchDegrees(), 1.0E-6D);
        assertEquals(0.0D, sample.rollDegrees(), 1.0E-6D);
    }

    @Test
    void aRightwardTiltReadsPositiveRoll() {
        GroundContact.Sample sample = fit(0.0D, 20.0D);
        assertTrue(sample.valid());
        assertEquals(0.0D, sample.pitchDegrees(), 1.0E-6D);
        assertEquals(20.0D, sample.rollDegrees(), 1.0E-6D);
    }

    @Test
    void bothAxesComposeIndependently() {
        GroundContact.Sample sample = fit(25.0D, -15.0D);
        assertTrue(sample.valid());
        assertEquals(25.0D, sample.pitchDegrees(), 1.0E-6D);
        assertEquals(-15.0D, sample.rollDegrees(), 1.0E-6D);
    }

    @Test
    void aMissedProbeIsInvalid() {
        GroundContact.Sample sample = GroundContact.fit(Double.NaN, 64.0D, 64.0D, 64.0D,
                HALF_LENGTH, HALF_WIDTH);
        assertFalse(sample.valid());
    }

    @Test
    void aGradePastTheLimitIsInvalidNotASnap() {
        assertFalse(fit(75.0D, 0.0D).valid(), "a wall must not be read as a 75 degree climb");
        assertFalse(fit(0.0D, 75.0D).valid());
    }

    @Test
    void sillyProbeGeometryIsInvalid() {
        assertFalse(GroundContact.fit(64.0D, 64.0D, 64.0D, 64.0D, 0.0D, HALF_WIDTH).valid());
        assertFalse(GroundContact.fit(64.0D, 64.0D, 64.0D, 64.0D, HALF_LENGTH, -1.0D).valid());
    }
}
