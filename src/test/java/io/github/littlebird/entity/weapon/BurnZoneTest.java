package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The incendiary's burning ground: how long it burns and how often it hurts. */
class BurnZoneTest {

    private static int pulsesFor(int lifetimeTicks) {
        BurnZone zone = new BurnZone(lifetimeTicks);
        int pulses = 0;
        for (int tick = 0; tick < lifetimeTicks; tick++) {
            BurnZone.Step step = zone.advance();
            if (step == BurnZone.Step.DAMAGE || step == BurnZone.Step.DAMAGE_AND_RELIGHT) {
                pulses++;
            }
        }
        return pulses;
    }

    @Test
    void theFirstPulseLandsOneSecondIn() {
        BurnZone zone = new BurnZone(100);
        for (int tick = 1; tick < BurnZone.DAMAGE_INTERVAL_TICKS; tick++) {
            assertEquals(BurnZone.Step.IDLE, zone.advance(), "tick " + tick + " must be quiet");
        }
        assertEquals(BurnZone.Step.DAMAGE, zone.advance(), "tick 20 is the first hit");
    }

    @Test
    void pulsesComeOnceASecondAndRelightsEveryTwo() {
        BurnZone zone = new BurnZone(80);
        for (int tick = 1; tick <= 80; tick++) {
            BurnZone.Step step = zone.advance();
            boolean second = tick % BurnZone.DAMAGE_INTERVAL_TICKS == 0;
            boolean twoSeconds = tick % BurnZone.RELIGHT_INTERVAL_TICKS == 0;
            BurnZone.Step expected = second
                    ? (twoSeconds ? BurnZone.Step.DAMAGE_AND_RELIGHT : BurnZone.Step.DAMAGE)
                    : (twoSeconds ? BurnZone.Step.RELIGHT : BurnZone.Step.IDLE);
            assertEquals(expected, step, "tick " + tick);
        }
    }

    @Test
    void aTwoMinuteZonePulsesOncePerSecondForItsWholeLife() {
        // The shipped default: 2400 ticks = 120 seconds = 120 pulses.
        assertEquals(120, pulsesFor(2400));
        assertEquals(2400 / BurnZone.RELIGHT_INTERVAL_TICKS, 60, "relights are counted separately");
    }

    @Test
    void nothingIsDueBeforeTheFirstInterval() {
        BurnZone zone = new BurnZone(19);
        for (int tick = 0; tick < 19; tick++) {
            assertFalse(zone.advance() == BurnZone.Step.DAMAGE, "no hit inside the first second");
        }
        assertTrue(zone.expired(), "19 ticks is over");
    }

    @Test
    void zeroAndNegativeLifetimesAreDoneImmediately() {
        BurnZone zero = new BurnZone(0);
        assertEquals(BurnZone.Step.EXPIRE, zero.advance());
        assertTrue(zero.expired());
        BurnZone negative = new BurnZone(-40);
        assertEquals(0, negative.lifetimeTicks());
        assertEquals(BurnZone.Step.EXPIRE, negative.advance());
        assertTrue(negative.expired());
    }

    @Test
    void anExpiredZoneStaysExpiredAndItsClockFreezes() {
        BurnZone zone = new BurnZone(5);
        for (int tick = 0; tick < 5; tick++) {
            zone.advance();
        }
        assertTrue(zone.expired());
        int frozen = zone.remainingTicks();
        for (int extra = 0; extra < 200; extra++) {
            assertEquals(BurnZone.Step.EXPIRE, zone.advance());
        }
        assertEquals(frozen, zone.remainingTicks(), "an expired zone must not keep counting");
    }

    @Test
    void theClockOnlyEverCountsDown() {
        BurnZone zone = new BurnZone(1200);
        int previous = zone.remainingTicks();
        for (int tick = 0; tick < 1200; tick++) {
            zone.advance();
            assertTrue(zone.remainingTicks() <= previous, "tick " + tick);
            previous = zone.remainingTicks();
        }
        assertEquals(0, zone.remainingTicks());
    }

    @Test
    void theLastTickStillActs() {
        // 2400 is both a damage and a relight tick: the final step must not be swallowed by expiry.
        BurnZone zone = new BurnZone(2400);
        BurnZone.Step last = BurnZone.Step.IDLE;
        for (int tick = 0; tick < 2400; tick++) {
            last = zone.advance();
        }
        assertEquals(BurnZone.Step.DAMAGE_AND_RELIGHT, last);
        assertTrue(zone.expired());
    }
}
