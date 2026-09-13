package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class LittleBirdPodTubesTest {

    @Test
    void centreTubeSitsOnTheAxisAndTheRingWrapsAround() {
        assertEquals(0.0F, LittleBirdPodTubes.offsetX(0), 1.0E-5F);
        assertEquals(0.0F, LittleBirdPodTubes.offsetY(0), 1.0E-5F);
        for (int tube = 1; tube < LittleBirdPodTubes.TUBES; tube++) {
            double radius = Math.hypot(LittleBirdPodTubes.offsetX(tube),
                    LittleBirdPodTubes.offsetY(tube));
            assertEquals(LittleBirdPodTubes.RING_RADIUS, radius, 1.0E-4D,
                    "tube " + tube + " must sit on the ring");
        }
    }

    @Test
    void loadedFollowsTheRemainingRounds() {
        for (int tube = 0; tube < LittleBirdPodTubes.TUBES; tube++) {
            assertTrue(LittleBirdPodTubes.loaded(tube, 7));
        }
        assertTrue(LittleBirdPodTubes.loaded(0, 1));
        assertFalse(LittleBirdPodTubes.loaded(1, 1));
        assertFalse(LittleBirdPodTubes.loaded(0, 0));
        assertFalse(LittleBirdPodTubes.loaded(-1, 7));
        assertFalse(LittleBirdPodTubes.loaded(9, 7));
    }
}