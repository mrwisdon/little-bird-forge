package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.RocketPodState.Snapshot;
import io.github.littlebird.entity.weapon.RocketPodState.Step;
import org.junit.jupiter.api.Test;

final class RocketPodStateTest {

    @Test
    void sevenReadyRoundsAreSplitAcrossTheTwoPods() {
        // The ready magazine holds seven rounds in TOTAL, not seven a pod: four tubes are loaded
        // on the left, three on the right, and the other seven tubes stay empty.
        Snapshot start = Snapshot.initial();
        assertEquals(RocketPodState.READY_CAPACITY, start.totalRemaining());
        assertEquals(4, start.leftRemaining());
        assertEquals(3, start.rightRemaining());
        assertEquals(42, RocketPodState.RESERVE_CAPACITY, "the crate load");
    }

    @Test
    void splitReadyIsCappedAndNeverNegative() {
        assertEquals(new RocketPodState.Ready(0, 0), RocketPodState.splitReady(0));
        assertEquals(new RocketPodState.Ready(1, 0), RocketPodState.splitReady(1));
        assertEquals(new RocketPodState.Ready(3, 2), RocketPodState.splitReady(5));
        assertEquals(new RocketPodState.Ready(4, 3), RocketPodState.splitReady(7));
        assertEquals(new RocketPodState.Ready(4, 3), RocketPodState.splitReady(99),
                "never more than the magazine holds");
        assertEquals(new RocketPodState.Ready(0, 0), RocketPodState.splitReady(-4));
    }

    @Test
    void singleModeFiresOneRocketPerPressAlternatingPods() {
        Snapshot state = Snapshot.initial();
        Step first = RocketPodState.advance(state, true, false);
        assertTrue(first.launch());
        assertEquals(0, first.pod(), "the left pod has more loaded, so it fires first");
        assertEquals(3, first.tube(), "tube 7 - 4 remaining");
        assertEquals(6, first.snapshot().totalRemaining());

        Step held = RocketPodState.advance(first.snapshot(), true, false);
        assertFalse(held.launch(), "single mode only fires on the rising edge");

        Step released = RocketPodState.advance(held.snapshot(), false, false);
        Step third = RocketPodState.advance(released.snapshot(), true, false);
        assertTrue(third.launch());
        assertEquals(1, third.pod(), "the pods alternate");
        assertEquals(4, third.tube(), "tube 7 - 3 remaining");
    }

    @Test
    void salvoModeFiresOnAFixedCadenceAndStopsWhenEmpty() {
        Snapshot state = Snapshot.initial();
        int launched = 0;
        for (int tick = 0; tick < 200; tick++) {
            Step step = RocketPodState.advance(state, true, true);
            state = step.snapshot();
            if (step.launch()) {
                launched++;
            }
        }
        assertEquals(RocketPodState.READY_CAPACITY, launched, "the magazine empties once");
        assertEquals(0, state.totalRemaining());
        assertFalse(RocketPodState.advance(state, true, true).launch());
    }

    @Test
    void aRearmFillsTheEmptierPodFirstAndNeverExceedsTheMagazine() {
        Snapshot topped = RocketPodState.rearm(RocketPodState.empty(),
                RocketPodState.READY_CAPACITY);
        assertEquals(4, topped.leftRemaining());
        assertEquals(3, topped.rightRemaining());

        Snapshot overfilled = RocketPodState.rearm(topped, 50);
        assertEquals(RocketPodState.READY_CAPACITY, overfilled.totalRemaining(),
                "a crate load cannot load more tubes than the magazine holds");
        assertTrue(overfilled.leftRemaining() <= RocketPodState.TUBES_PER_POD);
        assertTrue(overfilled.rightRemaining() <= RocketPodState.TUBES_PER_POD);

        Snapshot untouched = RocketPodState.rearm(topped, 0);
        assertEquals(topped.totalRemaining(), untouched.totalRemaining());
        assertEquals(topped.totalRemaining(),
                RocketPodState.rearm(topped, -5).totalRemaining());
    }

    @Test
    void aPartialRearmKeepsThePodsBalanced() {
        Snapshot one = RocketPodState.rearm(RocketPodState.empty(), 1);
        assertEquals(1, one.leftRemaining());
        assertEquals(0, one.rightRemaining());
        Snapshot two = RocketPodState.rearm(one, 1);
        assertEquals(1, two.leftRemaining());
        assertEquals(1, two.rightRemaining());
    }

    @Test
    void aNullSnapshotStartsFromTheShippedLoad() {
        Step step = RocketPodState.advance(null, true, false);
        assertTrue(step.launch());
        assertEquals(RocketPodState.READY_CAPACITY - 1, step.snapshot().totalRemaining());
    }
}
