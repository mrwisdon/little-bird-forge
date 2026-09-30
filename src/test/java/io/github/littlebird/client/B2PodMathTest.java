package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Adversarial edges for the pod rules: limits, zoom curve ends, ray directions. */
final class B2PodMathTest {
    private static final double YAW_LIMIT = 110.0D;
    private static final double PITCH_MIN = -120.0D;
    private static final double PITCH_MAX = 10.0D;

    @Test
    void gimbalClampsBothAxes() {
        B2PodMath.Gimbal g = B2PodMath.clamp(400.0D, 90.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX);
        assertEquals(YAW_LIMIT, g.yawDeg(), 1.0E-9D);
        assertEquals(PITCH_MAX, g.pitchDeg(), 1.0E-9D);
        B2PodMath.Gimbal low = B2PodMath.clamp(-400.0D, -400.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX);
        assertEquals(-YAW_LIMIT, low.yawDeg(), 1.0E-9D);
        assertEquals(PITCH_MIN, low.pitchDeg(), 1.0E-9D);
    }

    @Test
    void dragStacksAndStaysInsideTheLimits() {
        B2PodMath.Gimbal start = new B2PodMath.Gimbal(0.0D, -20.0D);
        B2PodMath.Gimbal moved = B2PodMath.drag(start, 30.0D, -10.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX);
        assertEquals(30.0D, moved.yawDeg(), 1.0E-9D);
        assertEquals(-30.0D, moved.pitchDeg(), 1.0E-9D);
        for (int i = 0; i < 200; i++) {
            moved = B2PodMath.drag(moved, 10.0D, 10.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX);
        }
        assertEquals(YAW_LIMIT, moved.yawDeg(), 1.0E-9D);
        assertEquals(PITCH_MAX, moved.pitchDeg(), 1.0E-9D);
    }

    @Test
    void zoomCurveIsMonotonicAndHitsBothEnds() {
        assertEquals(1.0D, B2PodMath.fovFor(0.0D, 0.16D, 1.0D), 1.0E-9D);
        assertEquals(0.16D, B2PodMath.fovFor(1.0D, 0.16D, 1.0D), 1.0E-9D);
        double previous = 2.0D;
        for (double t = 0.0D; t <= 1.0001D; t += 0.05D) {
            double fov = B2PodMath.fovFor(t, 0.16D, 1.0D);
            assertTrue(fov < previous, "t=" + t + " fov=" + fov);
            previous = fov;
        }
        assertEquals(0.5D, B2PodMath.zoomFor(B2PodMath.fovFor(0.5D, 0.16D, 1.0D), 0.16D, 1.0D),
                1.0E-6D);
    }

    @Test
    void levelSightHitsTheGroundAheadButSkyDoesNot() {
        assertNull(B2PodMath.groundIntersection(0.0D, 100.0D, 0.0D, 0.0D, 0.0D, 64.0D),
                "a level sight line never reaches the ground");
        double[] below = B2PodMath.groundIntersection(0.0D, 100.0D, 0.0D, 0.0D, 30.0D, 64.0D);
        assertNotNull(below);
        assertEquals(64.0D, below[1], 1.0E-6D);
        assertEquals(36.0D / Math.tan(Math.toRadians(30.0D)), below[2], 1.0E-6D);
        assertNull(B2PodMath.groundIntersection(0.0D, 100.0D, 0.0D, 0.0D, -30.0D, 64.0D),
                "looking up must not designate");
        assertNull(B2PodMath.groundIntersection(0.0D, 40.0D, 0.0D, 0.0D, 30.0D, 64.0D),
                "ground above the lens is not a target either");
        assertEquals(-1.0D, B2PodMath.range(0.0D, 0.0D, 0.0D, null), 1.0E-9D);
        assertEquals(5.0D, B2PodMath.range(0.0D, 0.0D, 0.0D, new double[]{3.0D, 4.0D, 0.0D}), 1.0E-9D);
    }

    @Test
    void reticleOffsetWrapsAndLocks() {
        B2PodMath.Gimbal camera = new B2PodMath.Gimbal(179.0D, 0.0D);
        B2PodMath.Gimbal gimbal = new B2PodMath.Gimbal(-179.0D, 0.0D);
        double[] offset = B2PodMath.reticleOffset(camera, gimbal);
        assertEquals(2.0D, offset[0], 1.0E-6D);
        assertTrue(B2PodMath.insideLockWindow(camera, new B2PodMath.Gimbal(180.0D, 1.0D), 3.0D));
        assertFalse(B2PodMath.insideLockWindow(camera, gimbal, 1.0D));
    }

    @Test
    void pitchWindowFloorsKeepTheSightPointedAtTheGround() {
        // The bug: the shipped config said "negative looks down" while the maths (and Minecraft)
        // say positive is down, so the pod could only tilt ten degrees down and never saw a target.
        double[] stale = B2PodMath.pitchWindow(-120.0D, 10.0D);
        assertEquals(B2PodMath.MIN_SIGHT_UP_DEG, stale[0], 1.0E-9D);
        assertEquals(B2PodMath.MIN_SIGHT_DOWN_DEG, stale[1], 1.0E-9D,
                "a ten degree downward limit must be raised, or the pod is blind");
        double[] sane = B2PodMath.pitchWindow(12.0D, 85.0D);
        assertEquals(12.0D, sane[0], 1.0E-9D);
        assertEquals(85.0D, sane[1], 1.0E-9D);
        double[] negative = B2PodMath.pitchWindow(-5.0D, -30.0D);
        assertEquals(B2PodMath.MIN_SIGHT_UP_DEG, negative[0], 1.0E-9D);
        assertEquals(B2PodMath.MIN_SIGHT_DOWN_DEG, negative[1], 1.0E-9D);
    }

    @Test
    void mouseTravelMapsStraightOntoTheSight() {
        // Minecraft xRot and the gimbal pitch are both positive downwards: mouse down, sight down.
        double[] travel = B2PodMath.mouseToGimbal(3.0D, 5.0D);
        assertEquals(3.0D, travel[0], 1.0E-9D);
        assertEquals(5.0D, travel[1], 1.0E-9D);
        B2PodMath.Gimbal gimbal = B2PodMath.drag(new B2PodMath.Gimbal(0.0D, 0.0D),
                travel[0], travel[1], YAW_LIMIT, -12.0D, 85.0D);
        assertTrue(gimbal.pitchDeg() > 0.0D, "pushing the mouse down must tilt the sight down");
    }

    @Test
    void sightFovIsDegreesAndSurvivesAStaleConfig() {
        assertEquals(70.0D, B2PodMath.sightFov(0.0D, 6.0D, 70.0D), 1.0E-9D);
        assertEquals(6.0D, B2PodMath.sightFov(1.0D, 6.0D, 70.0D), 1.0E-9D);
        double previous = 999.0D;
        for (double t = 0.0D; t <= 1.0001D; t += 0.1D) {
            double fov = B2PodMath.sightFov(t, 6.0D, 70.0D);
            assertTrue(fov < previous, "t=" + t + " fov=" + fov);
            previous = fov;
        }
        // Old multiplier style values must not render the world through a one degree straw.
        double staleTight = B2PodMath.sightFov(1.0D, 0.16D, 1.0D);
        assertTrue(staleTight >= 2.0D, "tight fov=" + staleTight);
        double staleWide = B2PodMath.sightFov(0.0D, 0.16D, 1.0D);
        assertTrue(staleWide >= 20.0D, "wide fov=" + staleWide);
        assertTrue(staleWide > staleTight);
    }
}
