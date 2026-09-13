package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import org.junit.jupiter.api.Test;

final class LittleBirdBallisticsTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void barrelsSitLeftAndRightOfTheCenterline() {
        Vector3 center = new Vector3(0.0D, 0.0D, 0.0D);
        Vector3 right = new Vector3(1.0D, 0.0D, 0.0D);
        Vector3 left = LittleBirdBallistics.muzzle(center, right, 0);
        Vector3 rightGun = LittleBirdBallistics.muzzle(center, right, 1);
        assertEquals(-LittleBirdBallistics.MUZZLE_LATERAL_OFFSET, left.x(), EPSILON);
        assertEquals(LittleBirdBallistics.MUZZLE_LATERAL_OFFSET, rightGun.x(), EPSILON);
        assertEquals(-1.0D, LittleBirdBallistics.barrelSign(0));
        assertEquals(1.0D, LittleBirdBallistics.barrelSign(1));
    }

    @Test
    void muzzleCenterDropsAndPushesForward() {
        Vector3 center = LittleBirdBallistics.muzzleCenter(
                new Vector3(0.0D, 10.0D, 0.0D), new Vector3(0.0D, 0.0D, 1.0D));
        assertEquals(-LittleBirdBallistics.MUZZLE_DROP, center.y() - 10.0D, EPSILON);
        assertEquals(0.0D, LittleBirdBallistics.MUZZLE_DROP, EPSILON,
                "the boresight origin is already at gun height");
        assertEquals(LittleBirdBallistics.MUZZLE_FORWARD, center.z(), EPSILON);
    }

    @Test
    void bothBarrelsConvergeOnTheSamePoint() {
        Vector3 origin = new Vector3(0.0D, 5.0D, 0.0D);
        Vector3 aim = new Vector3(0.0D, 0.0D, 1.0D);
        Vector3 center = LittleBirdBallistics.muzzleCenter(origin, aim);
        Vector3 right = new Vector3(1.0D, 0.0D, 0.0D);
        Vector3 convergence = LittleBirdBallistics.convergencePoint(center, aim);
        Vector3 leftDir = LittleBirdBallistics.convergingDirection(
                LittleBirdBallistics.muzzle(center, right, 0), convergence);
        Vector3 rightDir = LittleBirdBallistics.convergingDirection(
                LittleBirdBallistics.muzzle(center, right, 1), convergence);
        assertEquals(1.0D, leftDir.length(), EPSILON);
        // The left gun aims rightwards, the right gun aims leftwards.
        assertTrue(leftDir.x() > 0.0D, "left barrel should aim right, was " + leftDir.x());
        assertTrue(rightDir.x() < 0.0D, "right barrel should aim left, was " + rightDir.x());
        assertEquals(leftDir.z(), rightDir.z(), 1.0E-4D);
    }

    @Test
    void spreadGrowsWithHeat() {
        assertEquals(LittleBirdBallistics.MIN_SPREAD_DEGREES,
                LittleBirdBallistics.spreadDegrees(0), EPSILON);
        assertEquals(LittleBirdBallistics.MAX_SPREAD_DEGREES,
                LittleBirdBallistics.spreadDegrees(LittleBirdGunCycle.MAX_HEAT), EPSILON);
        double previous = -1.0D;
        for (int heat = 0; heat <= LittleBirdGunCycle.MAX_HEAT; heat += 10) {
            double spread = LittleBirdBallistics.spreadDegrees(heat);
            assertTrue(spread > previous, "spread must increase with heat");
            previous = spread;
        }
    }

    @Test
    void zeroSampleReturnsTheBaseDirection() {
        Vector3 base = new Vector3(0.0D, 0.0D, 1.0D);
        Vector3 result = LittleBirdBallistics.spreadDirection(base, 5.0D, 0.0D, 0.0D);
        assertEquals(base.x(), result.x(), 1.0E-9D);
        assertEquals(base.y(), result.y(), 1.0E-9D);
        assertEquals(base.z(), result.z(), 1.0E-9D);
    }

    @Test
    void spreadAngleMatchesTheRequestedCone() {
        Vector3 base = new Vector3(0.0D, 0.0D, 1.0D);
        Vector3 result = LittleBirdBallistics.spreadDirection(base, 10.0D, 1.0D, 0.0D);
        double dot = base.dot(result);
        assertEquals(Math.cos(Math.toRadians(10.0D)), dot, 1.0E-9D);
        assertEquals(1.0D, result.length(), EPSILON);
    }

    @Test
    void airframesArePricedForDogfights() {
        assertEquals(1.0F, LittleBirdBallistics.damageTo(true, 14.0F, 1.0F), 1.0E-6F,
                "a round costs an airframe a single point");
        assertEquals(10.0F, LittleBirdBallistics.damageTo(true, 40.0F, 10.0F), 1.0E-6F,
                "a rocket costs an airframe ten");
        assertEquals(14.0F, LittleBirdBallistics.damageTo(false, 14.0F, 1.0F), 1.0E-6F,
                "everything else still takes the full round");
        assertEquals(40.0F, LittleBirdBallistics.damageTo(false, 40.0F, 10.0F), 1.0E-6F);
        assertEquals(0.0F, LittleBirdBallistics.damageTo(true, 14.0F, -5.0F), 1.0E-6F,
                "a negative price must never heal the target");
        assertEquals(0.0F, LittleBirdBallistics.damageTo(true, 14.0F, Float.NaN), 1.0E-6F);
    }

    @Test
    void apacheHitsArePricedOnTheApacheRack() {
        assertEquals(7.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.APACHE, 40.0F, 10.0F, 7.0F), 1.0E-6F,
                "an Apache hit takes the Apache price, not the Little Bird one");
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.APACHE, 40.0F, 10.0F, -1.0F), 1.0E-6F,
                "a negative Apache price must never heal the target");
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.APACHE, 40.0F, 10.0F, Float.NaN), 1.0E-6F);
    }

    @Test
    void littleBirdHitsArePricedOnTheLittleBirdRack() {
        assertEquals(10.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.LITTLE_BIRD, 40.0F, 10.0F, 7.0F), 1.0E-6F,
                "a Little Bird hit must ignore the Apache price");
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.LITTLE_BIRD, 40.0F, -5.0F, 7.0F), 1.0E-6F);
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.LITTLE_BIRD, 40.0F, Float.NaN, 7.0F), 1.0E-6F);
    }

    @Test
    void nonAirframeHitsKeepTheAntiPersonnelPrice() {
        assertEquals(40.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.NONE, 40.0F, 10.0F, 7.0F), 1.0E-6F,
                "a body or a block still takes the full round");
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.NONE, -40.0F, 10.0F, 7.0F), 1.0E-6F);
    }

    @Test
    void twoRoundsTakeOnePercentOffTheApacheHull() {
        float perRound = 100.0F * 1.0F / 100.0F / 2.0F;
        assertEquals(0.5F, perRound, 1.0E-6F, "two 7.62mm rounds must cost 1% of a 100 HP hull");
        assertEquals(0.5F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.APACHE, 14.0F, 1.0F, perRound), 1.0E-6F);
        assertEquals(1.0F, perRound * 2.0F, 1.0E-6F,
                "two rounds together must be exactly one point");
        assertEquals(1.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.APACHE, 14.0F, 1.0F, perRound)
                        + LittleBirdBallistics.damageTo(
                                LittleBirdBallistics.Airframe.APACHE, 14.0F, 1.0F, perRound),
                1.0E-6F);
    }

    @Test
    void booleanOverloadStillPricesBothAirframesTheSame() {
        assertEquals(1.0F, LittleBirdBallistics.damageTo(true, 14.0F, 1.0F), 1.0E-6F);
        assertEquals(1.0F, LittleBirdBallistics.damageTo(
                        LittleBirdBallistics.Airframe.LITTLE_BIRD, 14.0F, 1.0F, 1.0F), 1.0E-6F);
        assertEquals(14.0F, LittleBirdBallistics.damageTo(false, 14.0F, 1.0F), 1.0E-6F);
        assertEquals(0.0F, LittleBirdBallistics.damageTo(true, 14.0F, -3.0F), 1.0E-6F);
        assertEquals(0.0F, LittleBirdBallistics.damageTo(true, 14.0F, Float.NaN), 1.0E-6F);
    }

    @Test
    void zeroLengthVectorCannotBeNormalized() {
        assertThrows(IllegalArgumentException.class,
                () -> new Vector3(0.0D, 0.0D, 0.0D).normalize());
    }
}
