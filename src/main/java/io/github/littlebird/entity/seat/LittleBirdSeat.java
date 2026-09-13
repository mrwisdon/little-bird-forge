package io.github.littlebird.entity.seat;

/**
 * Seating plan of the Little Bird: the pilot up front, three passengers on the cabin benches
 * behind them.
 *
 * Only the pilot flies or shoots - the server refuses control packets from anyone else (every
 * entry point checks getControllingPassenger) - and only the pilot gets the cockpit camera.
 * Pure rules: no Minecraft classes, so the plan itself is unit tested.
 */
public final class LittleBirdSeat {
    /** Seat 0 is the pilot; the rest are passengers. */
    public static final int PILOT = 0;
    public static final int SEATS = 4;

    private LittleBirdSeat() {
    }

    public static boolean isPilot(int seat) {
        return seat == PILOT;
    }

    /** Forward offset in blocks: positive is towards the nose. */
    public static double forwardBlocks(int seat) {
        return switch (clampSeat(seat)) {
            case 0 -> 0.30D;
            case 1 -> -0.15D;
            case 2 -> -0.15D;
            default -> -0.70D;
        };
    }

    /** Lateral offset in blocks: positive is to the aircraft's right. */
    public static double lateralBlocks(int seat) {
        return switch (clampSeat(seat)) {
            case 1 -> 0.42D;
            case 2 -> -0.42D;
            default -> 0.0D;
        };
    }

    private static int clampSeat(int seat) {
        return Math.max(0, Math.min(SEATS - 1, seat));
    }
}
