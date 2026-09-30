package io.github.littlebird.entity.seat;

/**
 * Seating plans of the ground vehicles. Passengers are stored in boarding order, so a seat is
 * just an index into the passenger list: the tank carries a driver and a gunner, the IFV a driver,
 * a gunner and two dismounts on the rear bench, and the Bradley a driver who also works the
 * turret, a roof gunner and four dismounts.
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

    // M2A2 Bradley: six seats. Seat 0 drives the hull and works the turret (25mm autocannon,
    // TOW launcher, smoke wall); seat 1 works the roof machine gun; the other four only ride.
    public static final int BRADLEY_DRIVER = 0;
    public static final int BRADLEY_GUNNER = 1;
    public static final int BRADLEY_CREW_FRONT_LEFT = 2;
    public static final int BRADLEY_CREW_FRONT_RIGHT = 3;
    public static final int BRADLEY_CREW_REAR_LEFT = 4;
    public static final int BRADLEY_CREW_REAR_RIGHT = 5;
    public static final int BRADLEY_SEATS = 6;

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

    public static boolean isBradleyDriver(int seat) {
        return seat == BRADLEY_DRIVER;
    }

    public static boolean isBradleyGunner(int seat) {
        return seat == BRADLEY_GUNNER;
    }

    /** The four dismounts in the back: they ride, they do not operate anything. */
    public static boolean isBradleyCrew(int seat) {
        return seat >= BRADLEY_CREW_FRONT_LEFT && seat <= BRADLEY_CREW_REAR_RIGHT;
    }
}
