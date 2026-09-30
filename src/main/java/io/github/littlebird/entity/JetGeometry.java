package io.github.littlebird.entity;

/**
 * Unit space dimensions of the F-35E Panther. Follows the same 16-units-per-block convention as
 * the helicopters so the three aircraft share one modelling workflow, but the layout is a
 * fixed-wing fighter: a long fuselage with a delta wing instead of a rotor disc.
 *
 * <p>The real F-35A is about 15.7 m long with a 10.7 m span, so the span-to-length ratio is about
 * 0.68. Scaling the airframe to 11 blocks keeps it in the same size class as the 9.3-block
 * Apache while still reading as a much longer, sleeker airframe.
 */
public final class JetGeometry {
    public static final float UNITS_PER_BLOCK = 16.0F;
    public static final float SCALE = 1.0F;
    /** The model's lowest geometry sits at the entity position, or it renders sunk. */
    public static final float SKID_BOTTOM_UNITS = 0.0F;
    public static final float ROOF_TOP_UNITS = 49.0F;
    /** Underside of the fuselage: the belly line the weapon bays sit on. */
    public static final float BELLY_BOTTOM_UNITS = 6.3F;

    /** The nose, pitot and all. */
    public static final float NOSE_TIP_UNITS = -86.0F;
    /** Tip of the two canted tail fins. */
    public static final float TAIL_TIP_UNITS = 90.0F;
    /** Half-span: the wing tip from the centreline. */
    public static final float WING_TIP_UNITS = 60.0F;

    /** The single seat, forward of the centre of gravity and close to the nose. */
    public static final float PILOT_SEAT_FORWARD_UNITS = 48.0F;
    public static final float PILOT_SEAT_HEIGHT_UNITS = 20.0F;

    /** Nose mounted 25mm cannon: where the muzzle ends up. */
    public static final float CANNON_MUZZLE_FORWARD_UNITS = -70.0F;
    public static final float CANNON_MUZZLE_HEIGHT_UNITS = 10.0F;

    /** Under-wing hardpoints: lateral offset from the centreline. */
    public static final float HARDPOINT_LATERAL_UNITS = 42.0F;
    public static final float HARDPOINT_FORWARD_UNITS = -6.0F;
    public static final float HARDPOINT_HEIGHT_UNITS = 2.0F;

    private static final float AABB_WIDTH_BASE_BLOCKS = 4.0F;
    private static final float AABB_HEIGHT_BASE_BLOCKS = 2.6F;

    private JetGeometry() {
    }

    public static float toScaledBlocks(float units) {
        return units / UNITS_PER_BLOCK * SCALE;
    }

    public static float aabbWidthBlocks() {
        return AABB_WIDTH_BASE_BLOCKS * SCALE;
    }

    public static float aabbHeightBlocks() {
        return AABB_HEIGHT_BASE_BLOCKS * SCALE;
    }

    /** Nose tip to tail tip, in unit space. */
    public static final float AIRFRAME_LENGTH_UNITS = TAIL_TIP_UNITS - NOSE_TIP_UNITS;

    /** Nose tip to tail tip, in blocks. */
    public static float airframeLengthBlocks() {
        return toScaledBlocks(AIRFRAME_LENGTH_UNITS);
    }

    /** Tip to tip across the wings, in blocks. */
    public static float wingSpanBlocks() {
        return toScaledBlocks(WING_TIP_UNITS * 2.0F);
    }

    /** Distance the nose reaches ahead of the origin, in blocks. */
    public static float noseReachAheadBlocks() {
        return toScaledBlocks(-NOSE_TIP_UNITS);
    }

    /** Forward offset of the pilot seat, in blocks: positive is ahead of the origin. */
    public static float seatForwardBlocks() {
        return toScaledBlocks(PILOT_SEAT_FORWARD_UNITS);
    }

    /** Seat height above the entity position, in blocks. */
    public static float seatHeightBlocks() {
        return toScaledBlocks(PILOT_SEAT_HEIGHT_UNITS);
    }

    /** How far the cannon muzzle sits ahead of the origin, in blocks. */
    public static float cannonMuzzleForwardBlocks() {
        return toScaledBlocks(-CANNON_MUZZLE_FORWARD_UNITS);
    }

    /** Height of the cannon muzzle above the entity position, in blocks. */
    public static float cannonMuzzleHeightBlocks() {
        return toScaledBlocks(CANNON_MUZZLE_HEIGHT_UNITS);
    }

    /** Lateral offset of the under-wing hardpoints, in blocks. */
    public static float hardpointLateralBlocks() {
        return toScaledBlocks(HARDPOINT_LATERAL_UNITS);
    }

    /** How far the hardpoint front sits ahead of the origin, in blocks. */
    public static float hardpointForwardBlocks() {
        return toScaledBlocks(-HARDPOINT_FORWARD_UNITS);
    }

    public static float hardpointHeightBlocks() {
        return toScaledBlocks(HARDPOINT_HEIGHT_UNITS);
    }

    /**
     * How far the tail tip sits behind the seat, in blocks. This is the number the third person
     * camera has to beat: vanilla asks for four blocks behind the seat.
     */
    public static float tailReachBehindSeatBlocks() {
        return toScaledBlocks(TAIL_TIP_UNITS) + seatForwardBlocks();
    }
}
