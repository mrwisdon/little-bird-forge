package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The ground drive model: acceleration, braking, reverse, turning and the delta it produces. */
class GroundDriveTest {
    private static final GroundDrive.Spec SPEC =
            new GroundDrive.Spec(0.30D, 0.02D, 0.04D, 2.0D, 0.35D);

    /** No acceleration or braking, so a test can hold a fixed speed and read the direction. */
    private static final GroundDrive.Spec CONSTANT =
            new GroundDrive.Spec(0.2D, 0.0D, 0.0D, 0.0D, 0.0D);

    private static GroundDrive.State drive(GroundDrive.State state, double throttle, double steer,
                                           int ticks) {
        GroundDrive.State current = state;
        for (int i = 0; i < ticks; i++) {
            current = GroundDrive.advance(current, throttle, steer, SPEC).state();
        }
        return current;
    }

    @Test
    void reachesTopSpeedUnderFullThrottle() {
        assertEquals(SPEC.maxSpeedBlocksPerTick(), drive(GroundDrive.State.still(0.0D), 1.0D, 0.0D,
                200).speedBlocksPerTick(), 1.0E-9D);
    }

    @Test
    void coastsToAStandstillWithoutReversing() {
        GroundDrive.State moving = new GroundDrive.State(SPEC.maxSpeedBlocksPerTick(), 0.0D);
        GroundDrive.State stopped = drive(moving, 0.0D, 0.0D, 200);
        assertEquals(0.0D, stopped.speedBlocksPerTick(), 1.0E-9D);
    }

    @Test
    void reverseThrottleDrivesBackwards() {
        GroundDrive.State state = drive(GroundDrive.State.still(0.0D), -1.0D, 0.0D, 200);
        assertTrue(state.speedBlocksPerTick() < 0.0D,
                "reverse throttle should produce a negative speed, got " + state.speedBlocksPerTick());
    }

    @Test
    void trackedHullPivotsAtAStandstill() {
        GroundDrive.State turned = drive(GroundDrive.State.still(0.0D), 0.0D, 1.0D, 1);
        assertEquals(SPEC.turnRateDegreesPerTick() * SPEC.turnAtStandstillFraction(),
                turned.yawDegrees(), 1.0E-9D);
    }

    @Test
    void turningIsFasterAtSpeed() {
        GroundDrive.State fast = drive(
                new GroundDrive.State(SPEC.maxSpeedBlocksPerTick(), 0.0D), 1.0D, 1.0D, 1);
        assertEquals(SPEC.turnRateDegreesPerTick(), fast.yawDegrees(), 1.0E-9D);
    }

    @Test
    void deltaFollowsTheHeading() {
        GroundDrive.Step north = GroundDrive.advance(
                new GroundDrive.State(0.2D, 0.0D), 1.0D, 0.0D, CONSTANT);
        assertEquals(0.0D, north.dx(), 1.0E-9D);
        assertEquals(0.2D, north.dz(), 1.0E-9D);

        GroundDrive.Step east = GroundDrive.advance(
                new GroundDrive.State(0.2D, 90.0D), 1.0D, 0.0D, CONSTANT);
        assertEquals(-0.2D, east.dx(), 1.0E-9D);
        assertEquals(0.0D, east.dz(), 1.0E-9D);
    }

    @Test
    void axesAreClamped() {
        GroundDrive.State state = drive(GroundDrive.State.still(0.0D), 5.0D, 5.0D, 1);
        assertEquals(SPEC.acceleration(), state.speedBlocksPerTick(), 1.0E-9D);
        assertTrue(state.yawDegrees() > 0.0D
                        && state.yawDegrees() <= SPEC.turnRateDegreesPerTick(),
                "steer must be clamped to one full turn rate, got " + state.yawDegrees());
    }

    @Test
    void headingWrapsIntoPlusMinus180() {
        GroundDrive.State state = drive(new GroundDrive.State(0.0D, 179.0D), 0.0D, 1.0D, 20);
        assertTrue(state.yawDegrees() >= -180.0D && state.yawDegrees() < 180.0D,
                "wrapped heading out of range: " + state.yawDegrees());
    }
}
