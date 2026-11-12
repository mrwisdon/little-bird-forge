package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * JDAM terminal homing and the two warhead rows that go with it: the round leads a moving hull,
 * the incendiary cannot hurt one, and the penetrator is priced against armour rather than flesh.
 */
class JdamTerminalHomingTest {

    private static final double EPS = 1.0E-9D;

    @Test
    void theFallTimeGrowsWithTheDropButNeverCountsBackwards() {
        assertEquals(0.0D, BombBallistics.fallTicksFor(0.0D), EPS);
        assertEquals(0.0D, BombBallistics.fallTicksFor(-12.0D), EPS);
        double low = BombBallistics.fallTicksFor(10.0D);
        double high = BombBallistics.fallTicksFor(100.0D);
        double higher = BombBallistics.fallTicksFor(400.0D);
        assertTrue(low > 0.0D && low < high && high < higher, "fall time is monotone in height");
    }

    @Test
    void aStationaryHullIsSimplyAimedAt() {
        double[] bomb = {0.0D, 120.0D, 0.0D};
        double[] bombVel = {0.0D, -1.0D, 0.0D};
        double[] hull = {30.0D, 64.0D, -12.0D};
        double[] aim = BombBallistics.leadAimPoint(bomb, bombVel, hull, new double[]{0.0D, 0.0D, 0.0D});
        assertArrayEquals(hull, aim, EPS);
    }

    @Test
    void aMovingHullIsLedByItsOwnVelocity() {
        double[] bomb = {0.0D, 100.0D, 0.0D};
        double[] bombVel = {0.0D, -1.25D, 0.0D};
        double[] hull = {20.0D, 64.0D, 0.0D};
        double[] hullVel = {0.2D, 0.0D, -0.1D};
        double[] aim = BombBallistics.leadAimPoint(bomb, bombVel, hull, hullVel);
        double fall = BombBallistics.fallTicksFor(36.0D);
        assertTrue(fall > 0.0D, "there is a fall time to lead by");
        assertEquals(20.0D + 0.2D * fall, aim[0], EPS);
        assertEquals(64.0D, aim[1], EPS);
        assertEquals(-0.1D * fall, aim[2], EPS);
    }

    @Test
    void nullInputsReadAsZeroInsteadOfExploding() {
        assertArrayEquals(new double[]{0.0D, 0.0D, 0.0D},
                BombBallistics.leadAimPoint(null, null, null, null), EPS);
        double[] partial = BombBallistics.leadAimPoint(new double[]{5.0D, 70.0D, 0.0D}, null,
                new double[]{9.0D, 64.0D, 1.0D}, null);
        assertArrayEquals(new double[]{9.0D, 64.0D, 1.0D}, partial, EPS);
    }

    @Test
    void terminalAuthorityPullsHarderThanTheMidCourseOne() {
        double[] pos = {0.0D, 80.0D, 0.0D};
        double[] vel = {0.0D, -1.25D, 0.0D};
        double[] target = {0.0D, 64.0D, 12.0D};
        double[] course = BombBallistics.step(pos, vel, BombKind.JDAM, target, true,
                BombBallistics.GUIDANCE_AUTHORITY);
        double[] terminal = BombBallistics.step(pos, vel, BombKind.JDAM, target, true,
                BombBallistics.TERMINAL_GUIDANCE_AUTHORITY);
        assertTrue(terminal[5] > course[5], "terminal homing bends further towards the target");
        assertTrue(terminal[5] < 12.0D, "and still does not teleport");
    }

    @Test
    void theOverloadLeavesTheDefaultAuthorityAlone() {
        double[] pos = {3.0D, 90.0D, -7.0D};
        double[] vel = {0.4D, -1.0D, 0.1D};
        double[] target = {10.0D, 64.0D, -2.0D};
        assertArrayEquals(BombBallistics.step(pos, vel, BombKind.JDAM, target, true),
                BombBallistics.step(pos, vel, BombKind.JDAM, target, true,
                        BombBallistics.GUIDANCE_AUTHORITY), 1.0E-12D);
    }

    @Test
    void aBrokenAuthorityFallsBackRatherThanFreezingTheRound() {
        double[] pos = {0.0D, 80.0D, 0.0D};
        double[] vel = {0.0D, -1.25D, 0.0D};
        double[] target = {0.0D, 64.0D, 8.0D};
        assertArrayEquals(BombBallistics.step(pos, vel, BombKind.JDAM, target, true,
                        BombBallistics.GUIDANCE_AUTHORITY),
                BombBallistics.step(pos, vel, BombKind.JDAM, target, true, Double.NaN), 1.0E-12D);
    }

    @Test
    void theIncendiaryCannotMarkAGroundVehicle() {
        assertEquals(0.0F, LittleBirdBallistics.damageTo(DamageType.SHELL, ArmorClass.HEAVY_ARMOR,
                LittleBirdBallistics.WeaponDamage.incendiary(90.0F)), 1.0E-6F);
        assertEquals(90.0F, LittleBirdBallistics.damageTo(DamageType.SHELL, ArmorClass.UNARMORED,
                LittleBirdBallistics.WeaponDamage.incendiary(90.0F)), 1.0E-6F);
        assertEquals(90.0F, LittleBirdBallistics.damageTo(DamageType.SHELL, ArmorClass.AIRFRAME,
                LittleBirdBallistics.WeaponDamage.incendiary(90.0F)), 1.0E-6F);
    }

    @Test
    void theShippedRowsMatchTheFrozenNumbers() {
        assertEquals(240.0F, BombKind.JDAM.damage(), 1.0E-6F);
        assertEquals(60.0F, BombKind.JDAM.groundVehicleDamage(), 1.0E-6F);
        assertEquals(0.0F, BombKind.INCENDIARY.groundVehicleDamage(), 1.0E-6F);
        assertEquals(90.0F, BombKind.INCENDIARY.damage(), 1.0E-6F);
    }
}
