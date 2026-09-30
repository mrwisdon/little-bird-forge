package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Layout and gating of the Bradley's smoke wall. */
class SmokeWallTest {
    private static final SmokeWall.Spec SPEC =
            new SmokeWall.Spec(6, 1.3D, 5.0D, 200, 240, 2);

    @Test
    void theWallIsCentredAndEvenlySpaced() {
        double[] offsets = SmokeWall.lateralOffsetsBlocks(SPEC);
        assertEquals(6, offsets.length);
        assertEquals(1.3D * 5.0D / 2.0D, offsets[0], 1.0E-9D);
        assertEquals(-1.3D * 5.0D / 2.0D, offsets[offsets.length - 1], 1.0E-9D);
        assertEquals(0.0D, offsets[0] + offsets[offsets.length - 1], 1.0E-9D);
        for (int i = 1; i < offsets.length; i++) {
            assertEquals(-1.3D, offsets[i] - offsets[i - 1], 1.0E-9D);
        }
        assertEquals(6.5D, SPEC.wallWidthBlocks(), 1.0E-9D);
    }

    @Test
    void aSingleCanisterSitsOnTheCentreLine() {
        SmokeWall.Spec one = new SmokeWall.Spec(1, 1.3D, 5.0D, 200, 240, 2);
        assertArrayEquals(new double[] {0.0D}, SmokeWall.lateralOffsetsBlocks(one), 1.0E-9D);
        assertEquals(0.0D, one.wallWidthBlocks(), 1.0E-9D);
    }

    @Test
    void theWallLandsFiveBlocksInFrontOfTheNose() {
        // Half of the 6.55 m hull is 3.275 blocks; the spec adds 5 more.
        assertEquals(8.275D, SmokeWall.wallCenterAheadBlocks(3.275D, SPEC), 1.0E-9D);
        assertTrue(SmokeWall.wallCenterAheadBlocks(3.275D, SPEC)
                > 3.275D + SPEC.aheadOfNoseBlocks() - 1.0E-9D);
    }

    @Test
    void firingSpendsASalvoAndStartsBothClocks() {
        SmokeWall.State start = SmokeWall.initial(SPEC);
        assertEquals(2, start.charges());
        assertTrue(start.canFire());
        SmokeWall.State fired = SmokeWall.fire(start, SPEC);
        assertEquals(1, fired.charges());
        assertEquals(SPEC.cooldownTicks(), fired.cooldownTicks());
        assertEquals(SPEC.durationTicks(), fired.activeTicks());
        // The cooldown is what gates the second salvo, not the charge count.
        assertFalse(SmokeWall.fire(fired, SPEC).charges() != fired.charges());
        assertFalse(fired.canFire());
    }

    @Test
    void anEmptyLauncherRefuses() {
        SmokeWall.State empty = new SmokeWall.State(0, 0, 0);
        assertFalse(empty.canFire());
        assertEquals(empty, SmokeWall.fire(empty, SPEC));
    }

    @Test
    void tickingRunsBothClocksDownWithoutSpendingCharges() {
        SmokeWall.State state = new SmokeWall.State(1, 2, 3);
        SmokeWall.State next = SmokeWall.tick(state, SPEC);
        assertEquals(1, next.charges());
        assertEquals(1, next.cooldownTicks());
        assertEquals(2, next.activeTicks());
        assertEquals(0, SmokeWall.tick(SmokeWall.tick(next, SPEC), SPEC).cooldownTicks());
        assertEquals(0, SmokeWall.tick(SmokeWall.tick(
                SmokeWall.tick(next, SPEC), SPEC), SPEC).activeTicks());
    }

    @Test
    void aCrateTopsTheSalvosUpButNeverPastCapacity() {
        SmokeWall.State state = new SmokeWall.State(0, 40, 0);
        SmokeWall.State supplied = SmokeWall.supply(state, SPEC, 1);
        assertEquals(1, supplied.charges());
        assertEquals(40, supplied.cooldownTicks());
        assertEquals(SPEC.charges(), SmokeWall.supply(supplied, SPEC, 9).charges());
    }

    @Test
    void hudFractionsReadZeroToOne() {
        SmokeWall.State start = SmokeWall.initial(SPEC);
        assertEquals(1.0F, SmokeWall.chargeFraction(start, SPEC), 1.0E-6F);
        assertEquals(1.0F, SmokeWall.cooldownFraction(start, SPEC), 1.0E-6F);
        SmokeWall.State fired = SmokeWall.fire(start, SPEC);
        assertEquals(0.5F, SmokeWall.chargeFraction(fired, SPEC), 1.0E-6F);
        assertEquals(0.0F, SmokeWall.cooldownFraction(fired, SPEC), 1.0E-6F);
        assertEquals(1.0F, SmokeWall.cooldownFraction(SmokeWall.initial(SPEC), SPEC), 1.0E-6F);
    }
}
