package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AmmoPoolTest {

    @Test
    void aFreshPoolHoldsAFullBelt() {
        AmmoPool belt = AmmoPool.full(2000);
        assertEquals(2000, belt.rounds());
        assertEquals(1.0F, belt.fraction(), 1.0E-6F);
        assertFalse(belt.isEmpty());
        assertEquals(600, AmmoPool.full(600).rounds());
    }

    @Test
    void firingDrainsOneRoundAtATimeAndStopsAtEmpty() {
        AmmoPool belt = AmmoPool.full(3);
        assertTrue(belt.fire(1));
        assertTrue(belt.fire(1));
        assertEquals(1, belt.rounds());
        assertTrue(belt.fire(1));
        assertFalse(belt.fire(1), "an empty belt refuses the shot");
        assertTrue(belt.isEmpty());
        assertFalse(belt.fire(1));
    }

    @Test
    void aPoolRefillsOnlyUpToItsCapacity() {
        AmmoPool belt = AmmoPool.empty(600);
        assertEquals(300, belt.refill(300));
        assertEquals(300, belt.rounds());
        assertEquals(300, belt.refill(10000), "returns what it actually took");
        assertEquals(600, belt.rounds());
        assertEquals(0, belt.refill(50));
    }

    @Test
    void aShotCostingMoreThanOneRoundIsRefusedWhole() {
        AmmoPool pool = AmmoPool.full(10);
        assertTrue(pool.fire(4));
        assertEquals(6, pool.rounds());
        assertEquals(6, pool.rounds());
        assertFalse(pool.fire(7), "the cost is paid in full or not at all");
        assertEquals(6, pool.rounds());
    }

    @Test
    void aCrateHandsOutTheConfiguredRateOverTime() {
        AmmoSupply supply = new AmmoSupply(40, 2, 1);
        assertEquals(0, supply.bullets(0));
        assertEquals(2, supply.bullets(1));
        assertEquals(40, supply.bullets(20), "forty rounds a second is forty rounds a second");
        assertEquals(80, supply.bullets(40));
        assertEquals(2, supply.rockets(20));
        assertEquals(1, supply.missiles(20));
        assertEquals(0, new AmmoSupply(0, 0, 0).rockets(200));
        assertTrue(AmmoSupply.SUPPLY_RADIUS_BLOCKS >= 4.0D,
                "close enough to be usable, far enough to not be glued to the crate");
    }
}
