package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The JDAM's crater: how deep, how wide, and what it refuses to break. */
class CraterShapeTest {

    @Test
    void theImpactBlockIsAlwaysCracked() {
        assertTrue(CraterShape.isCracked(0, 0, 0, 9.0D));
        assertTrue(CraterShape.isCracked(0, 0, 0, 0.5D));
        assertTrue(CraterShape.isCracked(0, 0, 0, 0.01D));
    }

    @Test
    void theBowlIsWiderThanItIsDeep() {
        double radius = 9.0D;
        assertTrue(CraterShape.isCracked(8, 0, 0, radius), "8 blocks sideways is inside");
        assertFalse(CraterShape.isCracked(10, 0, 0, radius), "10 blocks sideways is outside");
        assertTrue(CraterShape.isCracked(0, -5, 0, radius), "5 blocks down is inside");
        assertFalse(CraterShape.isCracked(0, -6, 0, radius), "6 blocks down is already out");
        assertTrue(CraterShape.isCracked(0, 0, -5, radius));
    }

    @Test
    void theLipAboveTheImpactIsCrackedForRealCraters() {
        assertTrue(CraterShape.isCracked(0, 1, 0, 9.0D), "the block above opens with the crater");
        assertFalse(CraterShape.isCracked(0, 1, 0, 0.5D), "a half block crater does not reach it");
    }

    @Test
    void aRadiusThatMakesNoSenseCracksNothing() {
        assertFalse(CraterShape.isCracked(0, 0, 0, 0.0D));
        assertFalse(CraterShape.isCracked(0, 0, 0, -4.0D));
        assertFalse(CraterShape.isCracked(0, 0, 0, Double.NaN));
        assertFalse(CraterShape.isCracked(0, 0, 0, Double.POSITIVE_INFINITY));
    }

    @Test
    void unbreakableAndToughMaterialIsLeftStanding() {
        assertFalse(CraterShape.isBreakable(-1.0F, 100.0F), "bedrock is not breakable");
        assertFalse(CraterShape.isBreakable(1200.0F, 100.0F), "obsidian survives");
        assertFalse(CraterShape.isBreakable(Float.NaN, 100.0F));
        assertTrue(CraterShape.isBreakable(6.0F, 100.0F), "stone gives way");
        assertTrue(CraterShape.isBreakable(100.0F, 100.0F), "the limit itself gives way");
        assertFalse(CraterShape.isBreakable(100.1F, 100.0F));
    }

    @Test
    void anUnusableLimitFallsBackToTheDefault() {
        assertTrue(CraterShape.isBreakable(6.0F, 0.0F));
        assertTrue(CraterShape.isBreakable(6.0F, Float.NaN));
        assertFalse(CraterShape.isBreakable(1200.0F, 0.0F), "the default still stops obsidian");
    }

    @Test
    void theScannedBoxMatchesTheReach() {
        assertEquals(0L, CraterShape.scannedBlocks(0.0D));
        assertEquals(27L, CraterShape.scannedBlocks(1.0D));
        assertEquals(6859L, CraterShape.scannedBlocks(9.0D));
    }
}
