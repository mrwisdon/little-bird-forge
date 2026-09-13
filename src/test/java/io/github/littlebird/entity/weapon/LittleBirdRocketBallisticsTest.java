package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import org.junit.jupiter.api.Test;

final class LittleBirdRocketBallisticsTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void launchVelocityUsesTheInitialSpeedAndTheAimDirection() {
        Vector3 velocity = LittleBirdRocketBallistics.launchVelocity(
                new Vector3(0.0D, 0.0D, 2.0D));
        assertEquals(LittleBirdRocketBallistics.INITIAL_SPEED, velocity.length(), 1.0E-6D);
        assertEquals(0.0D, velocity.x(), EPSILON);
        assertEquals(LittleBirdRocketBallistics.INITIAL_SPEED, velocity.z(), 1.0E-6D);
    }

    @Test
    void firstTickMatchesTheHandCalculatedStep() {
        Vector3 next = LittleBirdRocketBallistics.advance(
                new Vector3(0.0D, 0.0D, LittleBirdRocketBallistics.INITIAL_SPEED),
                new Vector3(0.0D, 0.0D, 1.0D));
        assertEquals(0.0D, next.x(), EPSILON);
        assertEquals(-LittleBirdRocketBallistics.GRAVITY, next.y(), EPSILON);
        assertEquals(LittleBirdRocketBallistics.INITIAL_SPEED
                + LittleBirdRocketBallistics.accelerationPerTick(), next.z(), EPSILON);
    }

    @Test
    void motorSpinsUpToCruiseAndNeverExceedsIt() {
        Vector3 axis = new Vector3(0.0D, 0.0D, 1.0D);
        Vector3 velocity = LittleBirdRocketBallistics.launchVelocity(axis);
        double previous = velocity.z();
        for (int tick = 0; tick < 60; tick++) {
            velocity = LittleBirdRocketBallistics.advance(velocity, axis);
            assertTrue(velocity.z() >= previous, "forward speed must not fall while boosting");
            assertTrue(velocity.z() <= LittleBirdRocketBallistics.CRUISE_SPEED + 1.0E-6D,
                    "cruise speed is a hard cap, was " + velocity.z());
            previous = velocity.z();
        }
        assertEquals(LittleBirdRocketBallistics.CRUISE_SPEED, velocity.z(), 1.0E-3D,
                "after spin-up the rocket cruises");
    }

    @Test
    void gravityPullsTheRocketDownMonotonically() {
        Vector3 axis = new Vector3(0.0D, 0.0D, 1.0D);
        Vector3 velocity = LittleBirdRocketBallistics.launchVelocity(axis);
        double previous = 0.0D;
        for (int tick = 0; tick < 40; tick++) {
            velocity = LittleBirdRocketBallistics.advance(velocity, axis);
            assertTrue(velocity.y() < previous, "vertical velocity must keep dropping");
            previous = velocity.y();
        }
    }

    @Test
    void zeroLengthHeadingIsTreatedAsForward() {
        Vector3 next = LittleBirdRocketBallistics.advance(
                new Vector3(0.0D, 0.0D, 0.0D), new Vector3(0.0D, 0.0D, 0.0D));
        assertTrue(Double.isFinite(next.x()) && Double.isFinite(next.y()) && Double.isFinite(next.z()));
        assertTrue(next.z() > 0.0D);
    }

    @Test
    void climbHeadingKeepsPushingAlongTheLaunchAxis() {
        Vector3 axis = new Vector3(0.0D, 1.0D, 0.0D);
        Vector3 velocity = LittleBirdRocketBallistics.launchVelocity(axis);
        velocity = LittleBirdRocketBallistics.advance(velocity, axis);
        assertTrue(velocity.y() > 0.0D, "an upward launch keeps climbing against gravity");
        assertEquals(LittleBirdRocketBallistics.INITIAL_SPEED
                        + LittleBirdRocketBallistics.accelerationPerTick()
                        - LittleBirdRocketBallistics.GRAVITY,
                velocity.y(), 1.0E-9D);
    }
}