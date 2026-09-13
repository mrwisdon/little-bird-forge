package io.github.littlebird.entity.seat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Seat plans of the two ground vehicles. */
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
}
