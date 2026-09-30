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

    @Test
    void theReloadClockStartsOnceAndCannotBeRestarted() {
        RocketPodState.Reload idle = RocketPodState.Reload.idle();
        assertFalse(idle.running());
        RocketPodState.Reload started = RocketPodState.begin(idle, 40, 5, 20);
        assertTrue(started.running());
        assertEquals(40, started.ticksLeft());
        // Hammering the reload key must not restart or shorten the clock.
        RocketPodState.Reload again = RocketPodState.begin(started, 40, 5, 20);
        assertEquals(40, again.ticksLeft());
        assertEquals(started, RocketPodState.begin(started, 99, 5, 20),
                "a running clock is handed back untouched, whatever the config says");
        // Nothing missing, or an empty reserve: no clock at all.
        assertFalse(RocketPodState.begin(idle, 40, 0, 20).running());
        assertFalse(RocketPodState.begin(idle, 40, -2, 20).running());
        assertFalse(RocketPodState.begin(idle, 40, 5, 0).running());
        // The config can still ask for the old instant reload.
        assertFalse(RocketPodState.begin(idle, 0, 5, 20).running());
    }

    @Test
    void theRoundsLandOnTheTickTheClockReachesZero() {
        RocketPodState.Reload reload = new RocketPodState.Reload(2);
        RocketPodState.Reload first = RocketPodState.tick(reload);
        assertEquals(1, first.ticksLeft());
        assertFalse(RocketPodState.completes(reload, first));
        RocketPodState.Reload second = RocketPodState.tick(first);
        assertEquals(0, second.ticksLeft());
        assertTrue(RocketPodState.completes(first, second));
        // Idle stays idle and the clock never runs negative, so it cannot complete twice.
        RocketPodState.Reload third = RocketPodState.tick(second);
        assertEquals(0, third.ticksLeft());
        assertFalse(RocketPodState.completes(second, third));
        assertFalse(RocketPodState.completes(RocketPodState.Reload.idle(),
                RocketPodState.Reload.idle()));
        assertFalse(RocketPodState.completes(null, RocketPodState.Reload.idle()));
    }

    @Test
    void progressAndTransferAmountsAreClamped() {
        assertEquals(0.0F, new RocketPodState.Reload(40).fraction(40), 1.0E-6F);
        assertEquals(0.5F, new RocketPodState.Reload(20).fraction(40), 1.0E-6F);
        assertEquals(1.0F, RocketPodState.Reload.idle().fraction(40), 1.0E-6F);
        assertEquals(0.0F, RocketPodState.Reload.idle().fraction(0), 1.0E-6F);
        assertEquals(1.0F, new RocketPodState.Reload(-3).fraction(40), 1.0E-6F,
                "a clock that has already run out reads as finished");
        // Only what the pods are missing, and only what the reserve actually holds.
        assertEquals(3, RocketPodState.transferAmount(4, 42));
        // The pods never take more than they are missing, however deep the reserve is.
        assertEquals(RocketPodState.READY_CAPACITY, RocketPodState.transferAmount(0, 10));
        assertEquals(RocketPodState.READY_CAPACITY, RocketPodState.transferAmount(0, 42));
        assertEquals(0, RocketPodState.transferAmount(RocketPodState.READY_CAPACITY, 42));
        assertEquals(RocketPodState.READY_CAPACITY, RocketPodState.transferAmount(-5, 42),
                "a nonsense negative count reads as empty pods, which then take a full load");
        assertEquals(0, RocketPodState.transferAmount(0, -1));
    }
}
