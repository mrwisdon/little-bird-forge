package io.github.littlebird.entity;

/**
 * Unit space dimensions of the B-2 Spirit flying wing.
 *
 * <p>Same 16-units-per-block convention as the rest of the pack: 1 block = 1 m, so the
 * Blender source was scaled by S = 240 units / 20.9 m = 11.4833 units per metre and the
 * whole airframe fits a 15 block envelope. The real aircraft is 20.9 m long with a 52.4 m
 * span - a span-to-length ratio of 2.5, because the B-2 is one big lifting surface rather
 * than a fuselage with wings and a tail.
 *
 * <p>Model space (the space B2Model and the OBJ renderer work in): +Y up, -Z nose, +X
 * right, converted from the Blender world (+X right / +Y nose / +Z up) by
 * {@code mc_x = x*S, mc_y = z*S, mc_z = -y*S}.
 *
 * <p>Two different sets of numbers live in this file, and mixing them up leads to a
 * model that does not match the renderer:
 * <ul>
 *   <li>the <b>real-aircraft constants</b> declared below (NOSE_TIP_UNITS, WING_TIP_UNITS,
 *       ROOF_TOP_UNITS, ...) - the frozen contract, derived from the published 20.9 m
 *       length, 52.4 m span and 4.88 m height;</li>
 *   <li>the <b>exported box mesh bbox</b>, which is voxelised into half-metre cells and
 *       therefore measures x +-296, y 0..64, z +-120 units (span 37.0 blocks, height
 *       4.0 blocks): the exporter fills every cell a surface touches, so its bbox runs
 *       slightly large. The OBJ mesh is not voxelised and measures 20.85 x 52.36 x
 *       5.17 m, which is the published shape.</li>
 * </ul>
 */
public final class B2Geometry {
    public static final float UNITS_PER_BLOCK = 16.0F;
    public static final float SCALE = 1.0F;
    /** Blender units per metre of the source model: (15 blocks * 16) / 20.9 m. */
    public static final float UNITS_PER_METRE = 11.4833F;

    /** The model's lowest geometry (the tyre contact patch) sits at the entity position. */
    public static final float SKID_BOTTOM_UNITS = 0.0F;
    /**
     * Nominal roof line: the canopy top, 4.88 m. The exported box mesh quantises to
     * half-metre cells and its top cell reaches 64 units, and the OBJ - the accurate one -
     * tops out at 5.16 m, which is the published 5.18 m height with the gear down.
     */
    public static final float ROOF_TOP_UNITS = 56.0F;
    /** Underside of the centre body: the belly line the weapon bay doors sit on (1.49 m). */
    public static final float BELLY_BOTTOM_UNITS = 17.1F;

    /** The nose, pitot and all. */
    public static final float NOSE_TIP_UNITS = -120.0F;
    /** Trailing edge apex: the wing tips are the rearmost points of a flying wing. */
    public static final float TAIL_TIP_UNITS = 120.0F;
    /** Half-span: the wing tip from the centreline. */
    public static final float WING_TIP_UNITS = 300.87F;

    /**
     * The two crew sit side by side under the canopy, forward of the centre of gravity and
     * about 5 m behind the nose tip.
     */
    public static final float PILOT_SEAT_FORWARD_UNITS = 60.0F;
    public static final float PILOT_SEAT_HEIGHT_UNITS = 32.0F;

    /**
     * Collision box: the same 4 block footprint the two fighters use, so the deployment
     * probe needs no new clearance. The 37.6 block wing overhangs it, exactly the way the
     * fighters' wings overhang theirs.
     *
     * <p>Frustum culling reads this box too, so a wing tip can pop out of view when the
     * centre body leaves the frame. If that ever shows, widen
     * {@code getBoundingBoxForCulling()} in B2Entity - do not grow the collision box to
     * the 37.6 block span, which would ruin the handling.
     */
    private static final float AABB_WIDTH_BASE_BLOCKS = 4.0F;
    private static final float AABB_HEIGHT_BASE_BLOCKS = 3.5F;

    private B2Geometry() {
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

    /** Nose tip to trailing edge apex, in unit space. */
    public static final float AIRFRAME_LENGTH_UNITS = TAIL_TIP_UNITS - NOSE_TIP_UNITS;

    /** Nose tip to trailing edge apex: 15 blocks, the number the OBJ renderer scales to. */
    public static float airframeLengthBlocks() {
        return toScaledBlocks(AIRFRAME_LENGTH_UNITS);
    }

    /** Tip to tip across the wing: 37.6 blocks of mostly wing. */
    public static float wingSpanBlocks() {
        return toScaledBlocks(WING_TIP_UNITS * 2.0F);
    }

    /** Distance the nose reaches ahead of the origin, in blocks. */
    public static float noseReachAheadBlocks() {
        return toScaledBlocks(-NOSE_TIP_UNITS);
    }

    /** Roof line above the ground, in blocks. */
    public static float roofHeightBlocks() {
        return toScaledBlocks(ROOF_TOP_UNITS);
    }

    /** Ground clearance under the belly, in blocks. */
    public static float bellyClearanceBlocks() {
        return toScaledBlocks(BELLY_BOTTOM_UNITS);
    }

    /** Forward offset of the cockpit, in blocks: positive is ahead of the origin. */
    public static float seatForwardBlocks() {
        return toScaledBlocks(PILOT_SEAT_FORWARD_UNITS);
    }

    /** Seat pan above the ground, in blocks. */
    public static float seatHeightBlocks() {
        return toScaledBlocks(PILOT_SEAT_HEIGHT_UNITS);
    }

    /** How far the trailing edge sits behind the seat, in blocks: the third person camera bound. */
    public static float tailReachBehindSeatBlocks() {
        return toScaledBlocks(TAIL_TIP_UNITS) + seatForwardBlocks();
    }
}
