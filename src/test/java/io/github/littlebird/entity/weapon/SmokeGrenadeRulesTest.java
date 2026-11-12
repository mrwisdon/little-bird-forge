package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Fuse, swell and fade of a thrown smoke canister's cloud. */
class SmokeGrenadeRulesTest {
    private static final int LIFETIME = 300;
    private static final float RADIUS = 5.5F;

    @Test
    void aFifteenSecondCloudLastsThreeHundredTicks() {
        assertEquals(300, SmokeGrenadeRules.lifetimeTicks(15));
        assertEquals(20, SmokeGrenadeRules.lifetimeTicks(1));
        // A zero or negative duration would be a broken setting, not an invisible cloud.
        assertEquals(SmokeGrenadeRules.MIN_LIFETIME_TICKS, SmokeGrenadeRules.lifetimeTicks(0));
        assertEquals(SmokeGrenadeRules.MIN_LIFETIME_TICKS, SmokeGrenadeRules.lifetimeTicks(-5));
    }

    @Test
    void theFuseBurnsDownToFiveSeconds() {
        assertFalse(SmokeGrenadeRules.fuseExpired(0));
        assertFalse(SmokeGrenadeRules.fuseExpired(SmokeGrenadeRules.FUSE_TICKS - 1));
        assertTrue(SmokeGrenadeRules.fuseExpired(SmokeGrenadeRules.FUSE_TICKS));
        assertTrue(SmokeGrenadeRules.fuseExpired(SmokeGrenadeRules.FUSE_TICKS + 40));
    }

    @Test
    void theCloudIsSpentOnlyAtTheEndOfItsLife() {
        assertFalse(SmokeGrenadeRules.spent(0, LIFETIME));
        assertFalse(SmokeGrenadeRules.spent(LIFETIME - 1, LIFETIME));
        assertTrue(SmokeGrenadeRules.spent(LIFETIME, LIFETIME));
        assertTrue(SmokeGrenadeRules.spent(LIFETIME + 1, LIFETIME));
        assertTrue(SmokeGrenadeRules.spent(0, 0));
    }

    @Test
    void theCloudStartsPartlyOpenAndReachesTheConfiguredRadius() {
        assertEquals(RADIUS * SmokeGrenadeRules.START_RADIUS_FRACTION,
                SmokeGrenadeRules.radiusAt(0, RADIUS), 1.0E-6F);
        assertEquals(RADIUS, SmokeGrenadeRules.radiusAt(SmokeGrenadeRules.GROWTH_TICKS, RADIUS),
                1.0E-6F);
        // It never overshoots once it is full.
        assertEquals(RADIUS, SmokeGrenadeRules.radiusAt(LIFETIME, RADIUS), 1.0E-6F);
        assertEquals(0.0F, SmokeGrenadeRules.radiusAt(10, 0.0F), 1.0E-6F);
    }

    @Test
    void theCloudGrowsMonotonically() {
        float previous = -1.0F;
        for (int tick = 0; tick <= SmokeGrenadeRules.GROWTH_TICKS + 10; tick++) {
            float radius = SmokeGrenadeRules.radiusAt(tick, RADIUS);
            assertTrue(radius >= previous, "radius fell back at tick " + tick);
            previous = radius;
        }
    }

    @Test
    void theCloudFadesInFastAndOutSlow() {
        assertEquals(0.0F, SmokeGrenadeRules.opacityAt(0, LIFETIME), 1.0E-6F);
        assertEquals(0.0F, SmokeGrenadeRules.opacityAt(LIFETIME, LIFETIME), 1.0E-6F);
        // The plateau is the whole middle of the life.
        assertEquals(1.0F, SmokeGrenadeRules.opacityAt(60, LIFETIME), 1.0E-6F);
        assertEquals(1.0F, SmokeGrenadeRules.opacityAt(200, LIFETIME), 1.0E-6F);
        // Fading out is a ramp, and it ends at zero.
        float late = SmokeGrenadeRules.opacityAt(LIFETIME - 5, LIFETIME);
        assertTrue(late > 0.0F && late < 1.0F, "opacity at the tail was " + late);
        assertTrue(SmokeGrenadeRules.opacityAt(LIFETIME - 20, LIFETIME) > late);
    }

    @Test
    void opacityNeverLeavesTheUnitRange() {
        for (int tick = -5; tick <= LIFETIME + 5; tick++) {
            float opacity = SmokeGrenadeRules.opacityAt(tick, LIFETIME);
            assertTrue(opacity >= 0.0F && opacity <= 1.0F, "opacity " + opacity + " at " + tick);
        }
        assertEquals(0.0F, SmokeGrenadeRules.opacityAt(10, 0), 1.0E-6F);
    }

    @Test
    void thePuffsStayInsideTheCloudAndWobble() {
        for (int puff = 0; puff < 14; puff++) {
            float small = SmokeGrenadeRules.puffRadius(puff, 5, RADIUS);
            float large = SmokeGrenadeRules.puffRadius(puff, LIFETIME - 10, RADIUS);
            assertTrue(large > small, "puff " + puff + " did not grow");
            assertTrue(large < SmokeGrenadeRules.radiusAt(LIFETIME - 10, RADIUS),
                    "puff " + puff + " escapes the cloud shell");
            assertTrue(large > 0.0F);
        }
        // Neighbouring puffs are out of phase, so the silhouette is not a breathing sphere.
        boolean outOfPhase = false;
        for (int puff = 1; puff < 14 && !outOfPhase; puff++) {
            outOfPhase = Math.abs(SmokeGrenadeRules.puffRadius(puff, 30, RADIUS)
                    - SmokeGrenadeRules.puffRadius(puff - 1, 30, RADIUS)) > 1.0E-4F;
        }
        assertTrue(outOfPhase);
    }
}
