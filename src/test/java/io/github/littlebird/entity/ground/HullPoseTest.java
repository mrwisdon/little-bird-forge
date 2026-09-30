package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The hull attitude smoother: follows a real slope, ignores a noisy one, never snaps, levels. */
final class HullPoseTest {
    private static final double MAX_PITCH = 60.0D;
    private static final double MAX_ROLL = 35.0D;
    private static final double MAX_RATE = 4.0D;
    private static final double DEADBAND = 0.75D;
    private static final double SMOOTHING = 0.35D;

    private static HullPose pose() {
        return new HullPose(MAX_PITCH, MAX_ROLL, MAX_RATE, DEADBAND, SMOOTHING);
    }

    private static GroundContact.Sample sample(double pitch, double roll) {
        return new GroundContact.Sample(pitch, roll, true);
    }

    @Test
    void aSteadyUphillSettlesAtNoseUp() {
        HullPose pose = pose();
        for (int tick = 0; tick < 200; tick++) {
            pose.update(sample(30.0D, 0.0D));
        }
        assertEquals(30.0D, pose.pitchDegrees(), 0.5D);
        assertEquals(0.0D, pose.rollDegrees(), 1.0E-9D);
    }

    @Test
    void aSideSlopeSettlesAtRoll() {
        HullPose pose = pose();
        for (int tick = 0; tick < 200; tick++) {
            pose.update(sample(0.0D, 20.0D));
        }
        assertEquals(20.0D, pose.rollDegrees(), 0.5D);
        assertEquals(0.0D, pose.pitchDegrees(), 1.0E-9D);
    }

    @Test
    void thePoseNeverMovesFasterThanTheRateLimit() {
        HullPose pose = pose();
        double previousPitch = 0.0D;
        double previousRoll = 0.0D;
        for (int tick = 0; tick < 60; tick++) {
            pose.update(sample(50.0D, 30.0D));
            assertTrue(Math.abs(pose.pitchDegrees() - previousPitch) <= MAX_RATE + 1.0E-9D);
            assertTrue(Math.abs(pose.rollDegrees() - previousRoll) <= MAX_RATE + 1.0E-9D);
            previousPitch = pose.pitchDegrees();
            previousRoll = pose.rollDegrees();
        }
    }

    @Test
    void tinySlopesAreIgnored() {
        HullPose pose = pose();
        for (int tick = 0; tick < 40; tick++) {
            pose.update(sample(0.4D, -0.4D));
        }
        assertEquals(0.0D, pose.pitchDegrees(), 1.0E-9D);
        assertEquals(0.0D, pose.rollDegrees(), 1.0E-9D);
    }

    @Test
    void aFlippingProbeKeepsTheHullCalm() {
        HullPose pose = pose();
        double peakPitch = 0.0D;
        double peakRoll = 0.0D;
        for (int tick = 0; tick < 60; tick++) {
            double sign = (tick % 2 == 0) ? 1.0D : -1.0D;
            pose.update(sample(8.0D * sign, -8.0D * sign));
            peakPitch = Math.max(peakPitch, Math.abs(pose.pitchDegrees()));
            peakRoll = Math.max(peakRoll, Math.abs(pose.rollDegrees()));
        }
        assertTrue(peakPitch < MAX_PITCH, "peak pitch " + peakPitch);
        assertTrue(peakRoll < MAX_ROLL && peakPitch < 3.0D, "peak roll " + peakRoll);
    }

    @Test
    void anInvalidSampleLevelsTheHullOut() {
        HullPose pose = pose();
        for (int tick = 0; tick < 200; tick++) {
            pose.update(sample(40.0D, 25.0D));
        }
        assertTrue(pose.pitchDegrees() > 20.0D);
        for (int tick = 0; tick < 200; tick++) {
            pose.update(GroundContact.Sample.LEVEL);
        }
        assertEquals(0.0D, pose.pitchDegrees(), 0.5D);
        assertEquals(0.0D, pose.rollDegrees(), 0.5D);
    }

    @Test
    void resetLevelsTheHull() {
        HullPose pose = pose();
        for (int tick = 0; tick < 60; tick++) {
            pose.update(sample(30.0D, 20.0D));
        }
        pose.reset();
        assertEquals(0.0D, pose.pitchDegrees(), 1.0E-9D);
        assertEquals(0.0D, pose.rollDegrees(), 1.0E-9D);
    }
}
