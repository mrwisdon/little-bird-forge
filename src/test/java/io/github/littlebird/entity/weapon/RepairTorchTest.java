package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the repair torch rate, clamp and reach. */
final class RepairTorchTest {
    private static final float EPSILON = 1.0E-5F;

    @Test
    void oneTickWeldsAFifteenthOfSecondOfHealth() {
        assertEquals(0.75F, RepairTorch.HEALTH_PER_TICK, EPSILON);
        assertEquals(15.0F, RepairTorch.HEALTH_PER_TICK * 20.0F, EPSILON);
    }

    @Test
    void aWeldNeverOvershootsTheHull() {
        assertEquals(80.0F, RepairTorch.repaired(79.5F, 80.0F, 0.75F), EPSILON);
        assertEquals(80.0F, RepairTorch.repaired(80.0F, 80.0F, 0.75F), EPSILON);
        assertEquals(0.75F, RepairTorch.repaired(0.0F, 80.0F, 0.75F), EPSILON);
    }

    @Test
    void nonsenseAmountsLeaveTheHullAlone() {
        assertEquals(12.0F, RepairTorch.repaired(12.0F, 80.0F, 0.0F), EPSILON);
        assertEquals(12.0F, RepairTorch.repaired(12.0F, 80.0F, -3.0F), EPSILON);
        assertEquals(12.0F, RepairTorch.repaired(12.0F, 0.0F, 0.75F), EPSILON);
    }

    @Test
    void onlyADamagedHullNeedsWork() {
        assertTrue(RepairTorch.needsRepair(79.0F, 80.0F));
        assertFalse(RepairTorch.needsRepair(80.0F, 80.0F));
        assertFalse(RepairTorch.needsRepair(0.0F, 0.0F));
    }

    @Test
    void reachStopsAtFourAndAHalfBlocks() {
        assertTrue(RepairTorch.inRange(0.0D));
        assertTrue(RepairTorch.inRange(4.5D * 4.5D));
        assertFalse(RepairTorch.inRange(4.5D * 4.5D + 0.01D));
        assertFalse(RepairTorch.inRange(-1.0D));
    }
}
