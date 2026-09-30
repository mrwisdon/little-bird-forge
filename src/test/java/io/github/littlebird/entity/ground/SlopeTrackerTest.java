package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The travelled slope estimator: flat is zero, a climb converges, standing still levels out. */
final class SlopeTrackerTest {
    @Test
    void flatGroundReadsZero() {
        SlopeTracker tracker = new SlopeTracker();
        for (int tick = 0; tick < 60; tick++) {
            tracker.update(0.0D, 0.45D);
        }
        assertEquals(0.0D, tracker.gradeDegrees(), 1.0E-9D);
    }

    @Test
    void aSteadyClimbConvergesOnItsSlope() {
        SlopeTracker tracker = new SlopeTracker();
        // Climbing 0.45 vertical per 0.45 horizontal is 45 degrees.
        for (int tick = 0; tick < 200; tick++) {
            tracker.update(0.45D, 0.45D);
        }
        assertEquals(45.0D, tracker.gradeDegrees(), 0.5D);
    }

    @Test
    void aBlockedLipIsNotACliff() {
        SlopeTracker tracker = new SlopeTracker();
        for (int tick = 0; tick < 60; tick++) {
            tracker.update(0.0D, 0.45D); // pushing a wall: no climb, drive still asks to move
        }
        assertEquals(0.0D, tracker.gradeDegrees(), 1.0E-9D);
    }

    @Test
    void standingStillLevelsOut() {
        SlopeTracker tracker = new SlopeTracker();
        for (int tick = 0; tick < 200; tick++) {
            tracker.update(0.45D, 0.45D);
        }
        assertTrue(tracker.gradeDegrees() > 40.0D);
        for (int tick = 0; tick < 80; tick++) {
            tracker.update(0.0D, 0.0D);
        }
        assertEquals(0.0D, tracker.gradeDegrees(), 1.0E-9D);
    }
}
