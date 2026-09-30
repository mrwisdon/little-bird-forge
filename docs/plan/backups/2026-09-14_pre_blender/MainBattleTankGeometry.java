package io.github.littlebird.entity;

import io.github.littlebird.entity.seat.GroundSeat;

/**
 * Unit space dimensions of the main battle tank (M1A5 style hull: long, wide, low).
 *
 * <p>Same 16 units per block convention as the aircraft: the model is built from these numbers,
 * while the entity itself only needs the hitbox, the turret ring and where the crew sits. The
 * layout is its own - a tank is not a rescaled helicopter - so nothing here is shared with the
 * airframes beyond the units convention.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class MainBattleTankGeometry {
    public static final float UNITS_PER_BLOCK = 16.0F;

    /** Hull: 7 blocks long, 3.5 wide, about 1.75 tall over the tracks (low profile). */
    public static final float HULL_LENGTH_UNITS = 112.0F;
    public static final float HULL_WIDTH_UNITS = 56.0F;
    public static final float TRACK_HEIGHT_UNITS = 8.0F;
    public static final float HULL_TOP_UNITS = 28.0F;

    /** Turret ring: centre of the hull, on the roof. */
    public static final float TURRET_PIVOT_HEIGHT_UNITS = 28.0F;
    /** The 120mm barrel reaches this far past the ring. */
    public static final float BARREL_LENGTH_UNITS = 96.0F;

    private static final float AABB_WIDTH_BLOCKS = 3.4F;
    private static final float AABB_HEIGHT_BLOCKS = 2.6F;

    /** Driver up front on the left, gunner in the turret behind. */
    public static final float DRIVER_SEAT_FORWARD_UNITS = 26.0F;
    public static final float DRIVER_SEAT_LATERAL_UNITS = -12.0F;
    public static final float DRIVER_SEAT_HEIGHT_UNITS = 24.0F;
    public static final float GUNNER_SEAT_FORWARD_UNITS = -2.0F;
    public static final float GUNNER_SEAT_LATERAL_UNITS = 0.0F;
    public static final float GUNNER_SEAT_HEIGHT_UNITS = 28.0F;

    private MainBattleTankGeometry() {
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

    /** How far a seat sits ahead of the entity origin, in blocks (positive is the nose). */
    public static double seatForwardBlocks(int seat) {
        return toBlocks(seat == GroundSeat.TANK_DRIVER
                ? DRIVER_SEAT_FORWARD_UNITS : GUNNER_SEAT_FORWARD_UNITS);
    }

    /** How far a seat sits to the vehicle's right, in blocks. */
    public static double seatLateralBlocks(int seat) {
        return toBlocks(seat == GroundSeat.TANK_DRIVER
                ? DRIVER_SEAT_LATERAL_UNITS : GUNNER_SEAT_LATERAL_UNITS);
    }

    public static double seatHeightBlocks(int seat) {
        return toBlocks(seat == GroundSeat.TANK_DRIVER
                ? DRIVER_SEAT_HEIGHT_UNITS : GUNNER_SEAT_HEIGHT_UNITS);
    }

    public static double turretPivotHeightBlocks() {
        return toBlocks(TURRET_PIVOT_HEIGHT_UNITS);
    }

    public static double barrelBlocks() {
        return toBlocks(BARREL_LENGTH_UNITS);
    }

    // ------------------------------------------------------------------
    // Turret machine guns. Both barrels are fixed to the turret, so a muzzle is the ring plus the
    // barrel's own lateral and height offset in turret space, then the barrel length along the aim.
    // The driver's coaxial gun is the model's M240 (right of the ring, below it); seat two's is the
    // cupola .50 (centred, just above the ring). Keeping the two apart is what stops both seats'
    // tracers leaving the same hole when two players crew the tank.
    // ------------------------------------------------------------------
    public static final float COAX_MG_LATERAL_UNITS = 9.5F;
    public static final float COAX_MG_HEIGHT_UNITS = -9.5F;
    public static final float COAX_MG_BARREL_UNITS = 16.0F;
    public static final float CUPOLA_MG_LATERAL_UNITS = 0.0F;
    public static final float CUPOLA_MG_HEIGHT_UNITS = 1.0F;
    public static final float CUPOLA_MG_BARREL_UNITS = 28.0F;

    /** One turret machine gun's muzzle offsets relative to the turret ring, in blocks. */
    public record MachineGunMuzzle(double lateralBlocks, double heightBlocks, double barrelBlocks) {
    }

    public static MachineGunMuzzle machineGunMuzzle(boolean cupola) {
        if (cupola) {
            return new MachineGunMuzzle(toBlocks(CUPOLA_MG_LATERAL_UNITS),
                    toBlocks(CUPOLA_MG_HEIGHT_UNITS), toBlocks(CUPOLA_MG_BARREL_UNITS));
        }
        return new MachineGunMuzzle(toBlocks(COAX_MG_LATERAL_UNITS),
                toBlocks(COAX_MG_HEIGHT_UNITS), toBlocks(COAX_MG_BARREL_UNITS));
    }
}
