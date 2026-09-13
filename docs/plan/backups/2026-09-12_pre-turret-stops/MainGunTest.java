package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The 120mm turret: slew rate, elevation stops and the single-shot cadence. */
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
    void slewsTowardsTheDesiredAim() {
        MainGun.Step step = MainGun.advance(MainGun.State.initial(),
                new MainGun.Aim(90.0D, 0.0D), false, INTERVAL);
        assertEquals(MainGun.SLEW_DEGREES_PER_TICK, step.state().aimYawDegrees(), 1.0E-9D);
    }

    @Test
    void pitchIsClampedToTheMountStops() {
        MainGun.State down = MainGun.State.initial();
        for (int i = 0; i < 20; i++) {
            down = MainGun.advance(down, MainGun.desiredAim(0.0F, 90.0F), false, INTERVAL).state();
        }
        assertEquals(MainGun.MAX_ELEVATION_DOWN_DEG, down.aimPitchDegrees(), 1.0E-9D);

        MainGun.State up = MainGun.State.initial();
        for (int i = 0; i < 20; i++) {
            up = MainGun.advance(up, MainGun.desiredAim(0.0F, -90.0F), false, INTERVAL).state();
        }
        assertEquals(-MainGun.MAX_ELEVATION_UP_DEG, up.aimPitchDegrees(), 1.0E-9D);
    }

    @Test
    void firesOnceAndThenWaitsOutTheInterval() {
        MainGun.State state = MainGun.State.initial();
        MainGun.Step first = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true, INTERVAL);
        assertTrue(first.fire(), "an off-cooldown gun must fire on the trigger");
        state = first.state();
        for (int i = 0; i < INTERVAL - 1; i++) {
            MainGun.Step held = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true, INTERVAL);
            assertFalse(held.fire(), "fired again before the interval elapsed at tick " + i);
            state = held.state();
        }
        MainGun.Step reloaded = MainGun.advance(state, new MainGun.Aim(0.0D, 0.0D), true, INTERVAL);
        assertTrue(reloaded.fire(), "the gun must fire again one interval later");
    }

    @Test
    void releasingTheTriggerClearsTheCooldown() {
        MainGun.Step fired = MainGun.advance(MainGun.State.initial(),
                new MainGun.Aim(0.0D, 0.0D), true, INTERVAL);
        MainGun.Step released = MainGun.advance(fired.state(),
                new MainGun.Aim(0.0D, 0.0D), false, INTERVAL);
        assertEquals(0, released.state().cooldownTicks());
        MainGun.Step again = MainGun.advance(released.state(),
                new MainGun.Aim(0.0D, 0.0D), true, INTERVAL);
        assertTrue(again.fire(), "a released trigger must not keep the gun locked out");
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
}
