package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Adversarial acceptance for the electro-optical pod math (frozen spec 0.7 / section 3).
 *
 * <p>Verifier-written. The cases drive the gimbal past both stops, look straight up, straight down
 * and dead level, invert the zoom curve, cross the +-180 yaw seam, and hand every helper
 * non-finite input - instead of replaying the happy path.
 */
final class B2PodMathAdversarialTest {
    private static final double YAW_LIMIT = 110.0D;
    private static final double PITCH_MIN = -120.0D;
    private static final double PITCH_MAX = 10.0D;
    private static final double FOV_MIN = 0.16D;
    private static final double FOV_MAX = 1.0D;
    private static final double EPS = 1.0E-9D;

    // ------------------------------------------------------------------ gimbal limits

    @Test
    void clampPinsBothYawStopsInclusively() {
        assertEquals(YAW_LIMIT, B2PodMath.clamp(500.0D, 0.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).yawDeg(), EPS);
        assertEquals(-YAW_LIMIT, B2PodMath.clamp(-500.0D, 0.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).yawDeg(), EPS);
        assertEquals(YAW_LIMIT, B2PodMath.clamp(YAW_LIMIT, 0.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).yawDeg(), EPS);
        assertEquals(-YAW_LIMIT, B2PodMath.clamp(-YAW_LIMIT, 0.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).yawDeg(), EPS);
        assertEquals(0.0D, B2PodMath.clamp(0.0D, 0.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).yawDeg(), EPS);
        assertEquals(37.5D, B2PodMath.clamp(37.5D, 0.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).yawDeg(), EPS);
    }

    @Test
    void clampPinsBothPitchStopsInclusively() {
        assertEquals(PITCH_MAX, B2PodMath.clamp(0.0D, 1000.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).pitchDeg(), EPS);
        assertEquals(PITCH_MIN, B2PodMath.clamp(0.0D, -1000.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).pitchDeg(), EPS);
        assertEquals(PITCH_MAX, B2PodMath.clamp(0.0D, PITCH_MAX, YAW_LIMIT, PITCH_MIN, PITCH_MAX).pitchDeg(), EPS);
        assertEquals(PITCH_MIN, B2PodMath.clamp(0.0D, PITCH_MIN, YAW_LIMIT, PITCH_MIN, PITCH_MAX).pitchDeg(), EPS);
        assertEquals(0.0D, B2PodMath.clamp(0.0D, 0.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX).pitchDeg(), EPS);
        assertTrue(PITCH_MIN < PITCH_MAX, "the frozen pod range must not be empty");
        assertTrue(PITCH_MIN <= -90.0D, "the pod must be able to look straight down");
        assertTrue(PITCH_MAX <= 10.0D, "the pod must not look far above the horizon");
    }

    @Test
    void draggingPastAStopStopsThereAndComesBackImmediately() {
        B2PodMath.Gimbal parked = new B2PodMath.Gimbal(YAW_LIMIT, PITCH_MAX);
        B2PodMath.Gimbal pushed = B2PodMath.drag(parked, 500.0D, 500.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX);
        assertEquals(YAW_LIMIT, pushed.yawDeg(), EPS);
        assertEquals(PITCH_MAX, pushed.pitchDeg(), EPS);
        B2PodMath.Gimbal back = B2PodMath.drag(pushed, -1.0D, -1.0D, YAW_LIMIT, PITCH_MIN, PITCH_MAX);
        assertEquals(YAW_LIMIT - 1.0D, back.yawDeg(), EPS, "one degree back must move, not stick");
        assertEquals(PITCH_MAX - 1.0D, back.pitchDeg(), EPS);
    }

    @Test
    void draggingIsAssociativeOverManyTinySteps() {
        B2PodMath.Gimbal gimbal = new B2PodMath.Gimbal(0.0D, 0.0D);
        for (int i = 0; i < 1000; i++) {
            gimbal = B2PodMath.drag(gimbal, 0.5D, -0.5D, YAW_LIMIT, PITCH_MIN, PITCH_MAX);
        }
        assertEquals(YAW_LIMIT, gimbal.yawDeg(), EPS, "500 degrees of drag must saturate at the stop");
        assertEquals(-120.0D, gimbal.pitchDeg(), EPS);
    }

    @Test
    void cameraChaseEndpointsAndNoOvershoot() {
        B2PodMath.Gimbal camera = new B2PodMath.Gimbal(0.0D, 0.0D);
        B2PodMath.Gimbal target = new B2PodMath.Gimbal(40.0D, -20.0D);
        assertEquals(0.0D, B2PodMath.chase(camera, target, 0.0D).yawDeg(), EPS);
        assertEquals(40.0D, B2PodMath.chase(camera, target, 1.0D).yawDeg(), EPS);
        B2PodMath.Gimbal half = B2PodMath.chase(camera, target, 0.5D);
        assertEquals(20.0D, half.yawDeg(), EPS);
        assertEquals(-10.0D, half.pitchDeg(), EPS);
        assertTrue(B2PodMath.chase(camera, target, 5.0D).yawDeg() <= 40.0D, "follow must clamp to 1");
        assertEquals(0.0D, B2PodMath.chase(camera, target, -5.0D).yawDeg(), EPS, "follow must clamp to 0");
    }

    // ------------------------------------------------------------------ lines of sight

    @Test
    void lookVectorIsUnitLengthIncludingStraightUpAndDown() {
        double[][] angles = {{0.0D, 0.0D}, {90.0D, 0.0D}, {180.0D, 0.0D}, {-90.0D, 0.0D},
                {0.0D, 90.0D}, {0.0D, -90.0D}, {45.0D, -45.0D}, {-135.0D, 30.0D},
                {359.9D, 89.9D}, {720.0D, 0.0D}};
        for (double[] a : angles) {
            double[] dir = B2PodMath.lookVector(a[0], a[1]);
            assertEquals(3, dir.length);
            assertEquals(1.0D, Math.sqrt(dir[0] * dir[0] + dir[1] * dir[1] + dir[2] * dir[2]), EPS,
                    "look vector must be unit length at yaw=" + a[0] + " pitch=" + a[1]);
        }
        double[] level = B2PodMath.lookVector(0.0D, 0.0D);
        assertEquals(0.0D, level[1], EPS, "level means no vertical component");
        assertEquals(1.0D, level[2], EPS, "yaw 0 looks towards +Z");
        double[] up = B2PodMath.lookVector(0.0D, -90.0D);
        assertEquals(1.0D, up[1], EPS, "pitch -90 is straight up: Minecraft +Y is up");
        double[] down = B2PodMath.lookVector(0.0D, 90.0D);
        assertEquals(-1.0D, down[1], EPS, "pitch 90 is straight down: Minecraft -Y is down");
    }

    @Test
    void levelAndClimbingSightLinesNeverHitTheGround() {
        assertNull(B2PodMath.groundIntersection(0.0D, 100.0D, 0.0D, 0.0D, 0.0D, 64.0D),
                "dead level never meets a horizontal plane below");
        assertNull(B2PodMath.groundIntersection(0.0D, 100.0D, 0.0D, 0.0D, -30.0D, 64.0D),
                "climbing away never meets the ground");
        assertNull(B2PodMath.groundIntersection(0.0D, 100.0D, 0.0D, 95.0D, -89.0D, 64.0D),
                "nearly straight up never meets the ground");
        assertNull(B2PodMath.groundIntersection(0.0D, 40.0D, 0.0D, 0.0D, 10.0D, 64.0D),
                "a target plane above the pod and a downward sight line never meet");
    }

    @Test
    void straightDownHitsExactlyBelowThePod() {
        double[] hit = B2PodMath.groundIntersection(12.0D, 200.0D, -7.0D, 123.0D, 90.0D, 63.0D);
        assertNotNull(hit);
        assertEquals(12.0D, hit[0], 1.0E-6D);
        assertEquals(63.0D, hit[1], 1.0E-6D);
        assertEquals(-7.0D, hit[2], 1.0E-6D, "straight down ignores yaw");
    }

    @Test
    void everyIntersectionSitsOnTheSightLineAndOnTheGround() {
        for (double yaw = -180.0D; yaw <= 180.0D; yaw += 15.0D) {
            for (double pitch = 1.0D; pitch <= 89.0D; pitch += 8.0D) {
                double[] hit = B2PodMath.groundIntersection(3.0D, 120.0D, -9.0D, yaw, pitch, 64.0D);
                assertNotNull(hit, "a downward line must hit the ground: yaw=" + yaw + " pitch=" + pitch);
                assertEquals(64.0D, hit[1], 1.0E-6D, "hit must lie on the ground plane");
                double[] dir = B2PodMath.lookVector(yaw, pitch);
                double scale = (hit[1] - 120.0D) / dir[1];
                assertTrue(scale > 0.0D, "the hit must be in front of the pod");
                assertEquals(3.0D + dir[0] * scale, hit[0], 1.0E-6D);
                assertEquals(-9.0D + dir[2] * scale, hit[2], 1.0E-6D);
            }
        }
    }

    @Test
    void rangeGrowsAsTheSightLineFlattens() {
        double previous = -1.0D;
        for (double pitch = 89.0D; pitch >= 1.0D; pitch -= 4.0D) {
            double[] hit = B2PodMath.groundIntersection(0.0D, 120.0D, 0.0D, 30.0D, pitch, 64.0D);
            assertNotNull(hit);
            double range = B2PodMath.range(0.0D, 120.0D, 0.0D, hit);
            assertTrue(range > previous, "flattening must push the ground hit further away");
            previous = range;
        }
        assertEquals(-1.0D, B2PodMath.range(0.0D, 0.0D, 0.0D, null), EPS, "no point = -1 range");
        assertTrue(B2PodMath.range(0.0D, 0.0D, 0.0D, new double[] {3.0D, 4.0D, 12.0D}) == 13.0D);
    }

    // ------------------------------------------------------------------ zoom curve

    @Test
    void zoomCurveHitsBothFrozenEndpointsAndNeverWidens() {
        assertEquals(FOV_MAX, B2PodMath.fovFor(0.0D, FOV_MIN, FOV_MAX), 1.0E-12D, "t=0 is the widest view");
        assertEquals(FOV_MIN, B2PodMath.fovFor(1.0D, FOV_MIN, FOV_MAX), 1.0E-12D, "t=1 is the tightest view");
        double previous = B2PodMath.fovFor(0.0D, FOV_MIN, FOV_MAX);
        for (double t = 0.0D; t <= 1.0D; t += 0.01D) {
            double fov = B2PodMath.fovFor(t, FOV_MIN, FOV_MAX);
            assertTrue(fov <= previous + 1.0E-12D, "the curve must never widen: t=" + t);
            assertTrue(fov >= FOV_MIN - 1.0E-12D && fov <= FOV_MAX + 1.0E-12D,
                    "fov left the frozen window: t=" + t + " fov=" + fov);
            previous = fov;
        }
        assertEquals(FOV_MAX, B2PodMath.fovFor(-5.0D, FOV_MIN, FOV_MAX), 1.0E-12D, "t below 0 clamps");
        assertEquals(FOV_MIN, B2PodMath.fovFor(5.0D, FOV_MIN, FOV_MAX), 1.0E-12D, "t above 1 clamps");
        assertEquals(1.0D, B2PodMath.fovFor(0.5D, 1.0D, 1.0D), 1.0E-12D, "a degenerate band stays finite");
    }

    @Test
    void zoomForInvertsFovForEverywhere() {
        for (double t = 0.0D; t <= 1.0D; t += 0.05D) {
            double fov = B2PodMath.fovFor(t, FOV_MIN, FOV_MAX);
            assertEquals(t, B2PodMath.zoomFor(fov, FOV_MIN, FOV_MAX), 1.0E-9D, "round trip at t=" + t);
        }
        assertEquals(0.0D, B2PodMath.zoomFor(FOV_MAX, FOV_MIN, FOV_MAX), 1.0E-9D);
        assertEquals(1.0D, B2PodMath.zoomFor(FOV_MIN, FOV_MIN, FOV_MAX), 1.0E-9D);
        assertEquals(0.0D, B2PodMath.zoomFor(10.0D, FOV_MIN, FOV_MAX), 1.0E-9D, "wider than max clamps to t=0");
        assertEquals(1.0D, B2PodMath.zoomFor(0.001D, FOV_MIN, FOV_MAX), 1.0E-9D, "tighter than min clamps to t=1");
    }

    // ------------------------------------------------------------------ yaw seam

    @Test
    void wrapStaysInsideTheHalfOpenHalfTurn() {
        assertEquals(0.0D, B2PodMath.wrap(0.0D), EPS);
        assertEquals(0.0D, B2PodMath.wrap(360.0D), EPS);
        assertEquals(0.0D, B2PodMath.wrap(-720.0D), EPS);
        assertEquals(-180.0D, B2PodMath.wrap(180.0D), EPS, "the seam resolves to -180, never +180");
        assertEquals(-180.0D, B2PodMath.wrap(-180.0D), EPS);
        assertEquals(-180.0D, B2PodMath.wrap(540.0D), EPS);
        assertEquals(-1.0D, B2PodMath.wrap(359.0D), 1.0E-9D);
        assertEquals(1.0D, B2PodMath.wrap(-359.0D), 1.0E-9D);
        for (double d = -2000.0D; d <= 2000.0D; d += 7.0D) {
            double wrapped = B2PodMath.wrap(d);
            assertTrue(wrapped >= -180.0D && wrapped < 180.0D, "wrap left the seam: " + d + " -> " + wrapped);
        }
    }

    @Test
    void reticleOffsetCrossesTheSeamTheShortWay() {
        double[] offset = B2PodMath.reticleOffset(new B2PodMath.Gimbal(170.0D, 0.0D),
                new B2PodMath.Gimbal(-170.0D, 0.0D));
        assertEquals(20.0D, offset[0], 1.0E-9D, "must be +20, not -340");
        double[] other = B2PodMath.reticleOffset(new B2PodMath.Gimbal(-170.0D, 0.0D),
                new B2PodMath.Gimbal(170.0D, 0.0D));
        assertEquals(-20.0D, other[0], 1.0E-9D);
        double[] pitch = B2PodMath.reticleOffset(new B2PodMath.Gimbal(0.0D, -30.0D),
                new B2PodMath.Gimbal(0.0D, -100.0D));
        assertEquals(-70.0D, pitch[1], 1.0E-9D, "pitch is not wrapped");
    }

    @Test
    void lockWindowIsInclusiveAndSeamAware() {
        B2PodMath.Gimbal camera = new B2PodMath.Gimbal(179.0D, 0.0D);
        B2PodMath.Gimbal gimbal = new B2PodMath.Gimbal(-179.0D, 0.0D);
        assertTrue(B2PodMath.insideLockWindow(camera, gimbal, 5.0D), "two degrees over the seam is locked");
        assertTrue(B2PodMath.insideLockWindow(camera, gimbal, 2.0D), "exactly on the window is locked");
        assertFalse(B2PodMath.insideLockWindow(camera, gimbal, 1.9D));
        assertTrue(B2PodMath.insideLockWindow(new B2PodMath.Gimbal(0.0D, 0.0D),
                new B2PodMath.Gimbal(0.0D, 0.0D), 0.0D), "dead centre with a zero window is locked");
        assertTrue(B2PodMath.insideLockWindow(new B2PodMath.Gimbal(0.0D, 0.0D),
                new B2PodMath.Gimbal(0.0D, -3.0D), 3.0D));
        assertFalse(B2PodMath.insideLockWindow(new B2PodMath.Gimbal(0.0D, 0.0D),
                new B2PodMath.Gimbal(0.0D, -3.001D), 3.0D));
    }

    // ------------------------------------------------------------------ non-finite input

    @Test
    void nonFiniteInputsAreSurvivable() {
        assertDoesNotThrow(() -> B2PodMath.clamp(Double.NaN, Double.NaN, YAW_LIMIT, PITCH_MIN, PITCH_MAX));
        assertDoesNotThrow(() -> B2PodMath.drag(new B2PodMath.Gimbal(Double.NaN, Double.NaN),
                Double.NaN, Double.NaN, YAW_LIMIT, PITCH_MIN, PITCH_MAX));
        assertDoesNotThrow(() -> B2PodMath.chase(new B2PodMath.Gimbal(0.0D, 0.0D),
                new B2PodMath.Gimbal(10.0D, 10.0D), Double.NaN));
        assertDoesNotThrow(() -> B2PodMath.fovFor(Double.NaN, FOV_MIN, FOV_MAX));
        assertDoesNotThrow(() -> B2PodMath.zoomFor(Double.NaN, FOV_MIN, FOV_MAX));
        assertDoesNotThrow(() -> B2PodMath.lookVector(Double.NaN, Double.NaN));
        assertDoesNotThrow(() -> B2PodMath.groundIntersection(Double.NaN, Double.NaN, Double.NaN,
                Double.NaN, Double.NaN, 64.0D));
        assertDoesNotThrow(() -> B2PodMath.wrap(Double.NaN));
        assertDoesNotThrow(() -> B2PodMath.range(0.0D, 0.0D, 0.0D,
                new double[] {Double.NaN, 0.0D, 0.0D}));
        assertDoesNotThrow(() -> B2PodMath.insideLockWindow(new B2PodMath.Gimbal(Double.NaN, Double.NaN),
                new B2PodMath.Gimbal(0.0D, 0.0D), 5.0D));
        assertFalse(Double.isNaN(B2PodMath.fovFor(0.0D, FOV_MIN, FOV_MAX)), "the frozen band is finite");
        assertTrue(B2PodMath.lookVector(0.0D, 90.0D)[1] < 0.0D, "straight down still points down (-Y) after the NaN cases");
    }
}
