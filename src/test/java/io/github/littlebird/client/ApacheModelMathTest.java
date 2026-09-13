package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ApacheModelMathTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void theRadarSweepsAndWraps() {
        assertEquals(0.0D, ApacheModelMath.radarAngleRadians(0.0F), EPSILON);
        double quarter = ApacheModelMath.radarAngleRadians(
                30.0F); // 30 ticks * 3 deg = 90 degrees
        assertEquals(Math.PI / 2.0D, quarter, 1.0E-5D);
        double wrapped = ApacheModelMath.radarAngleRadians(240.0F); // two full turns
        assertEquals(0.0D, wrapped, 1.0E-5D, "the sweep wraps rather than growing forever");
    }

    @Test
    void theTurretTurnsTheWayTheModelIsMirrored() {
        assertTrue(ApacheModelMath.turretYawRadians(30.0D, 0.0F) < 0.0D,
                "a right hand bearing is a negative model yaw");
        assertEquals(-Math.PI / 2.0D, ApacheModelMath.turretYawRadians(90.0D, 0.0F), 1.0E-5D);
        assertEquals(0.0D, ApacheModelMath.turretYawRadians(Double.NaN, 0.0F), EPSILON);
    }

    @Test
    void theTurretSwingIsMeasuredAgainstTheAirframe() {
        // The barrel's own attitude is absolute, so the mesh swing is the difference: the gun
        // stays on its target while the airframe turns underneath it.
        assertEquals(40.0D, ApacheModelMath.relativeBearing(120.0D, 80.0F), EPSILON);
        assertEquals(-40.0D, ApacheModelMath.relativeBearing(80.0D, 120.0F), EPSILON);
        assertEquals(0.0D, ApacheModelMath.relativeBearing(70.0D, 70.0F), EPSILON,
                "a barrel parked on the nose needs no swing at all");
    }

    @Test
    void aRelativeAngleNeverTakesTheLongWayRound() {
        assertEquals(-20.0D, ApacheModelMath.relativeBearing(170.0D, 190.0F), EPSILON);
        assertEquals(20.0D, ApacheModelMath.relativeBearing(190.0D, 170.0F), EPSILON);
        assertEquals(0.0D, ApacheModelMath.relativeElevation(Double.NaN, 0.0F), EPSILON);
    }

    @Test
    void elevationUpLiftsTheBarrel() {
        assertTrue(ApacheModelMath.turretPitchRadians(-11.0D, 0.0F) > 0.0D,
                "aiming up is negative Minecraft pitch and lifts the -Z barrel");
        assertTrue(ApacheModelMath.turretPitchRadians(60.0D, 0.0F) < 0.0D);
        assertEquals(0.0D, ApacheModelMath.turretPitchRadians(Double.NaN, 0.0F), EPSILON);
    }

    @Test
    void theAirframePitchDoesNotMoveTheBarrel() {
        // Same absolute aim, two very different airframe attitudes: the gun is unchanged, only
        // the local swing the mesh has to show is.
        double level = ApacheModelMath.relativeElevation(-20.0D, 0.0F);
        double nosed = ApacheModelMath.relativeElevation(-20.0D, 45.0F);
        assertEquals(20.0D, level, EPSILON);
        assertEquals(65.0D, nosed, EPSILON);
    }

    @Test
    void tubesEmptyInFiringOrder() {
        assertTrue(ApacheModelMath.tubeLoaded(0, 1));
        assertFalse(ApacheModelMath.tubeLoaded(1, 1));
        assertFalse(ApacheModelMath.tubeLoaded(0, 0));
        assertFalse(ApacheModelMath.tubeLoaded(3, -2), "a negative load shows nothing");
    }
}
