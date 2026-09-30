package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The tank's fire control: the flight model, the mil convention and the boundaries. */
final class MainGunFireControlTest {
    private static final double SPEED = MainGunShellBallistics.MUZZLE_SPEED;
    private static final double GRAVITY = MainGunShellBallistics.GRAVITY;
    private static final int LIFE = MainGunShellBallistics.LIFETIME_TICKS;
    private static final MainGunFireControl.Ground FLAT = (x, z) -> 0.0D;

    @Test
    void aLevelShotMatchesTheTextbooksDiscreteDrop() {
        // Muzzle 20 blocks up: after n ticks a level round has fallen g/2 * n * (n+1), so it lands
        // on tick 45 (20.7 blocks) and not on tick 44 (19.8).
        MainGunFireControl.Impact impact = MainGunFireControl.predict(0.0D, 20.0D, 0.0D,
                0.0D, 0.0D, 1.0D, SPEED, GRAVITY, LIFE, FLAT);
        assertTrue(impact.reached());
        assertEquals(45, impact.flightTicks());
        assertEquals(45.0D * SPEED, impact.rangeBlocks(), 1.0E-9D);
        assertEquals(0.02D * 0.5D * 45.0D * 46.0D, impact.dropBlocks(), 1.0E-9D);
        assertEquals(0.0D, impact.y(), 1.0E-9D,
                "the impact sits on the ground, it is not below it");
    }

    @Test
    void aRidgeStopsTheRoundWhereItStands() {
        MainGunFireControl.Ground ridge = (x, z) -> z >= 100.0D ? 40.0D : 0.0D;
        MainGunFireControl.Impact impact = MainGunFireControl.predict(0.0D, 20.0D, 0.0D,
                0.0D, 0.0D, 1.0D, SPEED, GRAVITY, LIFE, ridge);
        assertTrue(impact.reached());
        assertEquals(20, impact.flightTicks());
        assertEquals(100.0D, impact.rangeBlocks(), 1.0E-9D);
        assertEquals(40.0D, impact.y(), 1.0E-9D, "the marker must sit on the ridge, not behind it");
    }

    @Test
    void theTickBudgetIsRespected() {
        MainGunFireControl.Impact impact = MainGunFireControl.predict(0.0D, 20.0D, 0.0D,
                0.0D, 0.0D, 1.0D, SPEED, GRAVITY, 5, FLAT);
        assertFalse(impact.reached());
        assertEquals(5, impact.flightTicks());
        assertEquals(25.0D, impact.rangeBlocks(), 1.0E-9D);
    }

    @Test
    void sillyInputsProduceNoPredictionInsteadOfThrowing() {
        assertFalse(MainGunFireControl.predict(0.0D, 20.0D, 0.0D, 0.0D, 0.0D, 0.0D, SPEED, GRAVITY,
                LIFE, FLAT).reached());
        assertFalse(MainGunFireControl.predict(0.0D, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, GRAVITY,
                LIFE, FLAT).reached());
        assertFalse(MainGunFireControl.predict(0.0D, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, SPEED, 0.0D,
                LIFE, FLAT).reached());
        assertFalse(MainGunFireControl.predict(0.0D, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, SPEED, GRAVITY,
                LIFE, null).reached());
        assertFalse(MainGunFireControl.predict(0.0D, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, SPEED, GRAVITY,
                0, FLAT).reached());
        assertFalse(MainGunFireControl.predict(Double.NaN, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, SPEED,
                GRAVITY, LIFE, FLAT).reached());
        // Ground that never reports a height never stops the round either.
        assertFalse(MainGunFireControl.predict(0.0D, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, SPEED, GRAVITY,
                LIFE, (x, z) -> Double.NaN).reached());
    }

    @Test
    void elevationBuysRangeUntilTheBudgetRunsOut() {
        assertEquals(0.0D, MainGunFireControl.rangeAtElevation(0.0D, SPEED, GRAVITY, LIFE), 1.0E-9D);
        // With time to come down, the range peaks near forty five degrees and falls away past it.
        double twenty = MainGunFireControl.rangeAtElevation(20.0D, SPEED, GRAVITY, 400);
        double fortyFive = MainGunFireControl.rangeAtElevation(45.0D, SPEED, GRAVITY, 400);
        double seventy = MainGunFireControl.rangeAtElevation(70.0D, SPEED, GRAVITY, 400);
        assertTrue(twenty > 0.0D, "twenty degrees must put the round down range");
        assertTrue(fortyFive > twenty, fortyFive + " vs " + twenty);
        assertTrue(seventy < fortyFive, seventy + " vs " + fortyFive);
        // The shell's own sixty tick life is the real limit: fired up, it is still in the air when
        // the budget ends, so the range is just the horizontal distance it managed to fly.
        assertEquals(SPEED * Math.cos(Math.toRadians(30.0D)) * LIFE,
                MainGunFireControl.rangeAtElevation(30.0D, SPEED, GRAVITY, LIFE), 1.0E-6D);
    }

    @Test
    void theMilOffsetFollowsTheSightsOwnConvention() {
        assertEquals(17.4533D, MainGunFireControl.MILS_PER_DEGREE, 1.0E-4D);
        double[] ahead = MainGunFireControl.angularOffsetMils(0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.0D, 0.0D, 100.0D);
        assertEquals(0.0D, ahead[0], 1.0E-6D);
        assertEquals(0.0D, ahead[1], 1.0E-6D);
        double[] below = MainGunFireControl.angularOffsetMils(0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                0.0D, -100.0D, 100.0D);
        assertEquals(45.0D * MainGunFireControl.MILS_PER_DEGREE, below[1], 1.0E-6D,
                "pitch is positive downwards");
        double[] east = MainGunFireControl.angularOffsetMils(0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                10.0D, 0.0D, 100.0D);
        assertTrue(east[0] < 0.0D, "east of a south facing sight is off to the left: " + east[0]);
        double[] wrapped = MainGunFireControl.angularOffsetMils(0.0D, 0.0D, 0.0D, 179.0D, 0.0D,
                0.0D, 0.0D, -100.0D);
        assertEquals(1.0D * MainGunFireControl.MILS_PER_DEGREE, wrapped[0], 1.0E-6D,
                "one degree off, however far the bearing wrapped");
        assertEquals(0.0D, MainGunFireControl.angularOffsetMils(5.0D, 5.0D, 5.0D, 0.0D, 0.0D,
                5.0D, 5.0D, 5.0D)[0], 1.0E-9D);
    }

    @Test
    void aRealShotIsAlwaysBelowTheGunLine() {
        // Whatever the depression, a dart fired along the sight line lands under it: that is the
        // holdover the sight has to tell the crew about.
        for (double pitch = 0.0D; pitch <= 10.0D; pitch += 5.0D) {
            LittleBirdBallistics.Vector3 aim =
                    io.github.littlebird.entity.ground.MainGun.aimDirection(0.0D, pitch);
            MainGunFireControl.Impact impact = MainGunFireControl.predict(0.0D, 30.0D, 0.0D,
                    aim.x(), aim.y(), aim.z(), SPEED, GRAVITY, LIFE, FLAT);
            assertTrue(impact.reached(), "pitch " + pitch);
            double[] offset = MainGunFireControl.angularOffsetMils(0.0D, 30.0D, 0.0D, 0.0D, pitch,
                    impact.x(), impact.y(), impact.z());
            assertTrue(offset[1] > 0.0D,
                    "pitch " + pitch + " put the impact " + offset[1] + " mils off the line");
        }
    }

    @Test
    void aShotFiredUpTheMountsStopExpiresInTheAir() {
        // Thirty degrees up at five blocks a tick: the round is still climbing when its sixty tick
        // life ends, so the fire control reports no impact rather than inventing one.
        LittleBirdBallistics.Vector3 up =
                io.github.littlebird.entity.ground.MainGun.aimDirection(0.0D, -30.0D);
        MainGunFireControl.Impact impact = MainGunFireControl.predict(0.0D, 30.0D, 0.0D,
                up.x(), up.y(), up.z(), SPEED, GRAVITY, LIFE, FLAT);
        assertFalse(impact.reached());
        assertEquals(LIFE, impact.flightTicks());
        assertTrue(impact.y() > 30.0D, "it is still above the muzzle: " + impact.y());
    }
}
