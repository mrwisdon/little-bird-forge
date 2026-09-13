package io.github.littlebird.entity.seat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class LittleBirdSeatTest {

    @Test
    void fourSeatsAndOnlyTheFirstIsThePilot() {
        assertEquals(4, LittleBirdSeat.SEATS);
        assertTrue(LittleBirdSeat.isPilot(0));
        for (int seat = 1; seat < LittleBirdSeat.SEATS; seat++) {
            assertFalse(LittleBirdSeat.isPilot(seat), "seat " + seat + " is a passenger");
        }
    }

    @Test
    void everySeatIsSomewhereDifferent() {
        Set<String> seen = new HashSet<>();
        for (int seat = 0; seat < LittleBirdSeat.SEATS; seat++) {
            String where = LittleBirdSeat.forwardBlocks(seat) + ":" + LittleBirdSeat.lateralBlocks(seat);
            assertTrue(seen.add(where), "seat " + seat + " sits on top of another: " + where);
        }
        assertEquals(LittleBirdSeat.SEATS, seen.size());
    }

    @Test
    void thePilotIsAheadOfThePassengersAndTheBenchesAreLevel() {
        assertTrue(LittleBirdSeat.forwardBlocks(LittleBirdSeat.PILOT)
                > LittleBirdSeat.forwardBlocks(1), "the pilot sits in front");
        assertEquals(0.0D, LittleBirdSeat.lateralBlocks(LittleBirdSeat.PILOT), 1.0E-9D);
        assertEquals(-LittleBirdSeat.lateralBlocks(1), LittleBirdSeat.lateralBlocks(2), 1.0E-9D,
                "the two bench seats mirror each other");
        assertEquals(0.0D, LittleBirdSeat.lateralBlocks(3), 1.0E-9D, "the rear seat is centred");
        assertTrue(LittleBirdSeat.forwardBlocks(3) < LittleBirdSeat.forwardBlocks(1),
                "and it is the rearmost");
    }

    @Test
    void offsetsStayInsideTheCabin() {
        for (int seat = 0; seat < LittleBirdSeat.SEATS; seat++) {
            assertTrue(Math.abs(LittleBirdSeat.lateralBlocks(seat)) <= 0.6D,
                    "a seat outside the cabin would hang in the air: " + seat);
            assertTrue(Math.abs(LittleBirdSeat.forwardBlocks(seat)) <= 1.0D);
        }
        // Out of range seats clamp instead of throwing: a stale passenger count must not crash.
        assertEquals(LittleBirdSeat.forwardBlocks(0), LittleBirdSeat.forwardBlocks(-1), 1.0E-9D);
        assertEquals(LittleBirdSeat.forwardBlocks(3), LittleBirdSeat.forwardBlocks(99), 1.0E-9D);
    }
}
