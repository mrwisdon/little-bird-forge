package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.Phl191Geometry;
import org.junit.jupiter.api.Test;

final class Phl191BallisticsTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void launchVelocityUsesTheInitialSpeed() {
        Phl191Ballistics.V3 velocity = Phl191Ballistics.launchVelocity(
                new Phl191Ballistics.V3(0.0D, 0.0D, 2.0D));
        assertEquals(Phl191Ballistics.INITIAL_SPEED, velocity.length(), EPSILON);
    }

    @Test
    void directionFollowsMinecraftYawAndElevation() {
        // The launcher never goes below its 5 degree stop, so even "level" is a 5 degree shot.
        double low = Math.toRadians(Phl191Ballistics.MIN_ELEVATION_DEG);
        Phl191Ballistics.V3 forward = Phl191Ballistics.direction(0.0F, 0.0D);
        assertEquals(0.0D, forward.x(), 0.01D);
        assertEquals(Math.sin(low), forward.y(), 0.01D);
        assertEquals(Math.cos(low), forward.z(), 0.01D);

        Phl191Ballistics.V3 east = Phl191Ballistics.direction(90.0F, 0.0D);
        assertEquals(-Math.cos(low), east.x(), 0.01D);
        assertEquals(0.0D, east.z(), 0.01D);

        Phl191Ballistics.V3 up = Phl191Ballistics.direction(0.0F, 45.0D);
        assertEquals(Math.sin(Math.toRadians(45.0D)), up.y(), 0.01D);
        assertEquals(Math.cos(Math.toRadians(45.0D)), up.z(), 0.01D);
    }

    @Test
    void elevationIsClampedToTheLauncherStops() {
        assertEquals(Phl191Ballistics.MIN_ELEVATION_DEG,
                Phl191Ballistics.clampElevation(-30.0D), EPSILON);
        assertEquals(Phl191Ballistics.MAX_ELEVATION_DEG,
                Phl191Ballistics.clampElevation(120.0D), EPSILON);
        assertEquals(30.0D, Phl191Ballistics.clampElevation(30.0D), EPSILON);
    }

    @Test
    void slewMovesAtTheLimitAndStopsOnTarget() {
        double first = Phl191Ballistics.slew(10.0D, 60.0D);
        assertEquals(10.0D + Phl191Ballistics.SLEW_DEGREES_PER_TICK, first, EPSILON);
        double near = Phl191Ballistics.slew(59.0D, 60.0D);
        assertEquals(60.0D, near, EPSILON);
    }

    @Test
    void motorBurnsForTheBurnTicksThenGravityTakesOver() {
        Phl191Ballistics.V3 heading = new Phl191Ballistics.V3(0.0D, 1.0D, 0.0D);
        Phl191Ballistics.V3 velocity = Phl191Ballistics.launchVelocity(heading);
        for (int tick = 0; tick < Phl191Ballistics.BURN_TICKS; tick++) {
            velocity = Phl191Ballistics.advance(velocity, heading, tick);
        }
        // A vertical launch: the motor has pushed the axial speed up to cruise before the burn ends.
        assertTrue(velocity.y() > Phl191Ballistics.INITIAL_SPEED);
        Phl191Ballistics.V3 afterBurn = Phl191Ballistics.advance(
                velocity, heading, Phl191Ballistics.BURN_TICKS);
        assertEquals(velocity.y() - Phl191Ballistics.GRAVITY, afterBurn.y(), EPSILON);
    }

    @Test
    void gravityAccumulatesDownwards() {
        Phl191Ballistics.V3 heading = new Phl191Ballistics.V3(0.0D, 0.0D, 1.0D);
        Phl191Ballistics.V3 velocity = Phl191Ballistics.launchVelocity(heading);
        for (int tick = 0; tick < 30; tick++) {
            velocity = Phl191Ballistics.advance(velocity, heading, tick);
        }
        assertEquals(-30.0D * Phl191Ballistics.GRAVITY, velocity.y(), 0.02D);
    }

    @Test
    void muzzleRisesAndShortensAsTheLauncherElevates() {
        Phl191Geometry.Muzzle level = Phl191Geometry.muzzle(3, 0.0D);
        Phl191Geometry.Muzzle raised = Phl191Geometry.muzzle(3, 45.0D);
        assertTrue(raised.upBlocks() > level.upBlocks());
        assertTrue(raised.forwardBlocks() < level.forwardBlocks());
        // Level, the mouth sits just ahead of the pod face at the tube's own height.
        assertEquals(Phl191Geometry.toBlocks(Phl191Geometry.POD_FRONT_UNITS) + 0.125D,
                level.forwardBlocks(), 0.02D);
    }

    @Test
    void theEightTubesSitAtDistinctMouths() {
        Phl191Geometry.Muzzle first = Phl191Geometry.muzzle(0, 10.0D);
        Phl191Geometry.Muzzle second = Phl191Geometry.muzzle(1, 10.0D);
        Phl191Geometry.Muzzle far = Phl191Geometry.muzzle(7, 10.0D);
        assertNotEquals(first.upBlocks(), second.upBlocks());
        assertNotEquals(first.lateralBlocks(), far.lateralBlocks());
    }

    @Test
    void rangeIsLongestAroundTheOptimalAngle() {
        assertTrue(Phl191Ballistics.rangeAt(45.0D) > Phl191Ballistics.rangeAt(10.0D));
        assertTrue(Phl191Ballistics.rangeAt(45.0D) > Phl191Ballistics.rangeAt(65.0D));
    }

    @Test
    void requiredElevationInvertsTheRangeFunction() {
        double range = Phl191Ballistics.rangeAt(30.0D);
        assertEquals(30.0D, Phl191Ballistics.requiredElevation(range), 1.5D);
    }

    @Test
    void requiredElevationHandlesZeroAndOutOfRange() {
        assertEquals(Phl191Ballistics.MIN_ELEVATION_DEG,
                Phl191Ballistics.requiredElevation(0.0D), 0.01D);
        double far = Phl191Ballistics.requiredElevation(100000.0D);
        assertTrue(Phl191Ballistics.rangeAt(far) >= Phl191Ballistics.rangeAt(10.0D));
        assertTrue(Phl191Ballistics.rangeAt(far) >= Phl191Ballistics.rangeAt(65.0D));
    }

    @Test
    void qteBarSweepsUntilScoredOrTheReloadEnds() {
        assertTrue(Phl191Ballistics.qteActive(Phl191Ballistics.RELOAD_TICKS, false));
        assertTrue(Phl191Ballistics.qteActive(200, false));
        assertFalse(Phl191Ballistics.qteActive(0, false));
        assertFalse(Phl191Ballistics.qteActive(Phl191Ballistics.RELOAD_TICKS, true));
    }

    @Test
    void qteWindowIsOnePointSixToOnePointEightSecondsEverySweep() {
        int reload = Phl191Ballistics.RELOAD_TICKS;
        assertFalse(Phl191Ballistics.qteInWindow(reload));
        assertFalse(Phl191Ballistics.qteInWindow(
                reload - Phl191Ballistics.QTE_WINDOW_START + 1));
        assertTrue(Phl191Ballistics.qteInWindow(reload - Phl191Ballistics.QTE_WINDOW_START));
        assertTrue(Phl191Ballistics.qteInWindow(
                reload - Phl191Ballistics.QTE_WINDOW_END + 1));
        assertFalse(Phl191Ballistics.qteInWindow(reload - Phl191Ballistics.QTE_WINDOW_END));
        // The sweep repeats, so the same band comes round again two seconds later.
        assertTrue(Phl191Ballistics.qteInWindow(reload - Phl191Ballistics.QTE_TICKS
                - Phl191Ballistics.QTE_WINDOW_START));
    }

    @Test
    void qteRewardTakesFifteenSecondsOff() {
        int remaining = Phl191Ballistics.RELOAD_TICKS - Phl191Ballistics.QTE_WINDOW_START;
        assertEquals(remaining - Phl191Ballistics.QTE_REWARD_TICKS,
                Phl191Ballistics.qteReward(remaining));
        assertEquals(Phl191Ballistics.QTE_REWARD_TICKS, 300);
        assertEquals(Phl191Ballistics.QTE_MIN_REMAINING, Phl191Ballistics.qteReward(150));
    }

    @Test
    void reloadIsTwentySeconds() {
        assertEquals(400, Phl191Ballistics.RELOAD_TICKS);
    }

    @Test
    void salvoGridHasFourColumnsAndTwoElevationRows() {
        java.util.Set<String> slots = new java.util.HashSet<>();
        int low = 0;
        int high = 0;
        for (int tube = 0; tube < 8; tube++) {
            double lateral = Phl191Ballistics.salvoLateralOffset(tube, 8, 6.0D, 0.5D);
            double trim = Phl191Ballistics.salvoPitchTrim(tube, 8, 0.4D, 0.5D);
            assertTrue(Math.abs(lateral) <= 6.0D + 1.0E-9D);
            assertTrue(Math.abs(trim) <= 0.4D + 1.0E-9D);
            if (trim > 0.0D) {
                high++;
            } else {
                low++;
            }
            slots.add(String.format("%.2f/%.2f", lateral, trim));
        }
        assertEquals(8, slots.size());
        assertEquals(4, low);
        assertEquals(4, high);
    }

    @Test
    void yawOffsetSlidesTheImpactAcrossTheRequestedSpan() {
        double range = 150.0D;
        double yaw = Phl191Ballistics.yawOffsetForLateral(range, 6.0D);
        assertTrue(Math.abs(Phl191Ballistics.lateralImpact(range, yaw) - 6.0D) < 0.01D);
        // A short shot needs a bigger bearing change for the same sideways step.
        assertTrue(Math.abs(Phl191Ballistics.yawOffsetForLateral(50.0D, 6.0D)) > Math.abs(yaw));
    }

    @Test
    void pitchToleranceCanBeDisabled() {
        assertEquals(0.0D, Phl191Ballistics.salvoPitchTrim(5, 8, 0.0D, 0.5D), 1.0E-9D);
        assertEquals(0.0D, Phl191Ballistics.salvoPitchTrim(5, 4, 0.4D, 0.5D), 1.0E-9D);
    }

    @Test
    void yawToMatchesTheMinecraftConvention() {
        assertEquals(0.0D, Phl191Ballistics.yawTo(0.0D, 10.0D), 1.0E-6D);
        assertEquals(90.0D, Phl191Ballistics.yawTo(-10.0D, 0.0D), 1.0E-6D);
        assertEquals(-90.0D, Phl191Ballistics.yawTo(10.0D, 0.0D), 1.0E-6D);
    }
}
