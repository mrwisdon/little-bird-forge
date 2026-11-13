package io.github.littlebird.entity.seat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Seat plans of the ground vehicles. */
class GroundSeatTest {
    @Test
    void seatsFollowBoardingOrder() {
        assertEquals(GroundSeat.TANK_DRIVER, GroundSeat.seatOf(0, GroundSeat.TANK_SEATS));
        assertEquals(GroundSeat.TANK_GUNNER, GroundSeat.seatOf(1, GroundSeat.TANK_SEATS));
        assertEquals(GroundSeat.IFV_PASSENGER_RIGHT, GroundSeat.seatOf(3, GroundSeat.IFV_SEATS));
    }

    @Test
    void aFullVehicleClampsTheLastSeat() {
        assertEquals(GroundSeat.TANK_GUNNER, GroundSeat.seatOf(4, GroundSeat.TANK_SEATS));
    }

    @Test
    void theTankSeatSwitchFlipsTheTwoCrewSeats() {
        assertEquals(GroundSeat.TANK_GUNNER, GroundSeat.tankSeatOf(0, true));
        assertEquals(GroundSeat.TANK_DRIVER, GroundSeat.tankSeatOf(1, true));
        assertEquals(GroundSeat.TANK_DRIVER, GroundSeat.tankSeatOf(0, false));
        assertEquals(1, GroundSeat.tankIndexOf(GroundSeat.TANK_DRIVER, true, 2));
        assertEquals(0, GroundSeat.tankIndexOf(GroundSeat.TANK_GUNNER, true, 2));
        assertEquals(-1, GroundSeat.tankIndexOf(GroundSeat.TANK_DRIVER, true, 1));
        assertEquals(0, GroundSeat.tankIndexOf(GroundSeat.TANK_GUNNER, true, 1));
    }

    @Test
    void operatorSeatsAreIdentified() {
        assertTrue(GroundSeat.isTankDriver(0));
        assertTrue(GroundSeat.isTankGunner(1));
        assertFalse(GroundSeat.isTankDriver(1));
        assertTrue(GroundSeat.isIfvDriver(0));
        assertTrue(GroundSeat.isIfvGunner(1));
        assertTrue(GroundSeat.isIfvPassenger(2));
        assertTrue(GroundSeat.isIfvPassenger(3));
        assertFalse(GroundSeat.isIfvPassenger(1));
    }

    @Test
    void bradleyCarriesSixWithTwoOperators() {
        assertEquals(6, GroundSeat.BRADLEY_SEATS);
        assertEquals(GroundSeat.BRADLEY_DRIVER, GroundSeat.seatOf(0, GroundSeat.BRADLEY_SEATS));
        assertEquals(GroundSeat.BRADLEY_GUNNER, GroundSeat.seatOf(1, GroundSeat.BRADLEY_SEATS));
        assertEquals(GroundSeat.BRADLEY_CREW_FRONT_LEFT,
                GroundSeat.seatOf(2, GroundSeat.BRADLEY_SEATS));
        assertEquals(GroundSeat.BRADLEY_CREW_REAR_RIGHT,
                GroundSeat.seatOf(5, GroundSeat.BRADLEY_SEATS));
        // An over-full vehicle clamps onto the last seat rather than inventing a seventh.
        assertEquals(GroundSeat.BRADLEY_CREW_REAR_RIGHT,
                GroundSeat.seatOf(11, GroundSeat.BRADLEY_SEATS));
    }

    @Test
    void bradleyOperatorsAreTheDriverAndTheRoofGunner() {
        assertTrue(GroundSeat.isBradleyDriver(0));
        assertTrue(GroundSeat.isBradleyGunner(1));
        assertFalse(GroundSeat.isBradleyDriver(1));
        assertFalse(GroundSeat.isBradleyGunner(0));
        for (int seat = 2; seat < GroundSeat.BRADLEY_SEATS; seat++) {
            assertTrue(GroundSeat.isBradleyCrew(seat));
            assertFalse(GroundSeat.isBradleyDriver(seat));
            assertFalse(GroundSeat.isBradleyGunner(seat));
        }
        assertFalse(GroundSeat.isBradleyCrew(1));
    }
}
