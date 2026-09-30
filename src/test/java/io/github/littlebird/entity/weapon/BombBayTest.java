package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.BombBay;
import org.junit.jupiter.api.Test;

/** The bay's release schedule: what a trigger pull commits, and how long it stays shut. */
final class BombBayTest {
    @Test
    void anEmptyBayRefusesEvenWithTheCooldownClear() {
        assertFalse(BombBay.canRelease(0, 0), "no bombs left means no release");
        assertFalse(BombBay.canRelease(-3, 0), "a negative count is still empty");
    }

    @Test
    void aBayOnCooldownRefusesAsLongAsTheCooldownRuns() {
        assertTrue(BombBay.canRelease(4, 0), "ready");
        assertFalse(BombBay.canRelease(4, 1), "one tick short");
        assertFalse(BombBay.canRelease(4, BombBay.RELEASE_INTERVAL_TICKS), "still rippling");
        assertTrue(BombBay.canRelease(4, -1), "a stale negative cooldown is spent");
    }

    @Test
    void aSalvoCommitsTwoAndASingleCommitsOne() {
        assertEquals(BombBay.SALVO_COUNT, BombBay.bombsToDrop(true, 10));
        assertEquals(1, BombBay.bombsToDrop(false, 10));
        assertEquals(2, BombBay.SALVO_COUNT, "the frozen salvo size");
    }

    @Test
    void aSalvoWithOneLeftCommitsOnlyThatOne() {
        assertEquals(1, BombBay.bombsToDrop(true, 1));
        assertEquals(0, BombBay.bombsToDrop(true, 0));
        assertEquals(0, BombBay.bombsToDrop(false, 0));
        assertEquals(1, BombBay.bombsToDrop(false, 1));
    }

    @Test
    void everyDropCountFitsInsideTheRemainingStore() {
        for (int remaining = 0; remaining <= 5; remaining++) {
            assertTrue(BombBay.bombsToDrop(true, remaining) <= remaining, "salvo " + remaining);
            assertTrue(BombBay.bombsToDrop(false, remaining) <= remaining, "single " + remaining);
        }
    }

    @Test
    void aSalvoLocksTheBayForItsWholeRipple() {
        assertEquals(BombBay.RELEASE_INTERVAL_TICKS, BombBay.nextCooldown(false));
        assertEquals(BombBay.RELEASE_INTERVAL_TICKS * BombBay.SALVO_COUNT,
                BombBay.nextCooldown(true));
        assertTrue(BombBay.nextCooldown(true) > BombBay.RELEASE_INTERVAL_TICKS,
                "the ripple has to finish before the bay opens again");
    }

    @Test
    void theRippleIsSpacedAndTheStoreIsMultiShot() {
        assertTrue(BombBay.RELEASE_INTERVAL_TICKS > 0, "a zero interval would be one instant");
        assertTrue(BombBay.SALVO_COUNT > 1, "a salvo has to be more than a single");
        assertEquals(6, BombBay.RELEASE_INTERVAL_TICKS, "the frozen interval");
    }
}
