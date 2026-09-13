package io.github.littlebird.entity.seat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ApacheSeatTest {

    @Test
    void theFirstToBoardFliesTheAircraft() {
        assertEquals(ApacheSeat.PILOT, ApacheSeat.seatOf(0, false));
        assertEquals(ApacheSeat.GUNNER, ApacheSeat.seatOf(1, false));
        assertTrue(ApacheSeat.isPilot(ApacheSeat.seatOf(0, false)));
    }

    @Test
    void theSwapFlagFlipsBothSeats() {
        assertEquals(ApacheSeat.GUNNER, ApacheSeat.seatOf(0, true));
        assertEquals(ApacheSeat.PILOT, ApacheSeat.seatOf(1, true));
        assertEquals(1, ApacheSeat.indexOf(ApacheSeat.PILOT, true, 2));
        assertEquals(0, ApacheSeat.indexOf(ApacheSeat.GUNNER, true, 2));
    }

    @Test
    void indexOfFindsTheRightPassenger() {
        assertEquals(0, ApacheSeat.indexOf(ApacheSeat.PILOT, false, 2));
        assertEquals(1, ApacheSeat.indexOf(ApacheSeat.GUNNER, false, 2));
        assertEquals(-1, ApacheSeat.indexOf(ApacheSeat.GUNNER, false, 1),
                "a lone passenger leaves the gunner seat empty");
    }

    @Test
    void aLonePassengerCanTakeEitherSeat() {
        // One player aboard: swapping turns them into the gunner, and back.
        assertEquals(ApacheSeat.PILOT, ApacheSeat.seatOf(0, false));
        assertEquals(ApacheSeat.GUNNER, ApacheSeat.seatOf(0, true));
        assertEquals(0, ApacheSeat.indexOf(ApacheSeat.GUNNER, true, 1));
        assertEquals(-1, ApacheSeat.indexOf(ApacheSeat.PILOT, true, 1),
                "with one passenger the pilot seat is the one left empty after a swap");
    }

    @Test
    void outOfRangeInputIsClamped() {
        assertEquals(ApacheSeat.PILOT, ApacheSeat.seatOf(-3, false));
        assertEquals(ApacheSeat.GUNNER, ApacheSeat.seatOf(9, false));
        assertEquals(-1, ApacheSeat.indexOf(ApacheSeat.PILOT, false, 0));
        assertEquals(-1, ApacheSeat.indexOf(9, false, 2));
        assertFalse(ApacheSeat.isPilot(ApacheSeat.GUNNER));
    }
}
