package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class JetCannonTest {
    private static final JetCannon.Spec TWENTY_FIVE = JetCannon.F35_25MM;
    private static final JetCannon.Spec THIRTY = JetCannon.SU57_30MM;

    /** Ticks the trigger is held until the gun locks out, and how many rounds went out. */
    private record Burst(int fires, int ticks, JetCannon.Snapshot end) {
    }

    private static Burst burst(JetCannon.Spec spec, int maxTicks) {
        JetCannon.Snapshot state = JetCannon.Snapshot.initial();
        int fires = 0;
        int ticks = 0;
        for (; ticks < maxTicks; ticks++) {
            JetCannon.Step step = JetCannon.advance(true, state, spec);
            state = step.snapshot();
            if (step.fire()) {
                fires++;
            }
            if (state.state() == JetCannon.OVERHEATED) {
                break;
            }
        }
        return new Burst(fires, ticks, state);
    }

    @Test
    void theCannonFiresOnCadenceAfterWarmup() {
        JetCannon.Snapshot state = JetCannon.Snapshot.initial();
        int fires = 0;
        int firstFire = -1;
        for (int i = 0; i < 20; i++) {
            JetCannon.Step step = JetCannon.advance(true, state, TWENTY_FIVE);
            state = step.snapshot();
            if (step.fire()) {
                fires++;
                if (firstFire < 0) {
                    firstFire = i;
                }
            }
        }
        assertEquals(TWENTY_FIVE.warmupTicks() - 1, firstFire,
                "the first round leaves after the warmup");
        assertTrue(fires >= 7, "twenty ticks of 25mm should be most of a burst, fires=" + fires);
    }

    @Test
    void releasingTheTriggerCoolsAndResets() {
        JetCannon.Snapshot state = JetCannon.Snapshot.initial();
        for (int i = 0; i < 10; i++) {
            state = JetCannon.advance(true, state, TWENTY_FIVE).snapshot();
        }
        assertTrue(state.heat() > 0, "a burst must build heat");
        for (int i = 0; i < 40; i++) {
            JetCannon.Step step = JetCannon.advance(false, state, TWENTY_FIVE);
            state = step.snapshot();
            assertFalse(step.fire(), "a released trigger must not fire");
        }
        assertEquals(JetCannon.IDLE, state.state());
        assertEquals(0, state.heat());
    }

    @Test
    void sustainedFireOverheatsAndLocksOut() {
        Burst shot = burst(TWENTY_FIVE, 400);
        assertEquals(JetCannon.OVERHEATED, shot.end().state());
        assertTrue(shot.ticks() < 400, "it must lock out within the window");

        // Locked out: no round leaves however long the trigger is held.
        JetCannon.Snapshot state = shot.end();
        for (int i = 0; i < TWENTY_FIVE.overheatLockTicks(); i++) {
            JetCannon.Step step = JetCannon.advance(true, state, TWENTY_FIVE);
            state = step.snapshot();
            assertFalse(step.fire(), "an overheated gun must not fire");
        }
        assertEquals(JetCannon.WARMING, state.state(),
                "after the lock it starts warming up again");
    }

    @Test
    void the30mmFiresSlowerAndOverheatsSoonerThanThe25mm() {
        Burst small = burst(TWENTY_FIVE, 400);
        Burst large = burst(THIRTY, 400);
        assertTrue(small.fires() > large.fires(),
                "the 25mm should put out more rounds, 25mm=" + small.fires()
                        + " 30mm=" + large.fires());
        assertTrue(large.end().overheatLockTicks() >= small.end().overheatLockTicks(),
                "the bigger round should take longer to cool");
    }

    @Test
    void aNullSpecFallsBackToTheTwentyFive() {
        JetCannon.Step step = JetCannon.advance(true, null, null);
        assertFalse(step.fire());
        assertTrue(step.snapshot().state() == JetCannon.WARMING);
    }
}
