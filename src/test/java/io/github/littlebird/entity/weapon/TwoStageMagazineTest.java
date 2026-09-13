package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class TwoStageMagazineTest {
    private static final TwoStageMagazine.Spec ROCKETS = TwoStageMagazine.rocketBay();
    private static final TwoStageMagazine.Spec MISSILES = TwoStageMagazine.missileRail();

    private static TwoStageMagazine.State step(
            TwoStageMagazine.State state, TwoStageMagazine.Spec spec, int ticks) {
        TwoStageMagazine.State s = state;
        for (int i = 0; i < ticks; i++) {
            s = TwoStageMagazine.tick(s, spec);
        }
        return s;
    }

    private static TwoStageMagazine.State emptyReady(TwoStageMagazine.Spec spec) {
        TwoStageMagazine.State s = TwoStageMagazine.initial(spec);
        for (int i = 0; i < spec.readyCapacity(); i++) {
            s = TwoStageMagazine.fire(s, spec);
        }
        return s;
    }

    @Test
    void loadsMatchTheRequestedNumbers() {
        TwoStageMagazine.State rockets = TwoStageMagazine.initial(ROCKETS);
        assertEquals(19, rockets.ready());
        assertEquals(95, rockets.reserve(), "the decision was 19 / 95");
        assertEquals(114, rockets.total());

        TwoStageMagazine.State missiles = TwoStageMagazine.initial(MISSILES);
        assertEquals(1, missiles.ready());
        assertEquals(5, missiles.reserve(), "and 1 / 5 for the TOW");
    }

    @Test
    void firingDrainsTheReadyRoundsOnly() {
        TwoStageMagazine.State s = emptyReady(ROCKETS);
        assertEquals(0, s.ready());
        assertEquals(95, s.reserve(), "the reserve is untouched by firing");
        assertEquals(s, TwoStageMagazine.fire(s, ROCKETS), "an empty ready position cannot fire");
    }

    @Test
    void reloadMovesTheReserveIntoTheReadyPosition() {
        TwoStageMagazine.State s = emptyReady(ROCKETS);
        assertTrue(TwoStageMagazine.canReload(s, ROCKETS));
        s = TwoStageMagazine.startReload(s, ROCKETS);
        assertEquals(0, s.ready());
        s = step(s, ROCKETS, 39);
        assertEquals(0, s.ready(), "the reload takes the full two seconds");
        s = step(s, ROCKETS, 1);
        assertEquals(19, s.ready());
        assertEquals(76, s.reserve(), "95 - 19 moved up");
    }

    @Test
    void reloadIsRefusedWhenThereIsNothingToMove() {
        TwoStageMagazine.State full = TwoStageMagazine.initial(ROCKETS);
        assertFalse(TwoStageMagazine.canReload(full, ROCKETS), "ready is already full");
        assertEquals(full, TwoStageMagazine.startReload(full, ROCKETS));

        TwoStageMagazine.State nothing = new TwoStageMagazine.State(0, 0, 0, 0);
        assertFalse(TwoStageMagazine.canReload(nothing, ROCKETS), "nothing left in reserve");
        assertEquals(nothing, TwoStageMagazine.startReload(nothing, ROCKETS));
    }

    @Test
    void aRunningReloadIsNotRestarted() {
        TwoStageMagazine.State s = TwoStageMagazine.startReload(emptyReady(ROCKETS), ROCKETS);
        s = step(s, ROCKETS, 10);
        TwoStageMagazine.State again = TwoStageMagazine.startReload(s, ROCKETS);
        assertEquals(s, again, "the reload key does nothing while a reload is running");
        assertEquals(30, again.reloadTicks());
    }

    @Test
    void nothingRestocksItselfAnyMore() {
        // The ammunition crate is the only source now, so an empty reserve stays empty however
        // long the aircraft flies.
        TwoStageMagazine.State after = step(new TwoStageMagazine.State(0, 0, 0, 0), ROCKETS, 2000);
        assertEquals(0, after.reserve(), "no free resupply");
        assertEquals(0, after.resupplyTicks());
        assertEquals(10, step(new TwoStageMagazine.State(5, 10, 0, 0), MISSILES, 2000).reserve(),
                "and the same for the missile rail");
    }

    @Test
    void theCrateTopsTheReserveUpAndNeverOverfills() {
        TwoStageMagazine.State s = new TwoStageMagazine.State(0, 0, 0, 0);
        s = TwoStageMagazine.supply(s, ROCKETS, 10);
        assertEquals(10, s.reserve());
        assertEquals(0, s.ready(), "a crate fills the reserve, the reload key fills the tubes");
        s = TwoStageMagazine.supply(s, ROCKETS, 1000);
        assertEquals(95, s.reserve(), "capped at the reserve capacity");
        assertEquals(95, TwoStageMagazine.supply(s, ROCKETS, 5).reserve());
        assertEquals(95, TwoStageMagazine.supply(s, ROCKETS, -5).reserve());
    }

    @Test
    void theDefaultSpecsPinTheDesignedClocks() {
        assertEquals(19, ROCKETS.readyCapacity());
        assertEquals(95, ROCKETS.reserveCapacity());
        assertEquals(40, ROCKETS.reloadTicks(), "two seconds for a rocket bay");
        assertEquals(0, ROCKETS.resupplyTicks(), "no self resupply any more");
        assertEquals(1, MISSILES.readyCapacity());
        assertEquals(5, MISSILES.reserveCapacity());
        assertEquals(100, MISSILES.reloadTicks(), "five seconds for one TOW");
        assertEquals(0, MISSILES.resupplyTicks());
    }

    @Test
    void withTimingsKeepsTheLoadAndOnlyReplacesTheClocks() {
        TwoStageMagazine.Spec tuned = ROCKETS.withTimings(20, 0);
        assertEquals(ROCKETS.readyCapacity(), tuned.readyCapacity());
        assertEquals(ROCKETS.reserveCapacity(), tuned.reserveCapacity());
        assertEquals(20, tuned.reloadTicks());

        TwoStageMagazine.State s = TwoStageMagazine.startReload(emptyReady(tuned), tuned);
        s = step(s, tuned, 19);
        assertEquals(0, s.ready(), "one second is not enough at the tuned time");
        s = step(s, tuned, 1);
        assertEquals(19, s.ready());
    }

    @Test
    void aFullyLoadedMagazineNeedsNeitherTimer() {
        TwoStageMagazine.State s = step(TwoStageMagazine.initial(ROCKETS), ROCKETS, 500);
        assertEquals(19, s.ready());
        assertEquals(95, s.reserve());
        assertEquals(0, s.resupplyTicks(), "no resupply runs at all");
        assertNotEquals(0, TwoStageMagazine.initial(ROCKETS).total());
    }

    @Test
    void degenerateSpecsAreSafe() {
        TwoStageMagazine.Spec none = new TwoStageMagazine.Spec(0, 0, -5, -5, 0);
        TwoStageMagazine.State s = TwoStageMagazine.tick(TwoStageMagazine.initial(none), none);
        s = TwoStageMagazine.tick(s, none);
        assertEquals(0, s.total());
        assertFalse(TwoStageMagazine.canReload(s, none));
        assertEquals(0, TwoStageMagazine.supply(s, none, 50).total());
    }
}
