package io.github.littlebird.entity;

import io.github.littlebird.entity.seat.GroundSeat;

/**
 * Unit space (16 units per block) dimensions of the PLZ-05 155mm self-propelled howitzer, taken
 * from the Blender source model (11.60 m long, 3.38 m wide, 3.55 m to the turret roof, one 155mm
 * gun on a six wheel tracked chassis).
 *
 * <p>The geometry lives here for the same reason the tank's does: the model, the hitbox, the seat
 * and the muzzle read one set of numbers, so the drawn barrel and the barrel the shell leaves can
 * never drift apart. Pure rules: no Minecraft classes.
 */
public final class Plz05Geometry {
    public static final float UNITS_PER_BLOCK = 16.0F;

    /** 11.60 m nose to tail. */
    public static final float LENGTH_UNITS = 185.6F;
    /** 3.38 m across the tracks. */
    public static final float WIDTH_UNITS = 54.08F;
    /** 3.55 m to the turret roof. */
    public static final float TOP_UNITS = 56.8F;

    /** Hull deck: 1.95 m above the ground. */
    public static final float DECK_UNITS = 31.2F;
    public static final float HULL_FRONT_UNITS = 58.4F;
    public static final float HULL_REAR_UNITS = -58.4F;

    /** Turret: -3.55 m .. 1.62 m, 3.02 m wide, roof at 3.02 m. */
    public static final float TURRET_FRONT_UNITS = 25.92F;
    public static final float TURRET_REAR_UNITS = -56.8F;
    public static final float TURRET_HALF_WIDTH_UNITS = 24.16F;
    public static final float TURRET_TOP_UNITS = 48.32F;

    /** Gun trunnion: the pivot the 155mm barrel elevates about, in the turret front. */
    public static final float TRUNNION_FORWARD_UNITS = 13.76F;
    /** Gun axis height: 2.42 m. */
    public static final float TRUNNION_HEIGHT_UNITS = 38.72F;
    /** 7.09 m of barrel from the trunnion to the muzzle face. */
    public static final float BARREL_LENGTH_UNITS = 113.44F;
    /** Muzzle face at 7.95 m from the hull centre on the optical axis. */
    public static final float MUZZLE_FORWARD_UNITS = 127.2F;

    /** Six road wheels, x = 2.55 / 1.55 / 0.55 / -0.45 / -1.45 / -2.45 m. */
    public static final float[] AXLE_FORWARD_UNITS = {40.8F, 24.8F, 8.8F, -7.2F, -23.2F, -39.2F};
    public static final float WHEEL_RADIUS_UNITS = 5.76F;
    public static final float WHEEL_WIDTH_UNITS = 3.2F;
    public static final float TRACK_HALF_WIDTH_UNITS = 20.48F;
    /** Drive sprocket at 3.16 m, idler at -3.14 m. */
    public static final float SPROCKET_FORWARD_UNITS = 50.56F;
    public static final float IDLER_FORWARD_UNITS = -50.24F;

    /** One seat: the driver works the howitzer from the hull front. */
    public static final int SEATS = 1;
    public static final int DRIVER = GroundSeat.PLZ05_DRIVER;
    public static final float DRIVER_SEAT_FORWARD_UNITS = 38.4F;
    public static final float DRIVER_SEAT_LATERAL_UNITS = -11.2F;
    public static final float DRIVER_SEAT_HEIGHT_UNITS = 35.2F;

    private static final float AABB_WIDTH_BLOCKS = 3.45F;
    private static final float AABB_HEIGHT_BLOCKS = 3.95F;

    private Plz05Geometry() {
    }

    public static float toBlocks(float units) {
        return units / UNITS_PER_BLOCK;
    }

    public static float aabbWidthBlocks() {
        return AABB_WIDTH_BLOCKS;
    }

    public static float aabbHeightBlocks() {
        return AABB_HEIGHT_BLOCKS;
    }

    public static double seatForwardBlocks(int seat) {
        return toBlocks(DRIVER_SEAT_FORWARD_UNITS);
    }

    public static double seatLateralBlocks(int seat) {
        return toBlocks(DRIVER_SEAT_LATERAL_UNITS);
    }

    public static double seatHeightBlocks(int seat) {
        return toBlocks(DRIVER_SEAT_HEIGHT_UNITS);
    }

    public static double trunnionForwardBlocks() {
        return toBlocks(TRUNNION_FORWARD_UNITS);
    }

    public static double trunnionHeightBlocks() {
        return toBlocks(TRUNNION_HEIGHT_UNITS);
    }

    /** Forward offset of the turret ring centre from the hull centre, in blocks. */
    public static double turretRingForwardBlocks() {
        return toBlocks((TURRET_FRONT_UNITS + TURRET_REAR_UNITS) * 0.5F);
    }

    /**
     * Where the muzzle sits for the given gun elevation, in blocks relative to the entity origin:
     * forward along the hull's nose, lateral to its left, and up. The barrel rotates about the
     * trunnion, so the muzzle is the rest position swung through that angle.
     */
    public record Muzzle(double forwardBlocks, double lateralBlocks, double upBlocks) {
    }

    public static Muzzle muzzle(double elevationDeg) {
        double angle = Math.toRadians(elevationDeg);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double forward = BARREL_LENGTH_UNITS * cos;
        double up = BARREL_LENGTH_UNITS * sin;
        return new Muzzle(toBlocks((float) forward) + trunnionForwardBlocks(), 0.0D,
                toBlocks((float) up) + trunnionHeightBlocks());
    }

    /** The gun's rest attitude while travelling: five degrees, nose up. */
    public static final double TRAVEL_ELEVATION_DEG = 5.0D;
}
