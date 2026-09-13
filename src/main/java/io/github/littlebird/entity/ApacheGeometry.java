package io.github.littlebird.entity;

import io.github.littlebird.entity.seat.ApacheSeat;

/**
 * Unit space dimensions of the AH-64: the Little Bird's layout scaled up by 40%
 * (0.65 * 1.4 = 0.91) and then stretched along the fuselage, because an attack helicopter that
 * is only as long as a scout helicopter reads as a fat bug next to its own rotor. The real
 * aircraft's airframe (15.06 m) is about as long as its rotor disc (14.63 m); these constants
 * pin the same 1 : 1 ratio. The tandem cockpit is what makes the two seats read as two seats:
 * the pilot sits higher and further back, the gunner lower and further forward.
 *
 * Unit space keeps the same 16 units per block convention as the Little Bird. The part by part
 * model lands with the renderer; this class holds what the entity itself needs (hitbox and seat
 * placement), the layout the model is built from, and the invariants the model has to respect.
 */
public final class ApacheGeometry {
    public static final float UNITS_PER_BLOCK = 16.0F;
    /** 40% bigger than the Little Bird. */
    public static final float SCALE = 0.91F;
    /** The model's lowest geometry sits at the entity position, or it renders sunk. */
    public static final float SKID_BOTTOM_UNITS = 0.0F;
    public static final float ROOF_TOP_UNITS = 40.0F;
    /** Underside of the fuselage: the belly pan, and the line the chin turret hangs below. */
    public static final float BELLY_BOTTOM_UNITS = 8.0F;

    // ------------------------------------------------------------------
    // Airframe layout: the numbers the model is built from.
    // ------------------------------------------------------------------
    /** The nose, sensor turret and all. */
    public static final float NOSE_TIP_UNITS = -70.0F;
    /** Tip of the vertical fin at the back. */
    public static final float TAIL_TIP_UNITS = 94.0F;
    /** Length of the vertical fin, measured forward from the tail tip. */
    public static final float TAIL_FIN_LENGTH_UNITS = 16.0F;
    /** Where the tail boom leaves the fuselage. */
    public static final float TAIL_BOOM_FRONT_UNITS = 4.0F;
    /** Main rotor blade reach from the mast; the disc is twice this. */
    public static final float MAIN_ROTOR_RADIUS_UNITS = 80.0F;
    /** The mast sits slightly forward of the airframe centre, as on the real aircraft. */
    public static final float ROTOR_CENTER_UNITS = -2.0F;

    /** Rear seat: the pilot, higher and behind. */
    public static final float PILOT_SEAT_FORWARD_UNITS = -4.0F;
    public static final float PILOT_SEAT_HEIGHT_UNITS = 10.0F;
    /** Front seat: the gunner, lower and ahead. */
    public static final float GUNNER_SEAT_FORWARD_UNITS = 14.0F;
    public static final float GUNNER_SEAT_HEIGHT_UNITS = 2.0F;

    private static final float AABB_WIDTH_BASE_BLOCKS = 2.6F;
    private static final float AABB_HEIGHT_BASE_BLOCKS = 2.6F;

    private ApacheGeometry() {
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

    /** Outer pylon: the 19 tube rocket pod hangs here. */
    public static final float POD_X_UNITS = 29.5F;
    public static final float POD_FRONT_UNITS = -20.0F;
    /** Inner pylon: the guided missile rail hangs here. */
    public static final float RAIL_X_UNITS = 16.0F;
    public static final float RAIL_FRONT_UNITS = -18.0F;

    /** Chin turret: the pivot the barrel swings around, and how far the muzzle reaches. */
    public static final float TURRET_PIVOT_FORWARD_UNITS = 44.0F;
    /** Below the belly line, so the gun hangs under the nose instead of inside it. */
    public static final float TURRET_PIVOT_HEIGHT_UNITS = 4.0F;
    public static final float TURRET_BARREL_UNITS = 24.0F;

    public static final float POD_Y_UNITS = 12.5F;
    public static final float RAIL_Y_UNITS = 14.0F;

    /** Lateral offset of the rocket pods, in blocks. */
    public static float podLateralBlocks() {
        return toScaledBlocks(POD_X_UNITS);
    }

    /** How far the pod front sits ahead of the origin, in blocks. */
    public static float podForwardBlocks() {
        return toScaledBlocks(-POD_FRONT_UNITS);
    }

    /** Front of the vertical fin, in unit space. */
    public static float tailFinFrontUnits() {
        return TAIL_TIP_UNITS - TAIL_FIN_LENGTH_UNITS;
    }

    /** Length of the tail boom between the fuselage and the fin, in unit space. */
    public static float tailBoomLengthUnits() {
        return tailFinFrontUnits() - TAIL_BOOM_FRONT_UNITS;
    }

    /** Nose tip to tail tip, in unit space. */
    public static final float AIRFRAME_LENGTH_UNITS = TAIL_TIP_UNITS - NOSE_TIP_UNITS;

    /** Nose tip to tail tip, in blocks: what the model actually occupies. */
    public static float airframeLengthBlocks() {
        return toScaledBlocks(AIRFRAME_LENGTH_UNITS);
    }

    /** Diameter of the main rotor disc, in blocks. */
    public static float rotorDiameterBlocks() {
        return toScaledBlocks(MAIN_ROTOR_RADIUS_UNITS * 2.0F);
    }

    /** Airframe length over rotor diameter: the real aircraft sits at about 1.03. */
    public static float airframeToRotorRatio() {
        return airframeLengthBlocks() / rotorDiameterBlocks();
    }

    /** How far the turret pivot sits ahead of the entity origin, in blocks. */
    public static float turretPivotForwardBlocks() {
        return toScaledBlocks(TURRET_PIVOT_FORWARD_UNITS);
    }

    /** How high the turret pivot sits above the entity position, in blocks. */
    public static float turretPivotHeightBlocks() {
        return toScaledBlocks(TURRET_PIVOT_HEIGHT_UNITS);
    }

    /** Length of the chain gun barrel, in blocks: where the muzzle ends up. */
    public static float chainGunBarrelBlocks() {
        return toScaledBlocks(TURRET_BARREL_UNITS);
    }

    /** Height of the rocket pods above the entity position, in blocks. */
    public static float podHeightBlocks() {
        return toScaledBlocks(POD_Y_UNITS);
    }

    /** Height of the missile rails above the entity position, in blocks. */
    public static float railHeightBlocks() {
        return toScaledBlocks(RAIL_Y_UNITS);
    }

    /** Lateral offset of the missile rails, in blocks. */
    public static float railLateralBlocks() {
        return toScaledBlocks(RAIL_X_UNITS);
    }

    /** How far the missile rail front sits ahead of the origin, in blocks. */
    public static float railForwardBlocks() {
        return toScaledBlocks(-RAIL_FRONT_UNITS);
    }

    /**
     * How far the tail tip sits behind the given seat, in blocks. This is the number the third
     * person camera has to beat: vanilla asks for four blocks behind the seat.
     */
    public static float tailReachBehindSeatBlocks(int seat) {
        return toScaledBlocks(TAIL_TIP_UNITS) + seatForwardBlocks(seat);
    }

    /** Forward offset of a seat, in blocks: negative is behind the origin. */
    public static float seatForwardBlocks(int seat) {
        return toScaledBlocks(seat == ApacheSeat.PILOT
                ? PILOT_SEAT_FORWARD_UNITS : GUNNER_SEAT_FORWARD_UNITS);
    }

    /** Seat height above the entity position, in blocks. */
    public static float seatHeightBlocks(int seat) {
        return toScaledBlocks(seat == ApacheSeat.PILOT
                ? PILOT_SEAT_HEIGHT_UNITS : GUNNER_SEAT_HEIGHT_UNITS);
    }
}
