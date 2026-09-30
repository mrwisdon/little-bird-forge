package io.github.littlebird.entity;

import io.github.littlebird.entity.seat.GroundSeat;

/**
 * Unit space dimensions of the M2A2 Bradley infantry fighting vehicle.
 *
 * <p>Numbers come from the Blender model's frozen specification (the same file that drove the
 * geometry): 6.55 m hull, 3.28 m over the skirts, 0.43 m ground clearance, 0.61 m road wheels,
 * six road wheels a side, front drive sprocket, rear idler, three return rollers, the turret roof
 * at 2.44 m and the 25mm gun axis at 2.16 m. 16 units = 1 block, and blocks are treated as metres,
 * which is the same assumption the tank and the IFV are built on.
 *
 * <p>Axes, matching the rest of the pack: +Y up, the nose toward +Z, +X the vehicle's left. Seats
 * are expressed forward of the hull centre (positive toward the nose) and lateral of it (positive
 * toward the left).
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class BradleyGeometry {
    public static final float UNITS_PER_BLOCK = 16.0F;

    // ---------------------------------------------------------------- hull
    /** 6.55 m bumper to ramp. */
    public static final float HULL_LENGTH_UNITS = 104.8F;
    /** 3.28 m over the side skirts. */
    public static final float HULL_WIDTH_UNITS = 52.5F;
    /** 0.43 m of ground clearance under the belly. */
    public static final float BELLY_HEIGHT_UNITS = 6.9F;
    /** Lower edge of the six side skirt panels. */
    public static final float SKIRT_BOTTOM_UNITS = 9.3F;
    /** Underside of the sponson, where the skirts hang from. */
    public static final float SPONSON_HEIGHT_UNITS = 18.4F;
    /** Top of the hull deck, where the turret sits. */
    public static final float DECK_HEIGHT_UNITS = 26.9F;
    public static final float HULL_TOP_UNITS = DECK_HEIGHT_UNITS;

    private static final float AABB_WIDTH_BLOCKS = 3.3F;
    private static final float AABB_HEIGHT_BLOCKS = 2.2F;

    // ------------------------------------------------------- running gear
    /** Six road wheels a side, plus a front sprocket, a rear idler and three return rollers. */
    public static final int ROAD_WHEELS_PER_SIDE = 6;
    public static final float WHEEL_RADIUS_UNITS = 4.9F;
    public static final float WHEEL_CENTER_HEIGHT_UNITS = 5.7F;
    public static final float WHEEL_WIDTH_UNITS = 7.8F;
    public static final float SPROCKET_RADIUS_UNITS = 4.7F;
    public static final float IDLER_RADIUS_UNITS = 5.4F;
    public static final float ROLLER_RADIUS_UNITS = 1.5F;
    /** Half the track gauge: the track centre line, 1.325 m off the hull centre line. */
    public static final float TRACK_LATERAL_UNITS = 21.2F;
    public static final float TRACK_WIDTH_UNITS = 8.5F;
    /** Road wheel axle positions, forward of the hull centre, nose first. */
    private static final float[] ROAD_WHEEL_FORWARD = {
            35.5F, 22.7F, 9.9F, -2.9F, -15.7F, -28.5F,
    };
    /** Return rollers sit over the second, third/fourth gap and the fifth wheel. */
    private static final float[] RETURN_ROLLER_FORWARD = {25.6F, 1.6F, -22.4F};

    // -------------------------------------------------------------- turret
    /** Turret ring sits 0.62 m aft of the hull centre and 1.51 m up. */
    public static final float TURRET_PIVOT_FORWARD_UNITS = -9.9F;
    public static final float TURRET_PIVOT_HEIGHT_UNITS = 24.2F;
    public static final float TURRET_ROOF_UNITS = 39.0F;
    /** 25mm M242 axis, 2.16 m up, offset to the vehicle's right. */
    public static final float GUN_AXIS_HEIGHT_UNITS = 34.6F;
    public static final float GUN_LATERAL_UNITS = -5.3F;
    /** Muzzle reach measured from the ring: 2.52 m for the 25mm, 2.24 m for the coax. */
    public static final float BARREL_LENGTH_UNITS = 40.3F;
    public static final float COAX_LENGTH_UNITS = 35.8F;
    public static final float COAX_LATERAL_UNITS = -1.9F;

    // ------------------------------------------------- roof machine gun
    /** The M2 sits on the commander's cupola, right of the turret centre line. */
    public static final float M2_MOUNT_FORWARD_UNITS = -6.7F;
    public static final float M2_MOUNT_LATERAL_UNITS = -6.4F;
    public static final float M2_MOUNT_HEIGHT_UNITS = 44.0F;
    public static final float M2_BARREL_LENGTH_UNITS = 18.2F;

    // --------------------------------------------------------- crew seats
    public static final float DRIVER_SEAT_FORWARD_UNITS = 40.0F;
    public static final float DRIVER_SEAT_LATERAL_UNITS = 13.0F;
    public static final float DRIVER_SEAT_HEIGHT_UNITS = 30.0F;
    public static final float GUNNER_SEAT_FORWARD_UNITS = M2_MOUNT_FORWARD_UNITS;
    public static final float GUNNER_SEAT_LATERAL_UNITS = M2_MOUNT_LATERAL_UNITS;
    public static final float GUNNER_SEAT_HEIGHT_UNITS = 34.0F;
    public static final float CREW_SEAT_FORWARD_UNITS = -34.0F;
    public static final float CREW_SEAT_REAR_FORWARD_UNITS = -54.0F;
    public static final float CREW_SEAT_LATERAL_UNITS = 14.0F;
    public static final float CREW_SEAT_HEIGHT_UNITS = 24.0F;

    private BradleyGeometry() {
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

    /** Half the hull length in blocks: the nose is this far ahead of the entity origin. */
    public static double hullHalfLengthBlocks() {
        return toBlocks(HULL_LENGTH_UNITS) / 2.0D;
    }

    public static double hullHalfWidthBlocks() {
        return toBlocks(HULL_WIDTH_UNITS) / 2.0D;
    }

    public static double turretPivotHeightBlocks() {
        return toBlocks(TURRET_PIVOT_HEIGHT_UNITS);
    }

    public static double turretPivotForwardBlocks() {
        return toBlocks(TURRET_PIVOT_FORWARD_UNITS);
    }

    public static double barrelBlocks() {
        return toBlocks(BARREL_LENGTH_UNITS);
    }

    // ------------------------------------------------------------------
    // Gun trunnion. The 25mm elevates on its own pivot instead of taking the turret with it, so the
    // muzzle is measured from the trunnion rather than from the ring: the barrel axis already has a
    // frozen height (GUN_AXIS_HEIGHT_UNITS) and the trunnion sits at the mantlet's mid plane, 20.5
    // units ahead of the ring, leaving 19.8 units of barrel to the muzzle. Keep these in step with
    // BradleyModel's GUN_TRUNNION_Y / GUN_TRUNNION_Z.
    // ------------------------------------------------------------------

    /** Gun trunnion height above the hull centre: the barrel axis. */
    public static final float GUN_TRUNNION_HEIGHT_UNITS = GUN_AXIS_HEIGHT_UNITS;
    /** Gun trunnion distance ahead of the hull centre: ring offset plus the mantlet's own reach. */
    public static final float GUN_TRUNNION_FORWARD_UNITS = 10.6F;
    /** Barrel reach from the trunnion to the muzzle. */
    public static final float BARREL_REACH_UNITS = 19.8F;

    public static double gunTrunnionHeightBlocks() {
        return toBlocks(GUN_TRUNNION_HEIGHT_UNITS);
    }

    public static double gunTrunnionForwardBlocks() {
        return toBlocks(GUN_TRUNNION_FORWARD_UNITS);
    }

    public static double barrelReachBlocks() {
        return toBlocks(BARREL_REACH_UNITS);
    }

    public static double coaxBlocks() {
        return toBlocks(COAX_LENGTH_UNITS);
    }

    public static double m2BarrelBlocks() {
        return toBlocks(M2_BARREL_LENGTH_UNITS);
    }

    public static double m2MountHeightBlocks() {
        return toBlocks(M2_MOUNT_HEIGHT_UNITS);
    }

    public static int seatCount() {
        return GroundSeat.BRADLEY_SEATS;
    }

    public static double seatForwardBlocks(int seat) {
        return toBlocks(switch (seat) {
            case GroundSeat.BRADLEY_DRIVER -> DRIVER_SEAT_FORWARD_UNITS;
            case GroundSeat.BRADLEY_GUNNER -> GUNNER_SEAT_FORWARD_UNITS;
            case GroundSeat.BRADLEY_CREW_REAR_LEFT, GroundSeat.BRADLEY_CREW_REAR_RIGHT ->
                    CREW_SEAT_REAR_FORWARD_UNITS;
            default -> CREW_SEAT_FORWARD_UNITS;
        });
    }

    public static double seatLateralBlocks(int seat) {
        return toBlocks(switch (seat) {
            case GroundSeat.BRADLEY_DRIVER -> DRIVER_SEAT_LATERAL_UNITS;
            case GroundSeat.BRADLEY_GUNNER -> GUNNER_SEAT_LATERAL_UNITS;
            case GroundSeat.BRADLEY_CREW_FRONT_LEFT, GroundSeat.BRADLEY_CREW_REAR_LEFT ->
                    CREW_SEAT_LATERAL_UNITS;
            default -> -CREW_SEAT_LATERAL_UNITS;
        });
    }

    public static double seatHeightBlocks(int seat) {
        return toBlocks(switch (seat) {
            case GroundSeat.BRADLEY_DRIVER -> DRIVER_SEAT_HEIGHT_UNITS;
            case GroundSeat.BRADLEY_GUNNER -> GUNNER_SEAT_HEIGHT_UNITS;
            default -> CREW_SEAT_HEIGHT_UNITS;
        });
    }

    /** Axle position of road wheel {@code index} (0 = front), forward of the hull centre. */
    public static float roadWheelForwardUnits(int index) {
        return ROAD_WHEEL_FORWARD[Math.max(0, Math.min(ROAD_WHEELS_PER_SIDE - 1, index))];
    }

    public static float returnRollerForwardUnits(int index) {
        return RETURN_ROLLER_FORWARD[Math.max(0, Math.min(RETURN_ROLLER_FORWARD.length - 1, index))];
    }
}
