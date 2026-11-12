package io.github.littlebird.entity;

import io.github.littlebird.entity.seat.GroundSeat;

/**
 * Unit space dimensions of the main battle tank (M1A2 SEP v1: long, wide, low).
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

    /**
     * Real-scale M1A2 SEP v1 dimensions, measured from the Blender source model
     * (blender_m1a2sepv1, 16 units per block): 7.89 m hull shell, 3.66 m wide,
     * 2.42 m to the turret roof. See docs/plan/ground-units-model-log.md.
     */
    public static final float HULL_LENGTH_UNITS = 126.24F;
    public static final float HULL_WIDTH_UNITS = 58.56F;
    public static final float TRACK_HEIGHT_UNITS = 20.54F;
    public static final float HULL_TOP_UNITS = 25.28F;

    /** Turret ring: 0.45 m ahead of the hull centre, on the deck (1.58 m). */
    public static final float TURRET_PIVOT_HEIGHT_UNITS = 25.28F;
    /** The turret ring sits this far ahead of the entity origin. */
    public static final float TURRET_RING_FORWARD_UNITS = 7.2F;
    /** The 120mm barrel reaches this far past the ring. */
    public static final float BARREL_LENGTH_UNITS = 85.68F;

    private static final float AABB_WIDTH_BLOCKS = 3.7F;
    private static final float AABB_HEIGHT_BLOCKS = 2.6F;

    /** Driver up front on the left, gunner in the turret behind. */
    public static final float DRIVER_SEAT_FORWARD_UNITS = 48.0F;
    public static final float DRIVER_SEAT_LATERAL_UNITS = 0.0F;
    public static final float DRIVER_SEAT_HEIGHT_UNITS = 24.0F;
    public static final float GUNNER_SEAT_FORWARD_UNITS = 7.2F;
    public static final float GUNNER_SEAT_LATERAL_UNITS = 0.0F;
    public static final float GUNNER_SEAT_HEIGHT_UNITS = 25.28F;

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

    /** How far the ring sits ahead of the entity origin, in blocks. */
    public static double turretRingForwardBlocks() {
        return toBlocks(TURRET_RING_FORWARD_UNITS);
    }

    public static double barrelBlocks() {
        return toBlocks(BARREL_LENGTH_UNITS);
    }

    // ------------------------------------------------------------------
    // Gun trunnion. The barrel elevates on its own pivot instead of taking the turret with it, so
    // the muzzle is measured from here rather than from the ring: the barrel axis runs at 32.32
    // units above the hull floor (2.02 m) and the trunnion sits at the mantlet's mid plane, 29.6
    // units ahead of the ring, leaving 56.08 units of barrel between it and the muzzle. Keep these
    // three in step with MainBattleTankModel's GUN_TRUNNION_Y / GUN_TRUNNION_Z.
    // ------------------------------------------------------------------

    /** Gun trunnion height above the hull floor. */
    public static final float GUN_TRUNNION_HEIGHT_UNITS = 32.32F;
    /** Gun trunnion distance ahead of the turret ring. */
    public static final float GUN_TRUNNION_FORWARD_UNITS = 29.6F;
    /** Barrel reach from the trunnion to the muzzle. */
    public static final float BARREL_REACH_UNITS = 56.08F;

    public static double gunTrunnionHeightBlocks() {
        return toBlocks(GUN_TRUNNION_HEIGHT_UNITS);
    }

    public static double gunTrunnionForwardBlocks() {
        return toBlocks(GUN_TRUNNION_FORWARD_UNITS);
    }

    public static double barrelReachBlocks() {
        return toBlocks(BARREL_REACH_UNITS);
    }

    // ------------------------------------------------------------------
    // Turret machine guns. Both barrels are fixed to the turret, so a muzzle is the ring plus the
    // barrel's own lateral and height offset in turret space, then the barrel length along the aim.
    // The driver's coaxial gun is the model's loader-side M240 (left of the ring, above it); seat
    // two's is the commander's cupola .50 (right of the ring, higher still). Both offsets were
    // measured from the Blender model. Keeping the two apart is what stops both seats' tracers
    // leaving the same hole when two players crew the tank.
    // ------------------------------------------------------------------
    public static final float COAX_MG_LATERAL_UNITS = -16.96F;
    public static final float COAX_MG_HEIGHT_UNITS = 18.25F;
    public static final float COAX_MG_BARREL_UNITS = 12.32F;
    public static final float CUPOLA_MG_LATERAL_UNITS = 13.6F;
    public static final float CUPOLA_MG_HEIGHT_UNITS = 20.64F;
    public static final float CUPOLA_MG_BARREL_UNITS = 15.2F;

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
