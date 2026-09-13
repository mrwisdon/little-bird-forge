package io.github.littlebird.entity;

import io.github.littlebird.entity.seat.GroundSeat;

/**
 * Unit space dimensions of the infantry fighting vehicle (EBAA Wildcat style: shorter, lighter
 * and taller-seated than the tank, with a two man crew up front and a two man dismount bench in
 * the back).
 *
 * <p>Deliberately its own class rather than a scaled tank: the IFV is a smaller hull with a
 * different crew layout, and keeping the two geometries apart is what makes the models and the
 * hitboxes read as two different vehicles.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class InfantryFightingVehicleGeometry {
    public static final float UNITS_PER_BLOCK = 16.0F;

    /** Hull: 6 blocks long, 3 wide, about 2 tall. */
    public static final float HULL_LENGTH_UNITS = 96.0F;
    public static final float HULL_WIDTH_UNITS = 48.0F;
    public static final float WHEEL_HEIGHT_UNITS = 10.0F;
    public static final float HULL_TOP_UNITS = 30.0F;

    /** Small one man turret on the roof. */
    public static final float TURRET_PIVOT_HEIGHT_UNITS = 30.0F;
    public static final float BARREL_LENGTH_UNITS = 40.0F;

    private static final float AABB_WIDTH_BLOCKS = 3.0F;
    private static final float AABB_HEIGHT_BLOCKS = 2.0F;

    /** Driver front left, gunner front right, two dismounts on the rear bench. */
    public static final float DRIVER_SEAT_FORWARD_UNITS = 22.0F;
    public static final float DRIVER_SEAT_LATERAL_UNITS = -10.0F;
    public static final float DRIVER_SEAT_HEIGHT_UNITS = 22.0F;
    public static final float GUNNER_SEAT_FORWARD_UNITS = 4.0F;
    public static final float GUNNER_SEAT_LATERAL_UNITS = 10.0F;
    public static final float GUNNER_SEAT_HEIGHT_UNITS = 26.0F;
    public static final float PASSENGER_SEAT_FORWARD_UNITS = -26.0F;
    public static final float PASSENGER_SEAT_LATERAL_UNITS = 10.0F;
    public static final float PASSENGER_SEAT_HEIGHT_UNITS = 18.0F;

    private InfantryFightingVehicleGeometry() {
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
        return toBlocks(switch (seat) {
            case GroundSeat.IFV_DRIVER -> DRIVER_SEAT_FORWARD_UNITS;
            case GroundSeat.IFV_GUNNER -> GUNNER_SEAT_FORWARD_UNITS;
            default -> PASSENGER_SEAT_FORWARD_UNITS;
        });
    }

    public static double seatLateralBlocks(int seat) {
        return toBlocks(switch (seat) {
            case GroundSeat.IFV_DRIVER -> DRIVER_SEAT_LATERAL_UNITS;
            case GroundSeat.IFV_GUNNER, GroundSeat.IFV_PASSENGER_LEFT ->
                    PASSENGER_SEAT_LATERAL_UNITS;
            default -> -PASSENGER_SEAT_LATERAL_UNITS;
        });
    }

    public static double seatHeightBlocks(int seat) {
        return toBlocks(switch (seat) {
            case GroundSeat.IFV_DRIVER -> DRIVER_SEAT_HEIGHT_UNITS;
            case GroundSeat.IFV_GUNNER -> GUNNER_SEAT_HEIGHT_UNITS;
            default -> PASSENGER_SEAT_HEIGHT_UNITS;
        });
    }

    public static double turretPivotHeightBlocks() {
        return toBlocks(TURRET_PIVOT_HEIGHT_UNITS);
    }

    public static double barrelBlocks() {
        return toBlocks(BARREL_LENGTH_UNITS);
    }
}
