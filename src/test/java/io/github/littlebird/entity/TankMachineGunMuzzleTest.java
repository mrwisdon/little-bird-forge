package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** The two turret machine guns must not share a muzzle, or both seats' tracers leave one hole. */
final class TankMachineGunMuzzleTest {
    @Test
    void theCoaxAndCupolaMuzzlesSitApart() {
        MainBattleTankGeometry.MachineGunMuzzle coax =
                MainBattleTankGeometry.machineGunMuzzle(false);
        MainBattleTankGeometry.MachineGunMuzzle cupola =
                MainBattleTankGeometry.machineGunMuzzle(true);
        // Tolerance 1e-6: the constants are floats, so e.g. -9.6F/16 differs from -9.6/16 by ~2e-8.
        assertEquals(-16.96D / 16.0D, coax.lateralBlocks(), 1.0E-6D);
        assertEquals(18.25D / 16.0D, coax.heightBlocks(), 1.0E-6D);
        assertEquals(12.32D / 16.0D, coax.barrelBlocks(), 1.0E-6D);
        assertEquals(13.6D / 16.0D, cupola.lateralBlocks(), 1.0E-6D);
        assertEquals(20.64D / 16.0D, cupola.heightBlocks(), 1.0E-6D);
        assertEquals(15.2D / 16.0D, cupola.barrelBlocks(), 1.0E-6D);
        assertNotEquals(coax, cupola);
    }
}
