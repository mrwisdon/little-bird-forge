package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the aiming aid: a straight drop lands where the physics says, and READY is tight. */
final class B2BombAimTest {
    private static final double GROUND = 64.0D;

    @Test
    void ballisticDropLandsAheadByTheLeadDistance() {
        double[] impact = B2BombAim.predictImpact(
                new double[]{0.0D, GROUND + 100.0D, 0.0D},
                B2BombAim.inheritedVelocity(1.0D, 0.0D, 0.0D),
                BombKind.JDAM, null, false, GROUND);
        assertNotNull(impact);
        double lead = BombBallistics.dropLeadDistance(1.0D, 100.0D);
        assertEquals(lead, impact[0], 1.5D, "prediction must agree with the documented lead");
        assertEquals(0.0D, impact[1], 1.0E-9D);
        assertTrue(impact[2] > 50.0D && impact[2] < 200.0D, "fall time=" + impact[2]);
    }

    @Test
    void guidedRoundPullsTowardsTheDesignation() {
        double[] target = new double[]{60.0D, GROUND, 0.0D};
        double[] ballistic = B2BombAim.predictImpact(
                new double[]{0.0D, GROUND + 100.0D, 0.0D},
                B2BombAim.inheritedVelocity(1.0D, 0.0D, 0.0D), BombKind.JDAM, target, false, GROUND);
        double[] guided = B2BombAim.predictImpact(
                new double[]{0.0D, GROUND + 100.0D, 0.0D},
                B2BombAim.inheritedVelocity(1.0D, 0.0D, 0.0D), BombKind.JDAM, target, true, GROUND);
        assertNotNull(ballistic);
        assertNotNull(guided);
        assertTrue(B2BombAim.missDistance(guided, target[0], target[2])
                < B2BombAim.missDistance(ballistic, target[0], target[2]));
    }

    @Test
    void readyWindowIsInclusiveAndNullIsNeverReady() {
        // impact is {x, z, ticks}: the round lands exactly on the target here ...
        double[] onTarget = new double[]{100.0D, 50.0D, 40.0D};
        // ... and three blocks long here.
        double[] threeBlocksLong = new double[]{103.0D, 50.0D, 40.0D};
        assertTrue(B2BombAim.releaseReady(onTarget, 100.0D, 50.0D, 3.0D));
        assertEquals(0.0D, B2BombAim.missDistance(onTarget, 100.0D, 50.0D), 1.0E-9D);
        assertTrue(B2BombAim.releaseReady(threeBlocksLong, 100.0D, 50.0D, 3.0D),
                "the tolerance is inclusive");
        assertFalse(B2BombAim.releaseReady(threeBlocksLong, 100.0D, 50.0D, 2.9D));
        assertFalse(B2BombAim.releaseReady(threeBlocksLong, 100.0D, 50.0D, -5.0D),
                "a negative tolerance clamps to zero, so a three block miss fails");
        assertTrue(B2BombAim.releaseReady(onTarget, 100.0D, 50.0D, 0.0D));
        assertFalse(B2BombAim.releaseReady(null, 0.0D, 0.0D, 999.0D));
        assertEquals(Double.MAX_VALUE, B2BombAim.missDistance(null, 0.0D, 0.0D), 1.0E-9D);
        assertTrue(B2BombAim.needsAutoRelease(threeBlocksLong, 200.0D, 0.0D, 12.0D));
        assertFalse(B2BombAim.needsAutoRelease(threeBlocksLong, 101.0D, 50.0D, 12.0D));
        assertFalse(B2BombAim.needsAutoRelease(null, 0.0D, 0.0D, 12.0D));
    }

    @Test
    void onePressDesignatesWhenTheSightAlreadyLooksAtTheTarget() {
        assertTrue(B2BombAim.shouldAutoDesignate(false, new double[]{1.0D, 2.0D, 3.0D}));
        assertFalse(B2BombAim.shouldAutoDesignate(true, new double[]{1.0D, 2.0D, 3.0D}));
        assertFalse(B2BombAim.shouldAutoDesignate(false, null));
    }

    @Test
    void releaseVelocityIsTheAircraftsDisplacementNotDeltaMovement() {
        // The server follows the pilot's client, so the position history is the only velocity it
        // can see. getDeltaMovement() there is zero, which is what dropped bombs straight down.
        double[] v = B2BombAim.releaseVelocity(100.0D, 70.0D, -20.0D, 100.9D, 70.02D, -19.4D);
        assertEquals(0.9D, v[0], 1.0E-9D);
        assertEquals(0.02D, v[1], 1.0E-9D);
        assertEquals(0.6D, v[2], 1.0E-9D);
        double[] hover = B2BombAim.releaseVelocity(5.0D, 80.0D, 5.0D, 5.0D, 80.0D, 5.0D);
        assertEquals(0.0D, hover[0], 1.0E-9D);
        assertEquals(0.0D, hover[2], 1.0E-9D);
    }

    @Test
    void aFastAircraftDoesNotDropTheBombOnItsOwnNose() {
        double[] velocity = B2BombAim.releaseVelocity(0.0D, 160.0D, 0.0D, 0.9D, 160.0D, 0.0D);
        double[] release = new double[]{0.9D, 161.0D, 0.0D};
        double[] impact = B2BombAim.predictImpact(release, velocity, BombKind.JDAM, null, false,
                64.0D);
        assertNotNull(impact);
        assertTrue(impact[0] > 30.0D,
                "a fast release must lead the aeroplane, landed at " + impact[0]);
        double[] straightDown = B2BombAim.predictImpact(release, new double[]{0.0D, 0.0D, 0.0D},
                BombKind.JDAM, null, false, 64.0D);
        assertNotNull(straightDown);
        assertTrue(straightDown[0] < 2.0D, "the old zero velocity dropped it on the nose");
    }

    @Test
    void greatHeightStillTerminatesWithinTheBudget() {
        double[] impact = B2BombAim.predictImpact(new double[]{5.0D, GROUND + 300.0D, -5.0D},
                new double[]{0.0D, 0.0D, 0.0D}, BombKind.INCENDIARY, null, false, GROUND);
        assertNotNull(impact);
        assertTrue(impact[2] <= B2BombAim.MAX_TICKS, "ticks=" + impact[2]);
        assertTrue(impact[2] > 200, "a 300 block fall is a long one: ticks=" + impact[2]);
    }

    @Test
    void releaseFromAGlidingAircraftKeepsItsForwardSpeed() {
        double[] fast = B2BombAim.predictImpact(new double[]{0.0D, GROUND + 100.0D, 0.0D},
                B2BombAim.inheritedVelocity(2.0D, 0.0D, 0.0D), BombKind.JDAM, null, false, GROUND);
        double[] slow = B2BombAim.predictImpact(new double[]{0.0D, GROUND + 100.0D, 0.0D},
                B2BombAim.inheritedVelocity(0.5D, 0.0D, 0.0D), BombKind.JDAM, null, false, GROUND);
        assertNotNull(fast);
        assertNotNull(slow);
        assertTrue(fast[0] > slow[0], "faster release must land further ahead");
    }

    @Test
    void earlyReleaseOnlyArmsWhileTheSolutionIsClosing() {
        assertTrue(B2BombAim.shouldArm(80.0D, 120.0D, 12.0D), "closing in must arm");
        assertFalse(B2BombAim.shouldArm(120.0D, 80.0D, 12.0D), "opening up must not arm");
        assertFalse(B2BombAim.shouldArm(80.0D, 80.0D, 12.0D), "a frozen solution must not arm");
        assertFalse(B2BombAim.shouldArm(80.0D, Double.NaN, 12.0D), "no history must not arm");
        assertFalse(B2BombAim.shouldArm(Double.NaN, 80.0D, 12.0D), "no prediction must not arm");
        assertFalse(B2BombAim.shouldArm(11.0D, 40.0D, 12.0D), "already inside the window releases");
    }

    @Test
    void theArmingKeyToleratesAFreshSightButRefusesAnOpeningOne() {
        assertTrue(B2BombAim.canArm(80.0D, Double.NaN, 12.0D),
                "a sight just laid on a target has no history yet and must still arm");
        assertTrue(B2BombAim.canArm(80.0D, 120.0D, 12.0D), "closing in may arm");
        assertTrue(B2BombAim.canArm(80.0D, 80.0D, 12.0D), "a holding solution may arm");
        assertFalse(B2BombAim.canArm(120.0D, 80.0D, 12.0D),
                "an opening solution would never fire, so it must not arm");
        assertFalse(B2BombAim.canArm(11.0D, 40.0D, 12.0D),
                "already inside the window: the caller drops instead of arming");
        assertFalse(B2BombAim.canArm(-1.0D, 40.0D, 12.0D), "no designation, no arming");
        assertFalse(B2BombAim.canArm(Double.NaN, 40.0D, 12.0D), "no prediction, no arming");
    }

    @Test
    void theShippedReleaseToleranceIsThreeBlocks() {
        // The window the crew actually flies with. Pinned so it cannot drift silently.
        assertEquals(3.0D, B2BombAim.RELEASE_TOLERANCE_BLOCKS, 1.0E-9D);
        double[] fourShort = new double[]{100.0D, 0.0D, 4};
        assertEquals(4.0D, B2BombAim.missDistance(fourShort, 100.0D, 4.0D), 1.0E-9D);
        assertFalse(B2BombAim.releaseReady(fourShort, 100.0D, 4.0D,
                B2BombAim.RELEASE_TOLERANCE_BLOCKS), "4 blocks out is not ready in a 3 block window");
        double[] twoShort = new double[]{100.0D, 0.0D, 4};
        assertTrue(B2BombAim.releaseReady(twoShort, 100.0D, 2.0D,
                B2BombAim.RELEASE_TOLERANCE_BLOCKS), "2 blocks out is");
    }

    @Test
    void theReleaseVelocityIsBounded() {
        double[] clamped = B2BombAim.releaseVelocity(0.0D, 0.0D, 0.0D, 1000.0D, 0.0D, 0.0D);
        assertEquals(B2BombAim.MAX_RELEASE_SPEED, Math.hypot(clamped[0], clamped[2]), 1.0E-9D);
        double[] silly = B2BombAim.clampReleaseVelocity(Double.NaN, 0.0D, 0.0D);
        assertEquals(0.0D, silly[0], 1.0E-9D);
        double[] normal = B2BombAim.releaseVelocity(0.0D, 0.0D, 0.0D, 1.5D, 0.0D, 0.0D);
        assertEquals(1.5D, normal[0], 1.0E-9D);
    }

    @Test
    void aSampleIsDividedByTheTicksItCovers() {
        // One tick of motion at cruise, and the same motion reported after a dropped packet: the
        // speed is identical, so a lost packet cannot double the release velocity.
        double[] oneTick = B2BombAim.averageReleaseVelocity(1.5D, 0.0D, 0.0D, 1);
        double[] twoTicks = B2BombAim.averageReleaseVelocity(3.0D, 0.0D, 0.0D, 2);
        assertEquals(1.5D, oneTick[0], 1.0E-9D);
        assertEquals(1.5D, twoTicks[0], 1.0E-9D);
        assertEquals(1.5D, B2BombAim.averageReleaseVelocity(1.5D, 0.0D, 0.0D, 0)[0], 1.0E-9D,
                "a zero tick count must not divide by zero");
        // A non finite component poisons the whole sample rather than launching a nonsense stick.
        double[] notFinite = B2BombAim.averageReleaseVelocity(Double.NaN, 1.5D, 0.0D, 3);
        assertEquals(0.0D, notFinite[0], 1.0E-9D);
        assertEquals(0.0D, notFinite[1], 1.0E-9D);
    }

    @Test
    void thePredictionStartsWhereTheAircraftWillBeAtRelease() {
        // 3 ticks of lead at cruise moves the release point 4.5 blocks along the flight path, which
        // is exactly how far the aeroplane travels while the order is in flight.
        double[] led = B2BombAim.leadReleasePosition(0.0D, 100.0D, 0.0D, 1.5D, 0.0D, 0.0D, 3);
        assertEquals(4.5D, led[0], 1.0E-9D);
        assertEquals(100.0D, led[1], 1.0E-9D);
        double[] none = B2BombAim.leadReleasePosition(0.0D, 100.0D, 0.0D, 1.5D, 0.0D, 0.0D, 0);
        assertEquals(0.0D, none[0], 1.0E-9D);
        double[] silly = B2BombAim.leadReleasePosition(1.0D, 2.0D, 3.0D, Double.NaN, 0.0D, 0.0D, 3);
        assertEquals(1.0D, silly[0], 1.0E-9D, "a nonsense velocity must not move the release point");
        assertEquals(40.0D, B2BombAim.leadReleasePosition(0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 0.0D, 999)[0],
                1.0E-9D, "the lead is bounded");
    }

    @Test
    void theLeadGrowsWithPingAndNeverZero() {
        assertEquals(1, B2BombAim.releaseLeadTicks(0), "even a perfect link costs one server tick");
        assertEquals(1, B2BombAim.releaseLeadTicks(20));
        assertEquals(3, B2BombAim.releaseLeadTicks(100));
        assertEquals(5, B2BombAim.releaseLeadTicks(200));
        assertEquals(1, B2BombAim.releaseLeadTicks(-50), "nonsense ping is treated as zero");
        assertEquals(21 - 1, B2BombAim.releaseLeadTicks(100000), "the lead is capped");
    }
}
