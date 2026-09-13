package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Vehicle smoke discharger: charges, cooldown, cloud duration and crate refills. */
class SmokeLauncherTest {
    private static final SmokeLauncher.Spec SPEC = new SmokeLauncher.Spec(2, 300, 160);

    @Test
    void startsLoadedAndReady() {
        SmokeLauncher.State state = SmokeLauncher.initial(SPEC);
        assertEquals(2, state.charges());
        assertTrue(state.canFire());
        assertFalse(state.cloudActive());
    }

    @Test
    void firingConsumesAChargeAndStartsBothClocks() {
        SmokeLauncher.State state = SmokeLauncher.fire(SmokeLauncher.initial(SPEC), SPEC);
        assertEquals(1, state.charges());
        assertEquals(300, state.cooldownTicks());
        assertEquals(160, state.activeTicks());
        assertFalse(state.canFire(), "still cooling down");
        assertTrue(state.cloudActive());
    }

    @Test
    void cooldownAndCloudRunDownIndependently() {
        // A cloud that outlives its cooldown: the launcher is ready again while the smoke lingers.
        SmokeLauncher.Spec spec = new SmokeLauncher.Spec(2, 300, 400);
        SmokeLauncher.State state = SmokeLauncher.fire(SmokeLauncher.initial(spec), spec);
        for (int i = 0; i < 300; i++) {
            state = SmokeLauncher.tick(state, spec);
        }
        assertEquals(0, state.cooldownTicks());
        assertTrue(state.canFire(), "ready again after the cooldown");
        assertTrue(state.cloudActive(), "the cloud is still up");
        for (int i = 0; i < 100; i++) {
            state = SmokeLauncher.tick(state, spec);
        }
        assertFalse(state.cloudActive(), "the cloud finally disperses");

        // And the shipped spec: a shorter cloud is gone once the cooldown ends.
        SmokeLauncher.State shipped = SmokeLauncher.fire(SmokeLauncher.initial(SPEC), SPEC);
        for (int i = 0; i < 300; i++) {
            shipped = SmokeLauncher.tick(shipped, SPEC);
        }
        assertFalse(shipped.cloudActive(), "a 160 tick cloud is long gone after 300 ticks");
    }

    @Test
    void outOfChargesRefusesToFire() {
        SmokeLauncher.Spec one = new SmokeLauncher.Spec(1, 0, 100);
        SmokeLauncher.State state = SmokeLauncher.fire(SmokeLauncher.initial(one), one);
        assertEquals(0, state.charges());
        state = SmokeLauncher.fire(state, one);
        assertEquals(0, state.charges(), "an empty launcher stays empty");
        assertFalse(state.canFire());
    }

    @Test
    void crateRefillStopsAtCapacity() {
        SmokeLauncher.Spec one = new SmokeLauncher.Spec(2, 0, 100);
        SmokeLauncher.State state = SmokeLauncher.fire(SmokeLauncher.initial(one), one);
        state = SmokeLauncher.supply(state, one, 5);
        assertEquals(2, state.charges(), "never past capacity");
    }

    @Test
    void fractionsAreReadyForTheHud() {
        SmokeLauncher.State full = SmokeLauncher.initial(SPEC);
        assertEquals(1.0F, SmokeLauncher.chargeFraction(full, SPEC), 1.0E-6F);
        assertEquals(1.0F, SmokeLauncher.cooldownFraction(full, SPEC), 1.0E-6F);
        SmokeLauncher.State fired = SmokeLauncher.fire(full, SPEC);
        assertEquals(0.5F, SmokeLauncher.chargeFraction(fired, SPEC), 1.0E-6F);
        assertEquals(0.0F, SmokeLauncher.cooldownFraction(fired, SPEC), 1.0E-6F);
    }
}
