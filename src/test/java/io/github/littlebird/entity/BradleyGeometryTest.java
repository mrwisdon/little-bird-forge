package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.seat.GroundSeat;
import org.junit.jupiter.api.Test;

/** Dimensions and crew layout of the M2A2 Bradley. */
class BradleyGeometryTest {
    private static final double EPS = 1.0E-6D;

    @Test
    void envelopeMatchesTheFrozenSpec() {
        // 6.55 m long, 3.28 m over the skirts, so the hull reaches 3.275 and 1.640625 blocks out.
        assertEquals(3.275D, BradleyGeometry.hullHalfLengthBlocks(), 1.0E-4D);
        assertEquals(1.640625D, BradleyGeometry.hullHalfWidthBlocks(), 1.0E-4D);
        // The hitbox must cover the hull it stands for.
        assertTrue(BradleyGeometry.aabbWidthBlocks() > BradleyGeometry.hullHalfWidthBlocks() * 2.0D,
                "hitbox narrower than the hull");
        assertTrue(BradleyGeometry.aabbHeightBlocks() > BradleyGeometry.toBlocks(
                BradleyGeometry.DECK_HEIGHT_UNITS), "hitbox shorter than the deck");
    }

    @Test
    void deckSitsAboveTheSponsonWhichSitsAboveTheSkirts() {
        assertTrue(BradleyGeometry.BELLY_HEIGHT_UNITS < BradleyGeometry.SKIRT_BOTTOM_UNITS);
        assertTrue(BradleyGeometry.SKIRT_BOTTOM_UNITS < BradleyGeometry.SPONSON_HEIGHT_UNITS);
        assertTrue(BradleyGeometry.SPONSON_HEIGHT_UNITS < BradleyGeometry.DECK_HEIGHT_UNITS);
    }

    @Test
    void turretSitsOnTheDeckWithTheGunBetweenPivotAndRoof() {
        assertTrue(BradleyGeometry.TURRET_PIVOT_HEIGHT_UNITS < BradleyGeometry.DECK_HEIGHT_UNITS,
                "the turret must be bedded into the deck, not floating on it");
        assertTrue(BradleyGeometry.TURRET_PIVOT_HEIGHT_UNITS < BradleyGeometry.GUN_AXIS_HEIGHT_UNITS);
        assertTrue(BradleyGeometry.GUN_AXIS_HEIGHT_UNITS < BradleyGeometry.TURRET_ROOF_UNITS);
        assertTrue(BradleyGeometry.TURRET_PIVOT_FORWARD_UNITS < 0.0F,
                "the ring sits aft of the hull centre");
    }

    @Test
    void the25mmOutReachesTheCoax() {
        assertTrue(BradleyGeometry.BARREL_LENGTH_UNITS > BradleyGeometry.COAX_LENGTH_UNITS);
        // Muzzle ahead of the hull centre: the ring's reach minus its aft offset.
        double muzzle = BradleyGeometry.turretPivotForwardBlocks() + BradleyGeometry.barrelBlocks();
        assertTrue(muzzle > 0.0D, "barrel does not clear the nose line");
    }

    @Test
    void sixRoadWheelsMarchBackFromTheNoseAndThreeReturnRollersRideAbove() {
        assertEquals(6, BradleyGeometry.ROAD_WHEELS_PER_SIDE);
        float previous = Float.MAX_VALUE;
        for (int i = 0; i < BradleyGeometry.ROAD_WHEELS_PER_SIDE; i++) {
            float forward = BradleyGeometry.roadWheelForwardUnits(i);
            assertTrue(forward < previous, "road wheel " + i + " is not behind the one in front");
            previous = forward;
        }
        // Even spacing, matching the 0.80 m pitch of the real suspension.
        float pitch = BradleyGeometry.roadWheelForwardUnits(0)
                - BradleyGeometry.roadWheelForwardUnits(1);
        for (int i = 1; i < BradleyGeometry.ROAD_WHEELS_PER_SIDE - 1; i++) {
            assertEquals(pitch,
                    BradleyGeometry.roadWheelForwardUnits(i)
                            - BradleyGeometry.roadWheelForwardUnits(i + 1),
                    1.0E-3F);
        }
        assertEquals(0.8D, BradleyGeometry.toBlocks(pitch), 0.02D);
        // Three return rollers, all between the sprocket and the idler.
        for (int i = 0; i < 3; i++) {
            float roller = BradleyGeometry.returnRollerForwardUnits(i);
            assertTrue(roller < BradleyGeometry.roadWheelForwardUnits(0)
                    && roller > BradleyGeometry.roadWheelForwardUnits(
                            BradleyGeometry.ROAD_WHEELS_PER_SIDE - 1),
                    "return roller " + i + " is outside the wheel run");
        }
    }

    @Test
    void sixSeatsWithTheDriverAheadLeftAndTheRoofGunnerOnTheTurret() {
        assertEquals(6, BradleyGeometry.seatCount());
        double driverForward = BradleyGeometry.seatForwardBlocks(GroundSeat.BRADLEY_DRIVER);
        double crewForward = BradleyGeometry.seatForwardBlocks(GroundSeat.BRADLEY_CREW_FRONT_LEFT);
        double rearForward = BradleyGeometry.seatForwardBlocks(GroundSeat.BRADLEY_CREW_REAR_RIGHT);
        assertTrue(driverForward > crewForward, "the driver must sit ahead of the dismounts");
        assertTrue(crewForward > rearForward, "the rear bench must be behind the front bench");
        assertTrue(BradleyGeometry.seatLateralBlocks(GroundSeat.BRADLEY_DRIVER) > 0.0D,
                "the driver sits on the vehicle's left");
        assertTrue(BradleyGeometry.seatLateralBlocks(GroundSeat.BRADLEY_GUNNER) < 0.0D,
                "the roof gun sits on the commander's side, the vehicle's right");
        assertTrue(BradleyGeometry.seatHeightBlocks(GroundSeat.BRADLEY_GUNNER)
                        > BradleyGeometry.seatHeightBlocks(GroundSeat.BRADLEY_DRIVER),
                "the roof gunner stands above the driver");
        assertTrue(BradleyGeometry.m2MountHeightBlocks()
                        > BradleyGeometry.toBlocks(BradleyGeometry.TURRET_ROOF_UNITS),
                "the .50 mount stands on the cupola, above the turret roof");
        assertTrue(BradleyGeometry.m2BarrelBlocks() > 1.0D, "the .50 needs a real barrel");
    }

    @Test
    void the25mmPivotsOnItsTrunnionNotOnTheRing() {
        // The gun elevates alone: the trunnion is the barrel axis (the frozen GUN_AXIS_HEIGHT) at
        // the mantlet, so 2.1625 blocks up and 0.6625 ahead of the hull centre, with 1.2375 blocks
        // of barrel left to the muzzle. The old ring-based sum put the muzzle 2.519 blocks out and
        // GUN_AXIS_HEIGHT above the floor without ever moving the barrel.
        assertEquals(BradleyGeometry.GUN_AXIS_HEIGHT_UNITS,
                BradleyGeometry.GUN_TRUNNION_HEIGHT_UNITS, EPS);
        assertEquals(2.1625D, BradleyGeometry.gunTrunnionHeightBlocks(), 1.0E-4D);
        assertEquals(0.6625D, BradleyGeometry.gunTrunnionForwardBlocks(), 1.0E-4D);
        assertEquals(1.2375D, BradleyGeometry.barrelReachBlocks(), 1.0E-4D);
        // Ring to muzzle is unchanged: the trunnion is simply an intermediate station on the way.
        assertEquals(BradleyGeometry.barrelBlocks(),
                BradleyGeometry.gunTrunnionForwardBlocks()
                        - BradleyGeometry.turretPivotForwardBlocks()
                        + BradleyGeometry.barrelReachBlocks(), 1.0E-4D);
    }
}
