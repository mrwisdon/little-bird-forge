package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The 120mm carousel: per-type racks, switching, reload clock and crate supply. */
class MainGunAmmoTest {
    private static final TwoStageMagazine.Spec SPEC = new TwoStageMagazine.Spec(5, 15, 20, 0, 1);

    @Test
    void startsFullyLoadedOnTheFirstType() {
        MainGunAmmo ammo = MainGunAmmo.initial(SPEC);
        assertEquals(MainGunAmmoType.AP, ammo.selected());
        assertEquals(5, ammo.ready());
        assertEquals(5, ammo.ready(MainGunAmmoType.HE), "every type starts loaded");
        assertEquals(15, ammo.reserve(MainGunAmmoType.HE));
    }

    @Test
    void firingDrainsOnlyTheSelectedType() {
        MainGunAmmo ammo = MainGunAmmo.initial(SPEC);
        assertTrue(ammo.fire());
        assertEquals(4, ammo.ready(MainGunAmmoType.AP));
        assertEquals(5, ammo.ready(MainGunAmmoType.HE), "HE must be untouched");
        ammo.cycleType();
        assertTrue(ammo.fire());
        assertEquals(4, ammo.ready(MainGunAmmoType.HE));
        assertEquals(4, ammo.ready(MainGunAmmoType.AP));
    }

    @Test
    void emptyRackRefusesToFire() {
        MainGunAmmo ammo = MainGunAmmo.initial(new TwoStageMagazine.Spec(1, 0, 10, 0, 1));
        assertTrue(ammo.fire());
        assertFalse(ammo.fire(), "an empty ready rack refuses");
        assertEquals(0, ammo.ready());
    }

    @Test
    void reloadMovesReserveRoundsWhenTheClockRunsOut() {
        MainGunAmmo ammo = MainGunAmmo.initial(SPEC);
        ammo.fire();
        assertTrue(ammo.startReload());
        for (int i = 0; i < SPEC.reloadTicks(); i++) {
            ammo.tick();
        }
        assertEquals(5, ammo.ready(), "the rack is topped back up");
        assertEquals(14, ammo.reserve(), "exactly one round moved");
    }

    @Test
    void switchingTypesIsFreeAndWraps() {
        MainGunAmmo ammo = MainGunAmmo.initial(SPEC);
        ammo.fire();
        assertEquals(MainGunAmmoType.HE, ammo.cycleType());
        assertEquals(5, ammo.ready(), "the fresh type has its own full rack");
        assertEquals(MainGunAmmoType.AP, ammo.cycleType(), "carousel wraps");
    }

    @Test
    void supplyFillsOnlyTheSelectedReserve() {
        MainGunAmmo ammo = MainGunAmmo.initial(SPEC);
        ammo.select(MainGunAmmoType.HE);
        ammo.supply(4);
        assertEquals(15, ammo.reserve(MainGunAmmoType.HE), "already full, no overflow");
        ammo.fire();
        ammo.supply(0);
        assertEquals(15, ammo.reserve(MainGunAmmoType.HE));
    }

    @Test
    void theResupplyCooldownDripsRoundsBack() {
        // Exactly the clock the tank's main gun now carries: one reserve round every 100 ticks.
        TwoStageMagazine.Spec spec = new TwoStageMagazine.Spec(5, 15, 60, 100, 1);
        MainGunAmmo ammo = MainGunAmmo.initial(spec);
        ammo.fire();
        ammo.startReload();
        for (int i = 0; i < spec.reloadTicks(); i++) {
            ammo.tick();
        }
        int reserveAfterReload = ammo.reserve();
        // The resupply clock starts on the tick the reserve drops below capacity, so one tick of
        // the cooldown is already spent by the time the reload finishes.
        int ticksLeft = ammo.state().resupplyTicks();
        assertTrue(ticksLeft > 0, "the resupply clock is running");
        for (int i = 0; i < ticksLeft - 1; i++) {
            ammo.tick();
        }
        assertEquals(reserveAfterReload, ammo.reserve(), "no round before the cooldown is up");
        ammo.tick();
        assertEquals(spec.reserveCapacity(), ammo.reserve(), "one round came back");
        // One more tick: with the rack full again the clock parks at zero instead of counting.
        ammo.tick();
        assertEquals(0, ammo.state().resupplyTicks(), "the clock idles once the rack is full again");
    }

    @Test
    void aZeroResupplyCooldownNeverRefills() {
        MainGunAmmo ammo = MainGunAmmo.initial(SPEC);
        ammo.fire();
        for (int i = 0; i < 500; i++) {
            ammo.tick();
        }
        assertEquals(15, ammo.reserve(ammo.selected()), "the shipped spec has no self resupply");
    }

    @Test
    void rebuildFromSavedRacksToleratesShortInput() {
        MainGunAmmo ammo = MainGunAmmo.of(SPEC, MainGunAmmoType.HE,
                new TwoStageMagazine.State[]{new TwoStageMagazine.State(2, 3, 0, 0)});
        assertEquals(MainGunAmmoType.HE, ammo.selected());
        assertEquals(2, ammo.ready(MainGunAmmoType.AP), "saved rack kept");
        assertEquals(5, ammo.ready(MainGunAmmoType.HE), "missing rack defaults to full");
    }
}
