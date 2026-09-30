package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import org.junit.jupiter.api.Test;

/**
 * Command line-of-sight steering: the round has to follow the operator's sight line, and follow it
 * fast enough that moving the crosshair visibly drags the missile instead of nudging it.
 */
class TowGuidanceTest {
    private static final Vector3 FORWARD = new Vector3(0.0D, 0.0D, 1.0D);
    private static final Vector3 RIGHT = new Vector3(1.0D, 0.0D, 0.0D);

    private static Vector3 fly(Vector3 velocity, Vector3 sight, int ticks, double turnRate) {
        Vector3 current = velocity;
        for (int tick = 1; tick <= ticks; tick++) {
            current = ApacheMissileBallistics.guide(current, sight, tick, turnRate);
        }
        return current;
    }

    private static double degreesOff(Vector3 velocity, Vector3 target) {
        double length = velocity.length();
        double dot = Math.max(-1.0D, Math.min(1.0D, velocity.scale(1.0D / length).dot(target)));
        return Math.toDegrees(Math.acos(dot));
    }

    @Test
    void aRoundLaunchedNinetyDegreesOffSettlesOntoTheSightLine() {
        Vector3 velocity = RIGHT.scale(ApacheMissileBallistics.INITIAL_SPEED);
        assertTrue(degreesOff(velocity, FORWARD) > 89.0D);
        Vector3 flown = fly(velocity, FORWARD, 20, 12.0D);
        assertTrue(degreesOff(flown, FORWARD) < 2.0D,
                "the round never reached the sight line: " + degreesOff(flown, FORWARD));
    }

    @Test
    void aSwingOfTheSightLineVisiblyDragsTheRound() {
        // Launched straight ahead, then the operator swings 90 degrees. This is the whole point of
        // a command line-of-sight weapon, and the exact thing that used to do nothing.
        Vector3 velocity = FORWARD.scale(ApacheMissileBallistics.INITIAL_SPEED);
        Vector3 flown = fly(velocity, RIGHT, 10, 12.0D);
        double off = degreesOff(flown, RIGHT);
        assertTrue(off < 20.0D, "the round barely turned onto the new sight line: " + off);
    }

    @Test
    void aFasterTurnRateConvergesSooner() {
        Vector3 start = RIGHT.scale(ApacheMissileBallistics.INITIAL_SPEED);
        double slow = degreesOff(fly(start, FORWARD, 10, 7.0D), FORWARD);
        double fast = degreesOff(fly(start, FORWARD, 10, 12.0D), FORWARD);
        assertTrue(fast < slow - 10.0D,
                "raising the rate must show: slow=" + slow + " fast=" + fast);
    }

    @Test
    void theOldDefaultRateStillWorks() {
        Vector3 start = RIGHT.scale(ApacheMissileBallistics.INITIAL_SPEED);
        Vector3 implied = ApacheMissileBallistics.guide(start, FORWARD, 1);
        Vector3 explicit = ApacheMissileBallistics.guide(
                start, FORWARD, 1, ApacheMissileBallistics.MAX_TURN_DEGREES_PER_TICK);
        assertEquals(implied.x(), explicit.x(), 1.0E-9D);
        assertEquals(implied.y(), explicit.y(), 1.0E-9D);
        assertEquals(implied.z(), explicit.z(), 1.0E-9D);
    }

    @Test
    void oneTickNeverExceedsTheTurnLimit() {
        Vector3 start = RIGHT.scale(ApacheMissileBallistics.CRUISE_SPEED);
        Vector3 one = fly(start, FORWARD, 1, 12.0D);
        double turned = 90.0D - degreesOff(one, FORWARD);
        assertTrue(turned <= 12.5D, "turned too far in one tick: " + turned);
        assertTrue(turned > 8.0D, "barely turned in one tick: " + turned);
    }

    @Test
    void anUnchangedSightLineKeepsTheRoundOnIt() {
        Vector3 velocity = FORWARD.scale(ApacheMissileBallistics.CRUISE_SPEED);
        Vector3 flown = fly(velocity, FORWARD, 40, 12.0D);
        assertTrue(degreesOff(flown, FORWARD) < 1.0D,
                "the round drifted off a steady sight line: " + degreesOff(flown, FORWARD));
    }

    @Test
    void theRoundFliesStraightThroughTheLaunchPhase() {
        // Borrowed from the reference rover: the round swims out of the tube before the guidance
        // grabs it, so a shot does not visibly whip sideways off the rail.
        Vector3 start = FORWARD.scale(ApacheMissileBallistics.INITIAL_SPEED);
        Vector3 during = ApacheMissileBallistics.guide(start, RIGHT, 3, 12.0D, 6);
        assertEquals(0.0D, degreesOff(during, FORWARD), 1.0E-6D,
                "the round must not turn inside the launch phase");
    }

    @Test
    void guidanceTakesOverOnceTheLaunchPhaseEnds() {
        Vector3 start = FORWARD.scale(ApacheMissileBallistics.INITIAL_SPEED);
        Vector3 after = ApacheMissileBallistics.guide(start, RIGHT, 9, 12.0D, 6);
        double turned = degreesOff(after, FORWARD);
        assertTrue(turned > 5.0D,
                "the round never left its launch heading: " + turned + " degrees");
    }
}
