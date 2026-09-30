package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import org.junit.jupiter.api.Test;

/** The 120mm turret: elevation stops and the single-shot cadence. */
class MainGunTest {
    private static final int INTERVAL = 50;

    @Test
    void theMountStopsAtThirtyUpAndTenDown() {
        // The agreed stops, pinned as numbers so a later edit cannot drift them silently.
        assertEquals(30.0D, MainGun.MAX_ELEVATION_UP_DEG, 1.0E-9D);
        assertEquals(10.0D, MainGun.MAX_ELEVATION_DOWN_DEG, 1.0E-9D);
        assertEquals(10.0D, MainGun.desiredAim(0.0F, 90.0F).pitchDegrees(), 1.0E-9D,
                "past the depression stop clamps to +10 (pitch is positive downwards)");
        assertEquals(-30.0D, MainGun.desiredAim(0.0F, -90.0F).pitchDegrees(), 1.0E-9D,
                "past the elevation stop clamps to -30");
        assertTrue(MainGun.atElevationStop(MainGun.MAX_ELEVATION_DOWN_DEG));
        assertTrue(MainGun.atElevationStop(-MainGun.MAX_ELEVATION_UP_DEG));
        assertFalse(MainGun.atElevationStop(0.0D));
    }

    @Test
    void followsTheDesiredAimDirectly() {
        MainGun.Step step = MainGun.advance(MainGun.State.initial(),
                new MainGun.Aim(90.0D, 0.0D), false);
        assertEquals(90.0D, step.state().aimYawDegrees(), 1.0E-9D,
                "the mount holds the driver's sight with no traverse delay");
    }

    @Test
    void holdsTheAimAcrossTheSeam() {
        // Parked at 170 and asked for -170: the held angle wraps across the seam into +/-180
        // instead of running the long way round to +190.
        MainGun.Step step = MainGun.advance(new MainGun.State(170.0D, 0.0D, 0),
                MainGun.desiredAim(-170.0F, 0.0F), false);
        assertEquals(-170.0D, step.state().aimYawDegrees(), 1.0E-9D);
    }

    @Test
    void pitchIsClampedToTheMountStops() {
        MainGun.State down = MainGun.State.initial();
        for (int i = 0; i < 20; i++) {
            down = MainGun.advance(down, MainGun.desiredAim(0.0F, 90.0F), false).state();
        }
        assertEquals(MainGun.MAX_ELEVATION_DOWN_DEG, down.aimPitchDegrees(), 1.0E-9D);

        MainGun.State up = MainGun.State.initial();
        for (int i = 0; i < 20; i++) {
            up = MainGun.advance(up, MainGun.desiredAim(0.0F, -90.0F), false).state();
        }
        assertEquals(-MainGun.MAX_ELEVATION_UP_DEG, up.aimPitchDegrees(), 1.0E-9D);
    }

    @Test
    void firesOnceAndThenWaitsOutTheInterval() {
        MainGun.State state = MainGun.State.initial();
        MainGun.Step first = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true);
        assertTrue(first.fire(), "an off-cooldown gun must fire on the trigger");
        state = MainGun.afterFire(first.state(), INTERVAL);
        for (int i = 0; i < INTERVAL - 1; i++) {
            MainGun.Step held = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true);
            assertFalse(held.fire(), "fired again before the interval elapsed at tick " + i);
            state = held.state();
        }
        MainGun.Step reloaded = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true);
        assertTrue(reloaded.fire(), "the gun must fire again one interval later");
    }

    @Test
    void releasingTheTriggerDoesNotClearTheCooldown() {
        MainGun.Step fired = MainGun.advance(MainGun.State.initial(),
                new MainGun.Aim(0.0D, 0.0D), true);
        MainGun.State locked = MainGun.afterFire(fired.state(), INTERVAL);
        MainGun.Step released = MainGun.advance(locked, new MainGun.Aim(0.0D, 0.0D), false);
        assertTrue(released.state().cooldownTicks() > 0,
                "a released trigger keeps the clock running instead of zeroing it");
        MainGun.Step again = MainGun.advance(released.state(),
                new MainGun.Aim(0.0D, 0.0D), true);
        assertFalse(again.fire(), "re-pressing the trigger must not beat the interval");
    }

    @Test
    void tappingTheTriggerCannotBeatTheInterval() {
        // Alternate press/release after a shot: however the trigger is tapped, only the tick
        // INTERVAL ticks later may fire again.
        MainGun.State state = MainGun.afterFire(MainGun.State.initial(), INTERVAL);
        for (int i = 0; i < INTERVAL - 1; i++) {
            MainGun.Step tapped = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), i % 2 == 0);
            assertFalse(tapped.fire(), "a tap fired before the interval elapsed at tick " + i);
            state = tapped.state();
        }
        MainGun.Step ready = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true);
        assertTrue(ready.fire(), "the gun fires once the whole interval has run, however it was tapped");
    }

    @Test
    void anUncommittedShotLeavesTheGunReady() {
        // afterFire is the caller's confirmation that a round left the barrel. Without it the gate
        // stays open, so a trigger pulled on an empty rack starts no cooldown.
        MainGun.Step first = MainGun.advance(MainGun.State.initial(),
                new MainGun.Aim(0.0D, 0.0D), true);
        assertTrue(first.fire());
        MainGun.Step second = MainGun.advance(first.state(), new MainGun.Aim(0.0D, 0.0D), true);
        assertTrue(second.fire(), "no committed shot means no lockout");
    }

    @Test
    void afterFireCountsTheWholeInterval() {
        MainGun.State state = MainGun.afterFire(MainGun.State.initial(), INTERVAL);
        for (int i = 0; i < INTERVAL - 1; i++) {
            state = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true).state();
            assertEquals(INTERVAL - 1 - i, state.cooldownTicks(),
                    "the cooldown counts down one tick at a time");
        }
        assertTrue(MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true).fire(),
                "the next round is allowed exactly INTERVAL ticks later");
    }

    @Test
    void aimDirectionUsesMinecraftsConvention() {
        var forward = MainGun.aimDirection(0.0D, 0.0D);
        assertEquals(0.0D, forward.x(), 1.0E-9D);
        assertEquals(0.0D, forward.y(), 1.0E-9D);
        assertEquals(1.0D, forward.z(), 1.0E-9D);

        // Pitch is clamped to the mount's stops before it is turned into a direction.
        var down = MainGun.aimDirection(0.0D, MainGun.MAX_ELEVATION_DOWN_DEG);
        assertEquals(-Math.sin(Math.toRadians(MainGun.MAX_ELEVATION_DOWN_DEG)), down.y(), 1.0E-9D);
    }

    @Test
    void muzzleStartsAheadOfTheTurretRing() {
        var pivot = new io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3(0, 0, 0);
        var forward = new io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3(0, 0, 1);
        var up = new io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3(0, 1, 0);
        var muzzle = MainGun.muzzleOrigin(pivot, forward, up, 1.5D, 0.0D, 3.0D, 0.0D, 0.0D);
        assertEquals(0.0D, muzzle.x(), 1.0E-9D);
        assertEquals(1.5D, muzzle.y(), 1.0E-9D);
        assertEquals(3.0D, muzzle.z(), 1.0E-9D);
    }

    @Test
    void theMuzzleSitsOnTheTrunnionNotOnTheRing() {
        // The tank's barrel elevates on its own pivot at the mantlet: trunnion 31.36 units (1.96
        // blocks) above the hull floor and 30.72 units (1.92 blocks) ahead of the ring, with 62.16
        // units (3.885 blocks) of barrel left to the muzzle. Level, the muzzle is 5.805 blocks
        // ahead of the ring and 1.96 above it - the 0.52 block the old ring-based sum missed.
        assertEquals(1.96D,
                io.github.littlebird.entity.MainBattleTankGeometry.gunTrunnionHeightBlocks(),
                1.0E-6D);
        assertEquals(1.92D,
                io.github.littlebird.entity.MainBattleTankGeometry.gunTrunnionForwardBlocks(),
                1.0E-6D);
        assertEquals(3.885D,
                io.github.littlebird.entity.MainBattleTankGeometry.barrelReachBlocks(), 1.0E-6D);
        LittleBirdBallistics.Vector3 origin = new LittleBirdBallistics.Vector3(0.0D, 10.0D, 0.0D);
        LittleBirdBallistics.Vector3 muzzle = MainGun.muzzleOrigin(origin,
                new LittleBirdBallistics.Vector3(0.0D, 0.0D, 1.0D),
                new LittleBirdBallistics.Vector3(0.0D, 1.0D, 0.0D),
                io.github.littlebird.entity.MainBattleTankGeometry.gunTrunnionHeightBlocks(),
                io.github.littlebird.entity.MainBattleTankGeometry.gunTrunnionForwardBlocks(),
                io.github.littlebird.entity.MainBattleTankGeometry.barrelReachBlocks(), 0.0D, 0.0D);
        assertEquals(5.805D, muzzle.z(), 1.0E-3D, "ring to muzzle along the barrel");
        assertEquals(11.96D, muzzle.y(), 1.0E-3D, "trunnion height above the origin");
    }
}
