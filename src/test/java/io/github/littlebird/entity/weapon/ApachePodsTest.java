package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ApachePodsTest {

    @Test
    void theTwoPodsSplitTheReadyRounds() {
        assertEquals(ApachePods.READY_CAPACITY,
                ApachePods.podCapacity(0) + ApachePods.podCapacity(1));
        assertEquals(10, ApachePods.podCapacity(0));
        assertEquals(9, ApachePods.podCapacity(1));
    }

    @Test
    void podsAlternateAndAlwaysAddUp() {
        assertEquals(0, ApachePods.nextPod(ApachePods.READY_CAPACITY));
        assertEquals(1, ApachePods.nextPod(ApachePods.READY_CAPACITY - 1));
        assertEquals(0, ApachePods.nextPod(ApachePods.READY_CAPACITY - 2));

        for (int ready = 0; ready <= ApachePods.READY_CAPACITY; ready++) {
            int left = ApachePods.podReady(ready, 0);
            int right = ApachePods.podReady(ready, 1);
            assertTrue(left >= 0 && right >= 0, "no pod may go negative at ready=" + ready);
            assertEquals(ready, left + right, "pods must add up to the ready count");
        }
    }

    @Test
    void theFirstRoundComesFromTheLeftPod() {
        assertEquals(10, ApachePods.podReady(ApachePods.READY_CAPACITY, 0));
        assertEquals(9, ApachePods.podReady(ApachePods.READY_CAPACITY, 1));
        assertEquals(9, ApachePods.podReady(ApachePods.READY_CAPACITY - 1, 0));
        assertEquals(9, ApachePods.podReady(ApachePods.READY_CAPACITY - 1, 1));
        assertEquals(0, ApachePods.podReady(0, 0));
        assertEquals(0, ApachePods.podReady(0, 1));
    }

    @Test
    void theNextPodDecidesTheLaunchSideAndTheReticleUsesIt() {
        // The server launches the rocket from this offset and the client predicts the impact
        // from the same one, so the sight cannot drift a pod's width off the real rockets.
        double lateral = 1.68D;
        assertEquals(-lateral, ApachePods.launchLateralBlocks(ApachePods.READY_CAPACITY, lateral),
                1.0E-9D, "a full bay fires from the left wing first");
        assertEquals(lateral, ApachePods.launchLateralBlocks(ApachePods.READY_CAPACITY - 1, lateral),
                1.0E-9D);
        assertEquals(-lateral, ApachePods.launchLateralBlocks(ApachePods.READY_CAPACITY - 2, lateral),
                1.0E-9D);
        for (int ready = 0; ready <= ApachePods.READY_CAPACITY; ready++) {
            double side = ApachePods.launchLateralBlocks(ready, lateral);
            assertEquals(lateral, Math.abs(side), 1.0E-9D,
                    "the offset is always one pod out from the centre line: ready=" + ready);
            assertEquals(ApachePods.nextPod(ready) == 0 ? -lateral : lateral, side, 1.0E-9D);
        }
    }

    @Test
    void theReticleShowsTenTubesOnTheLeftWingAndNineOnTheRight() {
        assertEquals(10, ApachePods.tubeCount(0));
        assertEquals(9, ApachePods.tubeCount(1));
        for (int pod = 0; pod < ApachePods.PODS; pod++) {
            java.util.Set<String> seen = new java.util.HashSet<>();
            for (int tube = 0; tube < ApachePods.tubeCount(pod); tube++) {
                ApachePods.DotPosition dot = ApachePods.tubeDot(pod, tube);
                assertTrue(dot.column() >= 0 && dot.column() < ApachePods.HUD_COLUMNS,
                        "column out of the grid: " + dot);
                assertTrue(dot.row() >= 0 && dot.row() < ApachePods.HUD_ROWS,
                        "row out of the grid: " + dot);
                assertTrue(seen.add(dot.column() + ":" + dot.row()),
                        "two tubes would share one dot: " + dot);
            }
            assertEquals(ApachePods.tubeCount(pod), seen.size());
        }
    }

    @Test
    void theDotsAreTheRoundsThatPodStillHas() {
        for (int pod = 0; pod < ApachePods.PODS; pod++) {
            for (int ready = 0; ready <= ApachePods.READY_CAPACITY; ready++) {
                int lit = 0;
                for (int tube = 0; tube < ApachePods.tubeCount(pod); tube++) {
                    if (ApachePods.tubeLoaded(pod, tube, ready)) {
                        lit++;
                    }
                }
                assertEquals(ApachePods.podReady(ready, pod), lit,
                        "lit dots must equal that pod's rounds: pod=" + pod + " ready=" + ready);
            }
        }
    }

    @Test
    void theFirstTubeToFireOwnsTheLastDotToGoOut() {
        // Both pods empty towards the bottom inner cell, so a nearly empty sight still reads
        // the same way on either wing.
        ApachePods.DotPosition last = new ApachePods.DotPosition(1, 4);
        assertEquals(last, ApachePods.tubeDot(0, 0));
        assertEquals(last, ApachePods.tubeDot(1, 0));
        assertTrue(ApachePods.tubeLoaded(0, 0, 1), "one rocket left lights one dot");
        assertFalse(ApachePods.tubeLoaded(0, 1, 1));
        assertFalse(ApachePods.tubeLoaded(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> ApachePods.tubeDot(0, 10));
        assertThrows(IllegalArgumentException.class, () -> ApachePods.tubeDot(1, 9));
    }

    @Test
    void outOfRangeInputIsClamped() {
        // More rounds than the pods can hold reads as "nothing fired yet"; fewer than none
        // reads as "everything fired".
        assertEquals(0, ApachePods.podReady(-5, 0));
        assertEquals(ApachePods.podCapacity(0), ApachePods.podReady(999, 0));
        assertEquals(ApachePods.podCapacity(1), ApachePods.podReady(999, 1));
        assertEquals(1, ApachePods.nextPod(-5), "all fired means the right pod is next");
    }
}
