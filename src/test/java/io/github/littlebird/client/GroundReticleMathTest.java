package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Off-axis coax reticle geometry. */
class GroundReticleMathTest {
    @Test
    void bearingWrapsAroundNorth() {
        assertEquals(0.0D, GroundReticleMath.relativeBearingDeg(0.0D, 0.0D), 1.0E-9D);
        assertEquals(20.0D, GroundReticleMath.relativeBearingDeg(10.0D, 350.0D), 1.0E-9D);
        assertEquals(-20.0D, GroundReticleMath.relativeBearingDeg(350.0D, 10.0D), 1.0E-9D);
        assertEquals(180.0D, Math.abs(GroundReticleMath.relativeBearingDeg(0.0D, 180.0D)), 1.0E-9D);
    }

    @Test
    void fovTestKeepsTheEdgeOnScreen() {
        assertTrue(GroundReticleMath.withinFov(0.0D, 70.0D));
        assertTrue(GroundReticleMath.withinFov(35.0D, 70.0D), "exactly at the edge is still visible");
        assertFalse(GroundReticleMath.withinFov(35.1D, 70.0D));
        assertTrue(GroundReticleMath.withinFov(-34.0D, 70.0D));
    }

    @Test
    void gapGrowsWithSpreadAndClamps() {
        assertEquals(3, GroundReticleMath.gapPixels(0.0D, 3, 11));
        assertEquals(11, GroundReticleMath.gapPixels(1.0D, 3, 11));
        assertEquals(7, GroundReticleMath.gapPixels(0.5D, 3, 11));
        assertEquals(3, GroundReticleMath.gapPixels(-5.0D, 3, 11));
        assertEquals(11, GroundReticleMath.gapPixels(5.0D, 3, 11));
    }

    @Test
    void horizontalFovFollowsTheAspectRatio() {
        double square = GroundReticleMath.horizontalFovDeg(70.0D, 100, 100);
        assertEquals(70.0D, square, 1.0E-6D, "a square viewport has the same h/v fov");
        double wide = GroundReticleMath.horizontalFovDeg(70.0D, 200, 100);
        assertTrue(wide > square, "a wider viewport opens the horizontal fov");
    }
}
