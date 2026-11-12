package io.github.littlebird.entity;

import io.github.littlebird.entity.seat.GroundSeat;

/**
 * Unit space (16 units per block) dimensions of the PHL-191 8x370mm box launcher, measured from
 * the Blender source model (10.90 m long, 3.06 m wide, 8 tubes in two 2x2 pods).
 *
 * <p>The geometry lives here for the same reason the tank's does: the model, the hitbox, the seat
 * and every muzzle read one set of numbers, so the visible tube and the tube a rocket leaves can
 * never drift apart. Pure rules: no Minecraft classes.
 */
public final class Phl191Geometry {
    public static final float UNITS_PER_BLOCK = 16.0F;

    /** 10.90 m nose to tail. */
    public static final float LENGTH_UNITS = 174.4F;
    /** 3.06 m across the tyres. */
    public static final float WIDTH_UNITS = 48.96F;
    /** 3.06 m to the cab roof. */
    public static final float CAB_TOP_UNITS = 48.96F;
    /** 4.02 m to the top of the pods at the 10 degree travel angle. */
    public static final float POD_TOP_TRAVEL_UNITS = 64.32F;

    /** Trunnion: the pivot the whole launcher elevates about, behind the hull centre. */
    public static final float TRUNNION_FORWARD_UNITS = -48.32F;
    public static final float TRUNNION_HEIGHT_UNITS = 27.2F;

    /** Pod front face (33.6 units = 2.10 m ahead of the hull centre). */
    public static final float POD_FRONT_UNITS = 33.6F;
    public static final float POD_LENGTH_UNITS = 113.6F;
    public static final float POD_WIDTH_UNITS = 19.84F;
    public static final float POD_HEIGHT_UNITS = 20.48F;
    public static final float POD_FLOOR_UNITS = 31.04F;
    /** 0.10 m gap between the two pods. */
    public static final float POD_GAP_UNITS = 1.6F;
    /** 0.56 m between tube centres, both across and up. */
    public static final float TUBE_PITCH_UNITS = 8.96F;

    public static final float DECK_UNITS = 22.08F;

    /** Four axles, nose to tail, x = 3.95 / 1.55 / -1.05 / -3.55 m. */
    public static final float[] AXLE_FORWARD_UNITS = {63.2F, 24.8F, -16.8F, -56.8F};
    public static final float WHEEL_RADIUS_UNITS = 11.6F;
    public static final float WHEEL_WIDTH_UNITS = 7.04F;
    public static final float TRACK_HALF_WIDTH_UNITS = 20.16F;

    /** One seat: the driver works the launcher from the cab. */
    public static final int SEATS = 1;
    public static final int DRIVER = GroundSeat.PHL191_DRIVER;
    public static final float DRIVER_SEAT_FORWARD_UNITS = 64.0F;
    public static final float DRIVER_SEAT_LATERAL_UNITS = 0.0F;
    public static final float DRIVER_SEAT_HEIGHT_UNITS = 34.0F;

    private static final float AABB_WIDTH_BLOCKS = 3.25F;
    private static final float AABB_HEIGHT_BLOCKS = 4.45F;

    private Phl191Geometry() {
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

    /** Height of a tube's centre above the hull floor, in units. */
    public static float tubeHeightUnits(int tube) {
        int row = tube & 1;
        return POD_FLOOR_UNITS + POD_HEIGHT_UNITS * 0.5F
                + (row == 0 ? -0.5F : 0.5F) * TUBE_PITCH_UNITS;
    }

    /** Sideways offset of a tube's centre from the hull centre line, in units (positive is left). */
    public static float tubeLateralUnits(int tube) {
        int pod = (tube >> 2) & 1;
        int column = (tube >> 1) & 1;
        float podCentre = (pod == 0 ? 1.0F : -1.0F)
                * (POD_GAP_UNITS * 0.5F + POD_WIDTH_UNITS * 0.5F);
        return podCentre + (column == 0 ? -0.5F : 0.5F) * TUBE_PITCH_UNITS;
    }

    /**
     * Where a tube's mouth sits for the given launcher elevation, in blocks relative to the entity
     * origin: forward along the hull's nose, lateral to its left, and up. The pod assembly rotates
     * about the trunnion, so the mouth is the tube's rest position swung through that angle rather
     * than a fixed point on the hull.
     */
    public record Muzzle(double forwardBlocks, double lateralBlocks, double upBlocks) {
    }

    public static Muzzle muzzle(int tube, double elevationDeg) {
        int index = Math.max(0, Math.min(7, tube));
        double angle = Math.toRadians(elevationDeg);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        // Tube mouth just ahead of the pod's front face, measured from the trunnion.
        double dx = POD_FRONT_UNITS + 2.0D - TRUNNION_FORWARD_UNITS;
        double dz = tubeHeightUnits(index) - TRUNNION_HEIGHT_UNITS;
        double forward = dx * cos - dz * sin;
        double up = dx * sin + dz * cos;
        return new Muzzle(toBlocks((float) forward) + trunnionForwardBlocks(),
                toBlocks(tubeLateralUnits(index)),
                toBlocks((float) up) + trunnionHeightBlocks());
    }

    /** The launcher's rest attitude while travelling: 10 degrees, nose up. */
    public static final double TRAVEL_ELEVATION_DEG = 10.0D;
}
