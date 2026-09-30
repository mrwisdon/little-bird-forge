package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import org.junit.jupiter.api.Test;

final class MissileWeaponsTest {
    // ------------------------------------------------------------------
    // TargetLock
    // ------------------------------------------------------------------

    @Test
    void aLockMaturesAfterTheAcquireWindow() {
        TargetLock.Snapshot state = TargetLock.Snapshot.NONE;
        for (int i = 0; i < TargetLock.Spec.IR.acquireTicks(); i++) {
            state = TargetLock.advance(state, 7, true, TargetLock.Spec.IR);
        }
        assertTrue(state.locked());
        assertEquals(7, state.targetId());
    }

    @Test
    void aLockSurvivesAGracePeriodThenBreaks() {
        TargetLock.Snapshot state = TargetLock.Snapshot.NONE;
        for (int i = 0; i < TargetLock.Spec.IR.acquireTicks(); i++) {
            state = TargetLock.advance(state, 7, true, TargetLock.Spec.IR);
        }
        for (int i = 0; i < TargetLock.Spec.IR.loseGraceTicks() - 1; i++) {
            state = TargetLock.advance(state, 7, false, TargetLock.Spec.IR);
            assertTrue(state.locked(), "the lock should survive the grace period");
        }
        state = TargetLock.advance(state, 7, false, TargetLock.Spec.IR);
        assertFalse(state.hasTarget(), "one tick past the grace the lock is gone");
    }

    @Test
    void switchingTargetRestartsTheLock() {
        TargetLock.Snapshot state = TargetLock.Snapshot.NONE;
        for (int i = 0; i < TargetLock.Spec.IR.acquireTicks(); i++) {
            state = TargetLock.advance(state, 7, true, TargetLock.Spec.IR);
        }
        state = TargetLock.advance(state, 9, true, TargetLock.Spec.IR);
        assertFalse(state.locked());
        assertEquals(9, state.targetId());
        assertEquals(1, state.progress());
    }

    // ------------------------------------------------------------------
    // Countermeasures
    // ------------------------------------------------------------------

    @Test
    void deployingConsumesAFlareAndOpensAWindow() {
        Countermeasures.Snapshot state = Countermeasures.Snapshot.full(Countermeasures.DEFAULT);
        state = Countermeasures.deploy(state, Countermeasures.DEFAULT);
        assertEquals(Countermeasures.DEFAULT.flares() - 1, state.flares());
        assertEquals(Countermeasures.DEFAULT.activeWindowTicks(), state.activeWindow());
        assertTrue(Countermeasures.seduces(state, Countermeasures.DEFAULT, 0.1D));
        assertFalse(Countermeasures.seduces(state, Countermeasures.DEFAULT, 0.9D));
    }

    @Test
    void anEmptyOrCoolingDispenserRefuses() {
        Countermeasures.Snapshot cooling = new Countermeasures.Snapshot(4, 5, 0);
        Countermeasures.Snapshot refused = Countermeasures.deploy(cooling, Countermeasures.DEFAULT);
        assertEquals(4, refused.flares(), "a cooling dispenser spends nothing");
        assertEquals(0, refused.activeWindow(), "and opens no window");
        Countermeasures.Snapshot empty = new Countermeasures.Snapshot(0, 0, 0);
        assertEquals(empty, Countermeasures.deploy(empty, Countermeasures.DEFAULT));
    }

    @Test
    void theWindowClosesAndARefillIsCapped() {
        Countermeasures.Snapshot state = Countermeasures.Snapshot.full(Countermeasures.DEFAULT);
        state = Countermeasures.deploy(state, Countermeasures.DEFAULT);
        for (int i = 0; i < Countermeasures.DEFAULT.activeWindowTicks(); i++) {
            state = Countermeasures.advance(state);
        }
        assertFalse(Countermeasures.seduces(state, Countermeasures.DEFAULT, 0.0D));
        state = Countermeasures.refill(state, Countermeasures.DEFAULT, 100);
        assertEquals(Countermeasures.DEFAULT.flares(), state.flares());
    }

    // ------------------------------------------------------------------
    // MissileGuidance
    // ------------------------------------------------------------------

    @Test
    void theHeadingTurnsTowardTheTargetButNoFasterThanTheSeeker() {
        Vector3 position = new Vector3(0.0D, 0.0D, 0.0D);
        Vector3 velocity = new Vector3(0.0D, 0.0D, 10.0D);
        Vector3 target = new Vector3(10.0D, 0.0D, 10.0D);
        Vector3 steered = MissileGuidance.home(position, velocity, target,
                new Vector3(0.0D, 0.0D, 0.0D), MissileGuidance.IR);
        assertTrue(steered.x() > 0.0D, "the nose should swing toward the target");
        double dot = velocity.normalize().dot(steered.normalize());
        double turned = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, dot))));
        assertTrue(turned <= MissileGuidance.IR.maxTurnDegreesPerTick() + 1.0E-6D,
                "the turn must respect the seeker limit, turned=" + turned);
        assertEquals(10.0D, steered.length(), 1.0E-6D, "speed is preserved");
    }

    @Test
    void aLockAheadOfACrossingTargetLeadsIt() {
        Vector3 steered = MissileGuidance.home(
                new Vector3(0.0D, 0.0D, 0.0D),
                new Vector3(0.0D, 0.0D, 10.0D),
                new Vector3(0.0D, 0.0D, 40.0D),
                new Vector3(1.0D, 0.0D, 0.0D),
                MissileGuidance.IR);
        assertTrue(steered.x() > 0.0D,
                "a crossing target should be led, not chased");
    }

    @Test
    void aNullSpecFallsBackToTheIrSeeker() {
        Vector3 steered = MissileGuidance.home(
                new Vector3(0.0D, 0.0D, 0.0D), new Vector3(0.0D, 0.0D, 10.0D),
                new Vector3(0.0D, 0.0D, 10.0D), null, null);
        assertTrue(steered.length() > 0.0D);
    }
}
