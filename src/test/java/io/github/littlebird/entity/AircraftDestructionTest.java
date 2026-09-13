package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class AircraftDestructionTest {

    @Test
    void theWriteOffRunsExactlyOnce() {
        AircraftDestruction destruction = new AircraftDestruction();
        assertFalse(destruction.isDestroyed());
        assertTrue(destruction.beginDestruction(), "the first destruction goes ahead");
        assertFalse(destruction.beginDestruction(), "and no later one may explode again");
        assertFalse(destruction.beginDestruction());
        assertTrue(destruction.isDestroyed());
    }

    @Test
    void aDestroyedAirframeStopsTakingDamage() {
        AircraftDestruction destruction = new AircraftDestruction();
        assertTrue(destruction.acceptsDamage());
        destruction.beginDestruction();
        assertFalse(destruction.acceptsDamage(),
                "this is the link that broke the explosion ping pong");
    }

    @Test
    void twoAircraftExplodingAtEachOtherSettleInsteadOfRecursing() {
        // The shape of the real crash: A dies, its blast damages B, B dies, its blast comes back
        // to A. Without the guard the two keep taking turns until the stack overflows.
        AircraftDestruction a = new AircraftDestruction();
        AircraftDestruction b = new AircraftDestruction();
        int explosions = 0;
        for (int round = 0; round < 1000; round++) {
            if (damage(a)) {
                explosions++;
            }
            if (damage(b)) {
                explosions++;
            }
        }
        assertTrue(explosions <= 2, "each airframe may explode once, saw " + explosions);
        assertTrue(a.isDestroyed() && b.isDestroyed());
    }

    /** One incoming explosion: either it writes the airframe off, or it is ignored. */
    private static boolean damage(AircraftDestruction destruction) {
        return destruction.acceptsDamage() && destruction.beginDestruction();
    }
}
