package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The chain gun's cadence is owned by the caller, because two vehicles share the turret rules but
 * not the rate of fire: the Wildcat's 30mm runs the pack default of 2 ticks (600 rounds a minute),
 * while the Bradley's 25mm M242 runs the configured rounds-per-minute figure, which is fractional.
 */
class ChainGunCadenceTest {
    private static final ChainGunTurret.Aim AIM = ChainGunTurret.desiredAim(0.0F, 0.0F);

    /** The Bradley's setting: 180 rounds a minute is one round every six and two thirds ticks. */
    private static final double M242_INTERVAL_TICKS = 1200.0D / 180.0D;

    /** Rounds fired over {@code ticks} ticks with the trigger held, and the first firing tick. */
    private static int[] burst(double intervalTicks, int ticks) {
        ChainGunTurret.State state = ChainGunTurret.State.initial();
        int fires = 0;
        int first = -1;
        for (int tick = 0; tick < ticks; tick++) {
            ChainGunTurret.Step step = ChainGunTurret.advance(state, AIM, true, intervalTicks);
            state = step.state();
            if (step.fire()) {
                if (first < 0) {
                    first = tick;
                }
                fires++;
            }
        }
        return new int[] {fires, first};
    }

    /** The ticks a round actually left on, so a fractional cadence can be pinned exactly. */
    private static List<Integer> firingTicks(double intervalTicks, int ticks) {
        ChainGunTurret.State state = ChainGunTurret.State.initial();
        List<Integer> fired = new ArrayList<>();
        for (int tick = 0; tick < ticks; tick++) {
            ChainGunTurret.Step step = ChainGunTurret.advance(state, AIM, true, intervalTicks);
            state = step.state();
            if (step.fire()) {
                fired.add(tick);
            }
        }
        return fired;
    }

    @Test
    void theM242RunsOneHundredAndEightyRoundsAMinute() {
        int[] result = burst(M242_INTERVAL_TICKS, 300);
        // 300 ticks at 180 a minute: the trigger tick counts, then one round every 6 2/3 ticks.
        assertEquals(0, result[1], "the first round must leave on the trigger tick");
        assertEquals(45, result[0], "180 rounds a minute is 45 rounds in 300 ticks");
        assertEquals(180, result[0] * (1200 / 300));
    }

    @Test
    void aFractionalCadenceCarriesItsRemainder() {
        // The spacing has to alternate 7, 7, 6. Rounding the interval up to 7 would make the gun fire
        // 171 a minute, and throwing the remainder away at every shot is exactly how that happens.
        assertEquals(List.of(0, 7, 14, 20, 27, 34, 40), firingTicks(M242_INTERVAL_TICKS, 41));
    }

    @Test
    void aWholeTickCadenceIsUnchanged() {
        // The Wildcat's 30mm and the built-in default must keep firing on exactly the old ticks.
        assertEquals(List.of(0, 4, 8, 12, 16), firingTicks(4.0D, 17));
        assertEquals(List.of(0, 2, 4, 6, 8), firingTicks(2.0D, 9));
    }

    @Test
    void thePacksDefaultCadenceIsStillSixHundred() {
        int[] result = burst(ChainGunTurret.FIRE_INTERVAL_TICKS, 40);
        assertEquals(20, result[0], "the built-in cadence must not have moved");
        assertEquals(600, result[0] * (1200 / 40));
    }

    @Test
    void aSlowerCadenceSpendsLessHeat() {
        ChainGunTurret.State fast = ChainGunTurret.State.initial();
        ChainGunTurret.State slow = ChainGunTurret.State.initial();
        for (int tick = 0; tick < 40; tick++) {
            fast = ChainGunTurret.advance(fast, AIM, true, 2).state();
            slow = ChainGunTurret.advance(slow, AIM, true, 4).state();
        }
        assertEquals(20, fast.heat());
        assertEquals(10, slow.heat());
    }

    @Test
    void azeroIntervalIsClampedRatherThanFiringEveryTick() {
        // Defensive: a bad config entry must not turn the gun into a per-tick hose.
        int[] result = burst(0, 40);
        assertEquals(20, result[0], "interval 0 is clamped to the pack's 2 tick minimum");
    }

    @Test
    void aTapCannotStealAFreeShot() {
        // The reported bug: click-spam fired far faster than the setting because releasing the
        // trigger cleared the interval, so every fresh press fired on its first held tick. One
        // round, one tick off the trigger, then press again - the second press must not fire.
        ChainGunTurret.Step first = ChainGunTurret.advance(
                ChainGunTurret.State.initial(), AIM, true, M242_INTERVAL_TICKS);
        assertTrue(first.fire(), "the first press fires");
        ChainGunTurret.State released = ChainGunTurret.advance(
                first.state(), AIM, false, M242_INTERVAL_TICKS).state();
        assertFalse(ChainGunTurret.advance(released, AIM, true, M242_INTERVAL_TICKS).fire(),
                "a tap must not beat the 180 rounds a minute interval");
    }

    @Test
    void clickSpamCannotBeatTheInterval() {
        // Ten clicks a second is the pattern that used to hose. The spacing between rounds must
        // never come in under the configured interval, whatever the trigger does.
        ChainGunTurret.State state = ChainGunTurret.State.initial();
        List<Integer> fired = new ArrayList<>();
        for (int tick = 0; tick < 200; tick++) {
            ChainGunTurret.Step step = ChainGunTurret.advance(state, AIM, tick % 3 == 0,
                    M242_INTERVAL_TICKS);
            state = step.state();
            if (step.fire()) {
                fired.add(tick);
            }
        }
        assertFalse(fired.isEmpty(), "the gun still fires while it is being tapped");
        assertTrue(fired.size() <= 30,
                "200 ticks cannot hold more than 30 rounds at 180 a minute, got " + fired.size());
        for (int index = 1; index < fired.size(); index++) {
            int gap = fired.get(index) - fired.get(index - 1);
            assertTrue(gap >= 7, "rounds stayed only " + gap + " ticks apart at " + fired);
        }
    }

    @Test
    void aJunkIntervalFallsBackToTheBuiltInCadence() {
        // A NaN out of a hand-edited config must not silently stop the gun firing at all.
        int[] result = burst(Double.NaN, 40);
        assertEquals(20, result[0], "a non-finite interval uses the built-in 2 tick cadence");
    }
}

