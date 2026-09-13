package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Ground vehicle camera FOV selection, and its isolation from the aircraft camera. */
class GroundViewMathTest {
    @Test
    void aircraftKeepTheirOwnCamera() {
        assertEquals(1.0D, GroundViewMath.fovMultiplier(false, false, 1.35D, 1.0D), 1.0E-9D);
        assertEquals(1.0D, GroundViewMath.fovMultiplier(false, true, 1.35D, 1.1D), 1.0E-9D);
    }

    @Test
    void groundExteriorGetsTheGroundMultiplier() {
        assertEquals(1.35D, GroundViewMath.fovMultiplier(true, false, 1.35D, 1.1D), 1.0E-9D);
    }

    @Test
    void turretInteriorUsesItsOwnMultiplier() {
        assertEquals(1.1D, GroundViewMath.fovMultiplier(true, true, 1.35D, 1.1D), 1.0E-9D);
    }

    @Test
    void nonsenseConfigFallsBackToVanilla() {
        assertEquals(1.0D, GroundViewMath.fovMultiplier(true, false, 0.0D, 1.0D), 1.0E-9D);
        assertEquals(1.0D, GroundViewMath.fovMultiplier(true, false, Double.NaN, 1.0D), 1.0E-9D);
        assertEquals(1.0D, GroundViewMath.fovMultiplier(true, true, 1.35D, -2.0D), 1.0E-9D);
    }

    @Test
    void interiorCameraFollowsTheTurretYaw() {
        assertEquals(37.0F, GroundViewMath.followYawDeg(true, 10.0F, 37.0D), 1.0E-4F);
        // A wrapped turret yaw must be taken as-is: no unwrapping, no sign flip.
        assertEquals(-179.0F, GroundViewMath.followYawDeg(true, 10.0F, -179.0D), 1.0E-4F);
    }

    @Test
    void everyOtherCameraKeepsTheHullYaw() {
        assertEquals(10.0F, GroundViewMath.followYawDeg(false, 10.0F, 37.0D), 1.0E-4F);
        // A bad synched value must not pitch the exterior camera onto the turret.
        assertEquals(10.0F, GroundViewMath.followYawDeg(true, 10.0F, Double.NaN), 1.0E-4F);
        assertEquals(10.0F, GroundViewMath.followYawDeg(true, 10.0F,
                Double.POSITIVE_INFINITY), 1.0E-4F);
    }
}
