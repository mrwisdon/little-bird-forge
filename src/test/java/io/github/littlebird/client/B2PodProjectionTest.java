package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the sight projection: the middle of the glass, the aspect ratio and the edges. */
final class B2PodProjectionTest {
    private static final int W = 1920;
    private static final int H = 1080;

    @Test
    void straightAheadProjectsToTheCentre() {
        int[] p = B2PodMath.projectToScreen(0.0D, 0.0D, 70.0D, W, H);
        assertEquals(W / 2, p[0], 1);
        assertEquals(H / 2, p[1], 1);
    }

    @Test
    void horizontalIsWiderThanVerticalByTheAspectRatio() {
        int[] sideways = B2PodMath.projectToScreen(20.0D, 0.0D, 70.0D, W, H);
        int[] downwards = B2PodMath.projectToScreen(0.0D, 20.0D, 70.0D, W, H);
        double dx = Math.abs(sideways[0] - W / 2.0D) / (W / 2.0D);
        double dy = Math.abs(downwards[1] - H / 2.0D) / (H / 2.0D);
        assertTrue(dx < dy, "the same angle covers fewer pixels across than down: " + dx + " vs " + dy);
    }

    @Test
    void markersAreClampedIntoTheGlass() {
        int[] far = B2PodMath.projectToScreen(80.0D, -70.0D, 30.0D, W, H);
        assertTrue(far[0] >= 0 && far[0] < W, "x=" + far[0]);
        assertTrue(far[1] >= 0 && far[1] < H, "y=" + far[1]);
        assertTrue(B2PodMath.offScreen(80.0D, 0.0D, 30.0D));
        assertFalse(B2PodMath.offScreen(5.0D, -5.0D, 70.0D));
        assertEquals(0, B2PodMath.clampInt(-9, 0, 100));
        assertEquals(100, B2PodMath.clampInt(999, 0, 100));
    }

    @Test
    void angularOffsetFollowsTheMinecraftAxesAndSurvivesNull() {
        // Directly ahead: yaw 0 looks at +Z, so a point on +Z is dead centre.
        double[] ahead = B2PodMath.angularOffset(0.0D, 10.0D, 0.0D, 0.0D, 0.0D,
                new double[]{0.0D, 10.0D, 100.0D});
        assertEquals(0.0D, ahead[0], 1.0E-6D);
        assertEquals(0.0D, ahead[1], 1.0E-6D);
        // Down and to the east of a south facing sight.
        double[] below = B2PodMath.angularOffset(0.0D, 40.0D, 0.0D, 0.0D, 0.0D,
                new double[]{0.0D, 0.0D, 40.0D});
        assertEquals(45.0D, below[1], 1.0E-6D, "pitch is positive downwards");
        double[] east = B2PodMath.angularOffset(0.0D, 40.0D, 0.0D, 0.0D, 0.0D,
                new double[]{40.0D, 40.0D, 0.0D});
        assertEquals(-90.0D, east[0], 1.0E-6D, "east of a south facing sight is 90 degrees left");
        assertNull(B2PodMath.angularOffset(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, null));
    }
}
