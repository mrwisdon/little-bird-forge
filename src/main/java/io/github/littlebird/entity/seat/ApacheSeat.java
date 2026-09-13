package io.github.littlebird.entity.seat;

/**
 * Seat layout of the AH-64: the rear seat flies the aircraft, the front seat runs the chain
 * gun. Passengers are stored in boarding order, so which seat somebody sits in is just an
 * index and a single "swapped" flag - the V key flips that flag, which lets a lone pilot move
 * to the front seat without touching the passenger list at all. Pure rules.
 */
public final class ApacheSeat {
    /** Rear seat: flight, rocket pods and missiles. */
    public static final int PILOT = 0;
    /** Front seat: the 30mm chain gun turret. */
    public static final int GUNNER = 1;
    public static final int SEATS = 2;

    private ApacheSeat() {
    }

    /** Seat of a passenger, given its index in the passenger list. */
    public static int seatOf(int passengerIndex, boolean swapped) {
        int index = clampIndex(passengerIndex);
        return swapped ? SEATS - 1 - index : index;
    }

    /** Index in the passenger list of whoever sits in the given seat, or -1 for an empty seat. */
    public static int indexOf(int seat, boolean swapped, int passengers) {
        if (seat < 0 || seat >= SEATS || passengers <= 0) {
            return -1;
        }
        int index = swapped ? SEATS - 1 - seat : seat;
        return index < passengers ? index : -1;
    }

    public static boolean isPilot(int seat) {
        return seat == PILOT;
    }

    /** A third rider cannot happen, but if it did it would be sitting in the gunner's lap. */
    private static int clampIndex(int passengerIndex) {
        if (passengerIndex <= 0) {
            return 0;
        }
        return Math.min(passengerIndex, SEATS - 1);
    }
}
