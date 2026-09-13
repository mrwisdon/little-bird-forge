package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import org.junit.jupiter.api.Test;

final class ApacheMissileBallisticsTest {
    private static final Vector3 FORWARD = new Vector3(0.0D, 0.0D, 1.0D);
    private static final double EPSILON = 1.0E-6D;

    @Test
    void launchFollowsTheSightLine() {
        Vector3 velocity = ApacheMissileBallistics.launchVelocity(FORWARD);
        assertEquals(ApacheMissileBallistics.INITIAL_SPEED, velocity.length(), EPSILON);
        assertEquals(1.0D, velocity.normalize().dot(FORWARD), 1.0E-9D);
    }

    @Test
    void speedSpinsUpToCruise() {
        assertEquals(ApacheMissileBallistics.INITIAL_SPEED,
                ApacheMissileBallistics.speedAt(0), EPSILON);
        assertEquals(ApacheMissileBallistics.CRUISE_SPEED,
                ApacheMissileBallistics.speedAt(ApacheMissileBallistics.SPIN_UP_TICKS), EPSILON);
        assertEquals(ApacheMissileBallistics.CRUISE_SPEED,
                ApacheMissileBallistics.speedAt(500), EPSILON);
    }

    @Test
    void theMissileIsSteeredOntoTheSightLine() {
        // Fired straight ahead, sight swung 90 degrees to the right: the missile has to turn.
        Vector3 sight = new Vector3(1.0D, 0.0D, 0.0D);
        Vector3 velocity = ApacheMissileBallistics.launchVelocity(FORWARD);
        int ticks = 0;
        while (velocity.normalize().dot(sight) < 0.999D && ticks < 200) {
            Vector3 next = ApacheMissileBallistics.guide(velocity, sight, ticks);
            double turned = Math.toDegrees(Math.acos(Math.min(1.0D,
                    velocity.normalize().dot(next.normalize()))));
            assertTrue(turned <= ApacheMissileBallistics.MAX_TURN_DEGREES_PER_TICK + 1.0E-6D,
                    "a turn of " + turned + " degrees is faster than the missile can manage");
            velocity = next;
            ticks++;
        }
        assertTrue(ticks < 40, "it should converge in a couple of seconds, took " + ticks);
        assertEquals(ApacheMissileBallistics.CRUISE_SPEED, velocity.length(), 1.0E-6D);
    }

    @Test
    void aMissileOnTheSightLineStaysThere() {
        Vector3 velocity = ApacheMissileBallistics.launchVelocity(FORWARD);
        for (int tick = 0; tick < 60; tick++) {
            velocity = ApacheMissileBallistics.guide(velocity, FORWARD, tick);
        }
        assertTrue(Math.toDegrees(Math.acos(Math.min(1.0D,
                        velocity.normalize().dot(FORWARD)))) < 1.0D,
                "the drop bias must stay inside the guidance's authority");
    }

    @Test
    void nonsenseInputsAreIgnored() {
        Vector3 velocity = ApacheMissileBallistics.launchVelocity(FORWARD);
        assertEquals(velocity, ApacheMissileBallistics.guide(velocity,
                new Vector3(0.0D, 0.0D, 0.0D), 5), "a zero sight line changes nothing");
        Vector3 restarted = ApacheMissileBallistics.guide(
                new Vector3(0.0D, 0.0D, 0.0D), FORWARD, 5);
        assertEquals(ApacheMissileBallistics.INITIAL_SPEED, restarted.length(), EPSILON);
    }

    @Test
    void theMissileIsSlowEnoughToReadAsAMissile() {
        assertTrue(ApacheMissileBallistics.CRUISE_SPEED <= 1.5D,
                "cruise was halved from 3.00: " + ApacheMissileBallistics.CRUISE_SPEED);
        assertTrue(ApacheMissileBallistics.INITIAL_SPEED <= 0.75D,
                "and so was the launch speed: " + ApacheMissileBallistics.INITIAL_SPEED);
        assertTrue(ApacheMissileBallistics.INITIAL_SPEED
                < ApacheMissileBallistics.CRUISE_SPEED, "it still spins up");
        double radius = ApacheMissileBallistics.CRUISE_SPEED
                / Math.toRadians(ApacheMissileBallistics.MAX_TURN_DEGREES_PER_TICK);
        assertTrue(radius < 15.0D, "a slower missile turns tighter: " + radius);
    }

    @Test
    void theLifetimeStillCoversTheSightRange() {
        // Now that the flight ends on the aim point rather than a range cap, the lifetime has to
        // be slack over the whole sight range or it would become the thing that sets the reach.
        double travelled = 0.0D;
        for (int tick = 0; tick < ApacheMissileBallistics.LIFETIME_TICKS; tick++) {
            travelled += ApacheMissileBallistics.speedAt(tick);
        }
        assertTrue(travelled >= ApacheMissileBallistics.SIGHT_RANGE_BLOCKS,
                "a missile living its whole life must be able to cross the sight range: "
                        + travelled);
    }

    @Test
    void theAimPointSitsWhereTheSightLineEnds() {
        Vector3 eye = new Vector3(10.0D, 20.0D, 30.0D);
        ApacheMissileBallistics.AimPoint open = ApacheMissileBallistics.aimPoint(
                eye, FORWARD, ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, Double.NaN);
        assertEquals(ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, open.distanceBlocks(), EPSILON,
                "over open sky the aim point is the end of the sight range");
        assertEquals(30.0D + ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, open.point().z(), EPSILON);

        ApacheMissileBallistics.AimPoint onGround = ApacheMissileBallistics.aimPoint(
                eye, FORWARD, ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, 12.0D);
        assertEquals(12.0D, onGround.distanceBlocks(), EPSILON,
                "a block ahead pulls the aim point in to where it was hit");
        assertEquals(42.0D, onGround.point().z(), EPSILON);
    }

    @Test
    void theAimPointNeverReachesPastTheSightRange() {
        // A raycast can only report a hit inside the range it was cast over, but the geometry
        // must not trust the caller: clamping here keeps the reach honest.
        ApacheMissileBallistics.AimPoint clamped = ApacheMissileBallistics.aimPoint(
                new Vector3(0.0D, 0.0D, 0.0D), FORWARD,
                ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, 500.0D);
        assertEquals(ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, clamped.distanceBlocks(), EPSILON);
    }

    @Test
    void aNonsenseAimPointStillFliesForward() {
        ApacheMissileBallistics.AimPoint zero = ApacheMissileBallistics.aimPoint(
                new Vector3(0.0D, 0.0D, 0.0D), new Vector3(0.0D, 0.0D, 0.0D),
                Double.NaN, Double.NaN);
        assertEquals(0.0D, zero.point().x(), EPSILON);
        assertEquals(ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, zero.point().z(), EPSILON,
                "a zero sight line falls back to straight ahead rather than blowing up");
        assertEquals(ApacheMissileBallistics.SIGHT_RANGE_BLOCKS, zero.distanceBlocks(), EPSILON);
    }

    @Test
    void arrivingOnTheAimPointIsWhatFusesTheWarhead() {
        Vector3 aim = new Vector3(0.0D, 0.0D, 40.0D);
        assertFalse(ApacheMissileBallistics.reachedAimPoint(
                new Vector3(0.0D, 0.0D, 0.0D), aim,
                ApacheMissileBallistics.REACH_RADIUS_BLOCKS), "still on its way");
        assertFalse(ApacheMissileBallistics.reachedAimPoint(
                new Vector3(0.0D, 0.0D, 37.0D), aim,
                ApacheMissileBallistics.REACH_RADIUS_BLOCKS), "outside the reach radius");
        assertTrue(ApacheMissileBallistics.reachedAimPoint(
                new Vector3(0.0D, 0.0D, 39.0D), aim,
                ApacheMissileBallistics.REACH_RADIUS_BLOCKS), "inside it");
        assertTrue(ApacheMissileBallistics.reachedAimPoint(aim, aim,
                ApacheMissileBallistics.REACH_RADIUS_BLOCKS), "dead on it");
    }

    @Test
    void aDirectHitIsMeantToTakeTheRotorOff() {
        assertTrue(ApacheMissileBallistics.directHitDamage(120.0F) > 120.0F,
                "a full health Apache has to die to one hit");
        assertTrue(ApacheMissileBallistics.directHitDamage(80.0F) > 80.0F,
                "and so does a full health Little Bird");
        assertTrue(ApacheMissileBallistics.directHitDamage(1.0F) > 1.0F);
        assertEquals(1.0F, ApacheMissileBallistics.directHitDamage(0.0F), EPSILON,
                "even a wreck cannot be left standing by a zero damage rule");
    }

    @Test
    void theLifetimeIsTheOnlyBackstopLeft() {
        assertFalse(ApacheMissileBallistics.expired(
                ApacheMissileBallistics.LIFETIME_TICKS - 1));
        assertTrue(ApacheMissileBallistics.expired(ApacheMissileBallistics.LIFETIME_TICKS));
    }
}
