package io.github.littlebird.entity.seat;

/**
 * Seating plans of the ground vehicles. Passengers are stored in boarding order, so a seat is
 * just an index into the passenger list: the tank carries a driver and a gunner, the IFV a driver,
 * a gunner and two dismounts on the rear bench. Only the operator seats do anything.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class GroundSeat {
    // Main battle tank.
    public static final int TANK_DRIVER = 0;
    public static final int TANK_GUNNER = 1;
    public static final int TANK_SEATS = 2;

    // Infantry fighting vehicle.
    public static final int IFV_DRIVER = 0;
    public static final int IFV_GUNNER = 1;
    public static final int IFV_PASSENGER_LEFT = 2;
    public static final int IFV_PASSENGER_RIGHT = 3;
    public static final int IFV_SEATS = 4;

    private GroundSeat() {
    }

    /** Seat index for the nth passenger aboard, clamped into the given seat count. */
    public static int seatOf(int passengerIndex, int seats) {
        if (passengerIndex <= 0) {
            return 0;
        }
        return Math.min(passengerIndex, Math.max(0, seats - 1));
    }

    public static boolean isTankDriver(int seat) {
        return seat == TANK_DRIVER;
    }

    public static boolean isTankGunner(int seat) {
        return seat == TANK_GUNNER;
    }

    public static boolean isIfvDriver(int seat) {
        return seat == IFV_DRIVER;
    }

    public static boolean isIfvGunner(int seat) {
        return seat == IFV_GUNNER;
    }

    /** Passengers on the rear bench: they ride, they do not operate anything. */
    public static boolean isIfvPassenger(int seat) {
        return seat == IFV_PASSENGER_LEFT || seat == IFV_PASSENGER_RIGHT;
    }
}
