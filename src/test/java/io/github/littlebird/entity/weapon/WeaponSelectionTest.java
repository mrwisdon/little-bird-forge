package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Weapon selection for the ground vehicles: driver carousel and fixed single-weapon seats. */
class WeaponSelectionTest {
    @Test
    void tankDriverStartsOnTheMainGun() {
        assertEquals(WeaponSelection.MAIN_GUN, WeaponSelection.forTankDriver().selected());
    }

    @Test
    void driverCarouselWrapsBothWays() {
        WeaponSelection selection = WeaponSelection.forTankDriver();
        assertEquals(WeaponSelection.COAX, selection.cycle(1), "the coax sits next to the main gun");
        assertEquals(WeaponSelection.SMOKE, selection.cycle(1));
        assertEquals(WeaponSelection.MAIN_GUN, selection.cycle(1), "one step past the end wraps");
        assertEquals(WeaponSelection.SMOKE, selection.cycle(-1), "backwards wraps too");
    }

    @Test
    void driverCanSelectTheSharedCoax() {
        WeaponSelection selection = WeaponSelection.forTankDriver();
        assertTrue(selection.isAllowed(WeaponSelection.COAX));
        assertTrue(selection.select(WeaponSelection.COAX));
        assertEquals(WeaponSelection.COAX, selection.selected());
        assertFalse(selection.isAllowed(4), "an unknown slot is still refused");
    }

    @Test
    void selectingTheCurrentSlotReportsNoChange() {
        WeaponSelection selection = WeaponSelection.forTankDriver();
        assertFalse(selection.select(WeaponSelection.MAIN_GUN));
        assertTrue(selection.select(WeaponSelection.SMOKE));
        assertFalse(selection.select(WeaponSelection.SMOKE));
        assertTrue(selection.is(WeaponSelection.SMOKE));
    }

    @Test
    void aFixedSeatHasNothingToCycle() {
        WeaponSelection gunner = WeaponSelection.fixed(WeaponSelection.COAX);
        assertEquals(WeaponSelection.COAX, gunner.selected());
        assertTrue(gunner.isAllowed(WeaponSelection.COAX));
        assertFalse(gunner.isAllowed(WeaponSelection.MAIN_GUN));
        assertEquals(WeaponSelection.COAX, gunner.cycle(1));
        assertEquals(WeaponSelection.COAX, gunner.cycle(-1));
    }

    @Test
    void slotsAreClamped() {
        assertEquals(0, WeaponSelection.clampSlot(-4));
        assertEquals(WeaponSelection.SMOKE, WeaponSelection.clampSlot(WeaponSelection.SMOKE));
        assertEquals(WeaponSelection.SMOKE, WeaponSelection.clampSlot(99));
    }
}
