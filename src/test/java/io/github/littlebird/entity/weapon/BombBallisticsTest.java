package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.BombBallistics;
import io.github.littlebird.entity.weapon.BombKind;
import org.junit.jupiter.api.Test;

/**
 * The bomb's flight rules. Driven straight through the pure function, with the numbers the entity
 * actually flies (see the class javadoc): a 1 block/tick release from 100 blocks lands 48.3 blocks
 * down range with no guidance, and within a block of a designation inside the seeker's range.
 */
final class BombBallisticsTest {
    private static final double EPS = 1.0E-9D;

    /** One tick of flight, returning {x, y, z, vx, vy, vz}. */
    private static double[] tick(double[] pos, double[] vel, BombKind kind, double[] target,
                                 boolean guided) {
        return BombBallistics.step(pos, vel, kind, target, guided);
    }

    /** Flies until the bomb reaches the ground; returns {x, y, ticks}. */
    private static double[] land(double[] pos, double[] vel, BombKind kind, double[] target,
                                 boolean guided) {
        double[] p = pos.clone();
        double[] v = vel.clone();
        for (int t = 1; t <= 4000; t++) {
            double[] s = tick(p, v, kind, target, guided);
            p = new double[]{s[0], s[1], s[2]};
            v = new double[]{s[3], s[4], s[5]};
            if (p[1] <= 0.0D) {
                return new double[]{p[0], p[1], t};
            }
        }
        return new double[]{p[0], p[1], 4000.0D};
    }

    @Test
    void theStateLayoutIsPositionThenVelocity() {
        double[] s = tick(new double[]{1.0D, 2.0D, 3.0D}, new double[]{0.0D, 0.0D, 1.0D},
                BombKind.JDAM, null, false);
        assertEquals(6, s.length, "position + velocity");
        assertEquals(1.0D, s[0], EPS, "x does not move without vx");
        assertEquals(2.0D + s[4], s[1], EPS, "y moves by the new vy");
        assertEquals(3.0D + s[5], s[2], EPS, "z moves by the new vz");
    }

    @Test
    void stepDoesNotTouchTheCallersArrays() {
        double[] pos = {5.0D, 90.0D, -7.0D};
        double[] vel = {0.5D, 0.0D, -0.25D};
        double[] posCopy = pos.clone();
        double[] velCopy = vel.clone();
        tick(pos, vel, BombKind.JDAM, new double[]{0.0D, 0.0D, 0.0D}, true);
        assertEquals(posCopy[0], pos[0], EPS);
        assertEquals(posCopy[1], pos[1], EPS);
        assertEquals(posCopy[2], pos[2], EPS);
        assertEquals(velCopy[0], vel[0], EPS);
        assertEquals(velCopy[1], vel[1], EPS);
        assertEquals(velCopy[2], vel[2], EPS);
    }

    @Test
    void aBombPicksUpFallSpeedEveryTick() {
        double[] first = tick(new double[]{0.0D, 100.0D, 0.0D}, new double[]{0.0D, 0.0D, 0.0D},
                BombKind.JDAM, null, false);
        assertEquals(-BombBallistics.GRAVITY_BLOCKS_PER_TICK2, first[4], EPS);
        double[] second = tick(new double[]{0.0D, 100.0D, 0.0D},
                new double[]{0.0D, first[4], 0.0D}, BombKind.JDAM, null, false);
        assertTrue(second[4] < first[4], "still accelerating downwards");
        assertEquals(2.0D * -BombBallistics.GRAVITY_BLOCKS_PER_TICK2, second[4], EPS);
    }

    @Test
    void theFallStopsAtTerminalVelocity() {
        double[] s = tick(new double[]{0.0D, 500.0D, 0.0D}, new double[]{0.0D, -9.0D, 0.0D},
                BombKind.JDAM, null, false);
        assertEquals(-BombBallistics.TERMINAL_FALL_PER_TICK, s[4], EPS,
                "a bomb fired downwards cannot fall faster than the air allows");
    }

    @Test
    void forwardSpeedBleedsOffWithDrag() {
        double[] s = tick(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 2.0D},
                BombKind.JDAM, null, false);
        assertEquals(BombKind.JDAM.drag(), s[3], EPS);
        assertEquals(2.0D * BombKind.JDAM.drag(), s[5], EPS);
    }

    @Test
    void theIncendiaryIsBlunterThanTheJdam() {
        assertEquals(0.0D, BombKind.INCENDIARY.drag() <= BombKind.JDAM.drag() ? 0.0D : 1.0D, EPS,
                "the canister keeps less of its speed");
        assertTrue(BombKind.JDAM.guided(), "the JDAM has a seeker");
        assertFalse(BombKind.INCENDIARY.guided(), "the canister has none");
        assertTrue(BombKind.JDAM.blastRadius() > BombKind.INCENDIARY.blastRadius(),
                "the JDAM is the heavy store");
        assertTrue(BombKind.JDAM.damage() > BombKind.INCENDIARY.damage());
    }

    @Test
    void withoutAGuidedFuseTheDesignationIsIgnored() {
        double[] ball = land(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.JDAM, new double[]{60.0D, 0.0D, 0.0D}, false);
        assertEquals(48.299D, ball[0], 0.05D, "the ballistic landing point");
    }

    @Test
    void theReleaseLeadIsTheBallisticLandingDistance() {
        double lead = BombBallistics.dropLeadDistance(1.0D, 100.0D);
        assertEquals(48.25D, lead, 0.1D, "the closed form and the stepped flight must agree");
        double[] ball = land(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.JDAM, null, false);
        assertEquals(ball[0], lead, 0.2D, "the HUD cue is the distance the bomb really travels");
    }

    @Test
    void guidancePullsTheBombOntoTheDesignatedSpot() {
        double[] guided = land(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.JDAM, new double[]{60.0D, 0.0D, 0.0D}, true);
        assertEquals(60.0D, guided[0], 1.0D, "a designation inside the seeker range is hit");
        double[] ball = land(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.JDAM, null, false);
        assertTrue(guided[0] > ball[0] + 5.0D, "guidance has to beat the ballistic fall");
    }

    @Test
    void guidanceIsLimitedToItsRange() {
        double[] far = land(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.JDAM, new double[]{200.0D, 0.0D, 0.0D}, true);
        double[] ball = land(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.JDAM, null, false);
        assertEquals(ball[0], far[0], EPS, "outside " + BombBallistics.GUIDANCE_RANGE_BLOCKS
                + " blocks the round is a dumb bomb");
    }

    @Test
    void guidanceCannotThrowTheBombBackwards() {
        // A designation behind and above: the authority cap means the round slows, never reverses.
        double[] s = tick(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.JDAM, new double[]{-20.0D, 90.0D, 0.0D}, true);
        assertTrue(s[3] > 0.0D, "the bomb must not fly backwards");
        assertTrue(s[3] < 1.0D, "but the seeker did bite");
    }

    @Test
    void anIncendiaryIsNeverSteeredEvenWithAGuidedFuse() {
        double[] steered = tick(new double[]{0.0D, 100.0D, 0.0D}, new double[]{1.0D, 0.0D, 0.0D},
                BombKind.INCENDIARY, new double[]{-20.0D, 90.0D, 0.0D}, true);
        assertEquals(1.0D * BombKind.INCENDIARY.drag(), steered[3], EPS,
                "no seeker means no correction, whatever the fuse says");
    }

    @Test
    void groundContactAndTheWorldFloorDetonate() {
        assertTrue(BombBallistics.shouldDetonate(70.0D, true), "touching down");
        assertFalse(BombBallistics.shouldDetonate(70.0D, false), "still in the air");
        assertTrue(BombBallistics.shouldDetonate(BombBallistics.FLOOR_DETONATE_Y - 1.0D, false),
                "under the world floor");
        assertFalse(BombBallistics.shouldDetonate(BombBallistics.FLOOR_DETONATE_Y + 1.0D, false));
    }

    @Test
    void theLeadGrowsWithAltitudeAndSpeed() {
        assertTrue(BombBallistics.dropLeadDistance(1.0D, 200.0D)
                > BombBallistics.dropLeadDistance(1.0D, 100.0D));
        assertTrue(BombBallistics.dropLeadDistance(2.0D, 100.0D)
                > BombBallistics.dropLeadDistance(1.0D, 100.0D));
        assertEquals(0.0D, BombBallistics.dropLeadDistance(1.0D, 0.0D), EPS,
                "a bomb released on the ground needs no lead");
        assertEquals(0.0D, BombBallistics.dropLeadDistance(0.0D, 100.0D), EPS,
                "a stationary aircraft needs no lead");
    }

    @Test
    void everyKindSurvivesBeingSteppedWithNulls() {
        double[] s = tick(null, null, null, null, true);
        assertEquals(6, s.length);
        assertEquals(-BombBallistics.GRAVITY_BLOCKS_PER_TICK2, s[4], EPS);
    }
}
