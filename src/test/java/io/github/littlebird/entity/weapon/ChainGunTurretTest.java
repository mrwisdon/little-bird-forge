package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ChainGunTurretTest {
    private static final double EPSILON = 1.0E-4D;

    private static ChainGunTurret.Aim aim(double yaw, double pitch) {
        return new ChainGunTurret.Aim(yaw, pitch);
    }

    @Test
    void theGunFollowsTheGunnersLookDirection() {
        // Well inside the mount's stops, so this is the gunner's own look direction verbatim.
        ChainGunTurret.Aim aim = ChainGunTurret.desiredAim(55.0F, -8.0F);
        assertEquals(55.0D, aim.yawDeg(), EPSILON, "the barrel takes the gunner's own yaw");
        assertEquals(-8.0D, aim.pitchDeg(), EPSILON,
                "and the gunner's own pitch, which is negative looking up");
    }

    @Test
    void nobodyInTheFrontSeatParksTheBarrelOnTheNose() {
        ChainGunTurret.Aim parked = ChainGunTurret.parkedAim(210.0F, 12.0F);
        assertEquals(-150.0D, parked.yawDeg(), EPSILON, "wrapped into +/-180");
        assertEquals(12.0D, parked.pitchDeg(), EPSILON,
                "a nose 12 degrees down is inside the mount's depression, so it is taken as is");
        assertEquals(-ChainGunTurret.MAX_ELEVATION_UP_DEG,
                ChainGunTurret.parkedAim(210.0F, -40.0F).pitchDeg(), EPSILON,
                "while a nose pitched up past the stop is clamped to it");
    }

    @Test
    void elevationKeepsTheRealMountsStops() {
        // 11 degrees of elevation up is a pitch of -11; 60 degrees of depression is +60.
        assertEquals(-ChainGunTurret.MAX_ELEVATION_UP_DEG,
                ChainGunTurret.desiredAim(0.0F, -80.0F).pitchDeg(), EPSILON);
        assertEquals(ChainGunTurret.MAX_ELEVATION_DOWN_DEG,
                ChainGunTurret.desiredAim(0.0F, 80.0F).pitchDeg(), EPSILON);
        assertEquals(-10.0D, ChainGunTurret.desiredAim(0.0F, -10.0F).pitchDeg(), EPSILON,
                "inside the stops nothing is touched");
        assertEquals(45.0D, ChainGunTurret.desiredAim(0.0F, 45.0F).pitchDeg(), EPSILON);
    }

    @Test
    void theTurretHoldsTheAimWithNoSlewDelay() {
        ChainGunTurret.State s = ChainGunTurret.State.initial();
        s = ChainGunTurret.advance(s, aim(60.0D, 0.0D), false).state();
        assertEquals(60.0D, s.aimYawDeg(), EPSILON,
                "the barrel is on the gunner's look the tick the mouse moves");
    }

    @Test
    void theTurretTakesTheShortWayRoundTheRing() {
        // Parked at 170 and asked for -170: the held angle wraps across the seam into +/-180,
        // it does not run the long way round to +190.
        ChainGunTurret.State s = new ChainGunTurret.State(170.0D, 0.0D, 0, 0, 0);
        s = ChainGunTurret.advance(s, aim(-170.0D, 0.0D), false).state();
        assertEquals(-170.0D, s.aimYawDeg(), EPSILON,
                "the aim is held wrapped into +/-180");
    }

    @Test
    void sixHundredRoundsPerMinute() {
        ChainGunTurret.State s = ChainGunTurret.State.initial();
        int shots = 0;
        for (int tick = 0; tick < 100; tick++) {
            ChainGunTurret.Step step = ChainGunTurret.advance(s, aim(0.0D, 0.0D), true);
            s = step.state();
            if (step.fire()) {
                shots++;
            }
        }
        assertEquals(50, shots, "ten rounds a second over five seconds");
    }

    @Test
    void theBarrelLocksAfterFiveSecondsAndCoolsDown() {
        ChainGunTurret.State s = ChainGunTurret.State.initial();
        int ticksToLock = 0;
        while (s.lockTicks() <= 0 && ticksToLock < 400) {
            s = ChainGunTurret.advance(s, aim(0.0D, 0.0D), true).state();
            ticksToLock++;
        }
        assertTrue(s.lockTicks() > 0, "five seconds of fire overheats the barrel");
        assertTrue(ticksToLock >= 95 && ticksToLock <= 100,
                "fifty rounds at ten a second is five seconds, took " + ticksToLock);
        assertEquals(ChainGunTurret.MAX_HEAT, s.heat());

        ChainGunTurret.Step locked = ChainGunTurret.advance(s, aim(0.0D, 0.0D), true);
        assertFalse(locked.fire(), "the trigger does nothing while locked");
        assertTrue(locked.state().heat() < s.heat(), "but the barrel is cooling");
    }

    @Test
    void releasingTheTriggerCoolsAndRearms() {
        ChainGunTurret.State s = ChainGunTurret.State.initial();
        for (int tick = 0; tick < 20; tick++) {
            s = ChainGunTurret.advance(s, aim(0.0D, 0.0D), true).state();
        }
        assertTrue(s.heat() > 0);
        for (int tick = 0; tick < 200; tick++) {
            s = ChainGunTurret.advance(s, aim(0.0D, 0.0D), false).state();
        }
        assertEquals(0, s.heat(), "a cold barrel has no heat left");
        assertTrue(ChainGunTurret.advance(s, aim(0.0D, 0.0D), true).fire(),
                "and it fires immediately when asked");
    }

    @Test
    void theBarrelPointsWhereItIsAimed() {
        LittleBirdBallistics.Vector3 forward = ChainGunTurret.aimDirection(0.0D, 0.0D);
        assertEquals(0.0D, forward.x(), 1.0E-9D);
        assertEquals(0.0D, forward.y(), 1.0E-9D);
        assertEquals(1.0D, forward.z(), 1.0E-9D, "straight down the nose at yaw 0");

        // Yaw is Minecraft yaw, so +90 points to the gunner's right, which is -X.
        LittleBirdBallistics.Vector3 right = ChainGunTurret.aimDirection(90.0D, 0.0D);
        assertEquals(-1.0D, right.x(), 1.0E-9D);
        assertEquals(0.0D, right.z(), 1.0E-9D);

        LittleBirdBallistics.Vector3 up = ChainGunTurret.aimDirection(0.0D, -30.0D);
        assertTrue(up.y() > 0.0D, "aiming up must point up, y=" + up.y());

        LittleBirdBallistics.Vector3 down = ChainGunTurret.aimDirection(0.0D, 30.0D);
        assertTrue(down.y() < 0.0D, "aiming down must point down, y=" + down.y());
    }

    @Test
    void theBarrelDirectionIsIndependentOfTheAirframe() {
        // The regression for the reported defect: the whole circle is reachable. Under the old
        // relative-angle scheme everything past +/-86 of the nose clamped and the barrel stopped
        // answering the mouse, which is exactly what "the gun ignores aiming" looked like.
        LittleBirdBallistics.Vector3 left = ChainGunTurret.aimDirection(-90.0D, 0.0D);
        assertEquals(1.0D, left.x(), 1.0E-9D, "the far side of the ring is reachable");
        LittleBirdBallistics.Vector3 behind = ChainGunTurret.aimDirection(180.0D, 0.0D);
        assertEquals(-1.0D, behind.z(), 1.0E-9D, "and so is straight aft");

        // Every aim yaw maps to a distinct direction, so a mouse move always moves the gun.
        double previous = Double.NaN;
        for (double yaw = -175.0D; yaw <= 175.0D; yaw += 5.0D) {
            LittleBirdBallistics.Vector3 direction = ChainGunTurret.aimDirection(yaw, 0.0D);
            double bearing = Math.toDegrees(Math.atan2(-direction.x(), direction.z()));
            assertFalse(bearing == previous, "yaw " + yaw + " produced no change");
            previous = bearing;
        }
    }

    @Test
    void theMuzzleRidesTheAirframeButTheRoundFollowsTheAim() {
        LittleBirdBallistics.Vector3 origin = new LittleBirdBallistics.Vector3(0.0D, 0.0D, 0.0D);
        LittleBirdBallistics.Vector3 forward = new LittleBirdBallistics.Vector3(0.0D, 0.0D, 1.0D);
        LittleBirdBallistics.Vector3 up = new LittleBirdBallistics.Vector3(0.0D, -1.0D, 0.0D);

        LittleBirdBallistics.Vector3 level = ChainGunTurret.muzzleOrigin(
                origin, forward, up, 1.0D, 2.0D, 3.0D, 0.0D, 0.0D);
        LittleBirdBallistics.Vector3 swung = ChainGunTurret.muzzleOrigin(
                origin, forward, up, 1.0D, 2.0D, 3.0D, 90.0D, 0.0D);

        assertEquals(5.0D, level.z(), 1.0E-9D, "2 forward plus a 3 block barrel");
        assertEquals(0.0D, level.x(), 1.0E-9D);
        assertEquals(-1.0D, level.y(), 1.0E-9D, "lifted by the airframe's own up axis");
        assertEquals(2.0D, swung.z(), 1.0E-9D, "a 90 degree swing stops adding reach forward");
        assertEquals(-3.0D, swung.x(), 1.0E-9D, "and lays the barrel out to the gunner's right");
    }

    @Test
    void theAimDirectionIsAlwaysAUnitVector() {
        for (double yaw = -180.0D; yaw <= 180.0D; yaw += 45.0D) {
            for (double pitch = -60.0D; pitch <= 11.0D; pitch += 35.5D) {
                LittleBirdBallistics.Vector3 direction =
                        ChainGunTurret.aimDirection(yaw, pitch);
                assertEquals(1.0D, direction.length(), 1.0E-9D);
            }
        }
        LittleBirdBallistics.Vector3 garbage =
                ChainGunTurret.aimDirection(Double.NaN, Double.NaN);
        assertEquals(1.0D, garbage.length(), 1.0E-9D, "garbage angles must not break the barrel");
    }

    @Test
    void theTurretReportsWhenItIsAgainstTheElevationStop() {
        assertTrue(ChainGunTurret.atElevationStop(ChainGunTurret.MAX_ELEVATION_DOWN_DEG));
        assertTrue(ChainGunTurret.atElevationStop(-ChainGunTurret.MAX_ELEVATION_UP_DEG));
        assertFalse(ChainGunTurret.atElevationStop(0.0D), "centred is not pegged");
        assertFalse(ChainGunTurret.atElevationStop(-10.0D));
        assertFalse(ChainGunTurret.atElevationStop(Double.NaN), "garbage is not pegged");
    }

    @Test
    void spreadGrowsWithHeat() {
        assertEquals(ChainGunTurret.MIN_SPREAD_DEGREES,
                ChainGunTurret.spreadDegrees(0), EPSILON);
        assertEquals(ChainGunTurret.MAX_SPREAD_DEGREES,
                ChainGunTurret.spreadDegrees(ChainGunTurret.MAX_HEAT), EPSILON);
        assertTrue(ChainGunTurret.spreadDegrees(30) > ChainGunTurret.spreadDegrees(10));
    }
}
