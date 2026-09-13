package io.github.littlebird.entity;

/**
 * Shared geometry for the helicopter, kept free of Minecraft classes so the
 * invariants that decide whether the aircraft is visible at all are unit-tested:
 *
 * 1. A Minecraft entity position is the BOTTOM of its bounding box, so the
 *    model's lowest geometry (the skid tubes) sits at SKID_BOTTOM_UNITS = 0.
 * 2. The airframe must fit inside its own hitbox.
 * 3. The pilot's eye must stay below the rotor disc.
 *
 * Unit-space constants describe the true-scale AH-6 airframe (16 units = 1 block).
 * SCALE shrinks the whole aircraft for playability; every block value below is
 * already scaled, and the renderer applies the same SCALE to the model pose.
 */
public final class LittleBirdGeometry {
    public static final float UNITS_PER_BLOCK = 16.0F;

    /**
     * Overall size multiplier. 1.0 is the true-scale 10 block rotor; 0.65 gives a
     * 6.5 block rotor, which clears most player builds and keeps the cabin big
     * enough for the cockpit view. Change this one number to resize the aircraft.
     */
    public static final float SCALE = 0.65F;

    // Vertical layout in unit space, measured from the ground (the entity position).
    public static final float SKID_BOTTOM_UNITS = 0.0F;
    public static final float SKID_TUBE_HEIGHT_UNITS = 3.0F;
    public static final float BELLY_UNITS = 10.0F;
    public static final float CABIN_BOTTOM_UNITS = 12.0F;
    public static final float CANOPY_BOTTOM_UNITS = 20.0F;
    public static final float CABIN_TOP_UNITS = 34.0F;
    public static final float ROOF_TOP_UNITS = 40.0F;
    /** Raised mast: keeps the rotor disc clear of the pilot's eye line. */
    public static final float ROTOR_HUB_UNITS = 56.0F;
    /** Top face of the nose deck, the surface the cockpit camera looks over. */
    public static final float NOSE_DECK_TOP_UNITS = 26.0F;
    /**
     * Cockpit camera eye height. It must clear the nose deck, otherwise the deck
     * sits on the eye line and fills the lower half of the screen with black.
     */
    public static final float COCKPIT_EYE_UNITS = 38.0F;
    /**
     * How far forward of the entity origin the cockpit camera sits. It has to be
     * ahead of the windscreen frame and dashboard, otherwise that frame hangs in
     * front of the eye and blocks the middle of the screen.
     */
    public static final float COCKPIT_SEAT_FORWARD_UNITS = 42.0F;

    // Horizontal layout in unit space.
    public static final float SKID_HALF_SPACING_UNITS = 10.0F;
    public static final float CABIN_HALF_WIDTH_UNITS = 13.0F;
    public static final float NOSE_FRONT_UNITS = -46.0F;
    public static final float CANOPY_FRONT_UNITS = -38.0F;
    public static final float CANOPY_BACK_UNITS = -26.0F;
    public static final float TAIL_END_UNITS = 70.0F;
    /** Outboard pylon under each stub wing: the 70mm pod hangs here. */
    public static final float POD_X_UNITS = 25.0F;
    public static final float POD_Y_UNITS = 10.0F;
    /** Inboard pylon under each stub wing: the machine gun hangs here. */
    public static final float GUN_X_UNITS = 17.0F;
    public static final float GUN_Y_UNITS = 15.0F;
    /** Muzzles of the detailed under-wing guns (receiver + 12 unit barrel cluster). */
    public static final float GUN_MUZZLE_Z_UNITS = -22.0F;
    public static final float WING_TIP_UNITS = 32.0F;

    public static final float ROTOR_RADIUS_UNITS = 80.0F;
    public static final float TAIL_ROTOR_RADIUS_UNITS = 11.0F;

    // Hitbox and rider placement, in unscaled blocks (multiplied by SCALE below).
    private static final float AABB_WIDTH_BASE_BLOCKS = 2.6F;
    private static final float AABB_HEIGHT_BASE_BLOCKS = 2.6F;
    private static final float RIDER_OFFSET_BASE_BLOCKS = 0.6F;

    private LittleBirdGeometry() {
    }

    /** Unit-space value to unscaled blocks. */
    public static float toBlocks(float units) {
        return units / UNITS_PER_BLOCK;
    }

    /** Unit-space value to blocks after SCALE is applied. */
    public static float toScaledBlocks(float units) {
        return toBlocks(units) * SCALE;
    }

    public static float toUnits(float blocks) {
        return blocks * UNITS_PER_BLOCK;
    }

    public static float aabbWidthBlocks() {
        return AABB_WIDTH_BASE_BLOCKS * SCALE;
    }

    public static float aabbHeightBlocks() {
        return AABB_HEIGHT_BASE_BLOCKS * SCALE;
    }

    public static float riderOffsetBlocks() {
        return RIDER_OFFSET_BASE_BLOCKS * SCALE;
    }

    /** Rotor diameter in blocks after scaling. */
    public static float rotorDiameterBlocks() {
        return toScaledBlocks(ROTOR_RADIUS_UNITS * 2.0F);
    }

    /** Airframe height without the rotor disc, in scaled blocks. */
    public static float airframeHeightBlocks() {
        return toScaledBlocks(ROOF_TOP_UNITS - SKID_BOTTOM_UNITS);
    }

    /** Height of the front windscreen band, in scaled blocks. */
    public static float canopyTopBlocks() {
        return toScaledBlocks(CABIN_TOP_UNITS - 1.0F);
    }

    public static float canopyBottomBlocks() {
        return toScaledBlocks(CANOPY_BOTTOM_UNITS);
    }

    public static float rotorHubBlocks() {
        return toScaledBlocks(ROTOR_HUB_UNITS);
    }

    public static float gunHeightBlocks() {
        return toScaledBlocks(GUN_Y_UNITS);
    }

    public static float gunLateralBlocks() {
        return toScaledBlocks(GUN_X_UNITS);
    }

    public static float podLateralBlocks() {
        return toScaledBlocks(POD_X_UNITS);
    }

    public static float podHeightBlocks() {
        return toScaledBlocks(POD_Y_UNITS);
    }

    /** How far the rocket pod front sits ahead of the origin, in blocks. */
    public static float podForwardBlocks() {
        return toScaledBlocks(21.0F);
    }

    public static float gunForwardBlocks() {
        return toScaledBlocks(-GUN_MUZZLE_Z_UNITS);
    }
    /** Middle of the windscreen band, in scaled blocks. */
    public static float canopyMidBlocks() {
        return toScaledBlocks((CANOPY_BOTTOM_UNITS + CABIN_TOP_UNITS - 1.0F) * 0.5F);
    }

    /**
     * Rider offset used by the cockpit camera. Minecraft's eye sits 1.62 blocks
     * above the rider position, so at this build scale the offset is negative: the
     * camera has to drop into the airframe to look through the windscreen instead
     * of over the roof. It is applied on the client only, so the server keeps the
     * pilot above ground.
     */
    public static float cockpitEyeBlocks() {
        return toScaledBlocks(COCKPIT_EYE_UNITS);
    }

    public static float noseDeckTopBlocks() {
        return toScaledBlocks(NOSE_DECK_TOP_UNITS);
    }

    /** Forward offset of the cockpit camera, in scaled blocks. */
    public static float cockpitSeatForwardBlocks() {
        return toScaledBlocks(COCKPIT_SEAT_FORWARD_UNITS);
    }

    /** Z distance from the origin to the windscreen plane, in scaled blocks. */
    public static float windscreenFrontBlocks() {
        return toScaledBlocks(-CANOPY_FRONT_UNITS);
    }

    public static float cockpitRiderOffsetBlocks() {
        return cockpitRiderOffsetBlocks(0.0F);
    }

    /**
     * Same, with the player's eye lift added on top. The cabin shell and the roof are
     * hidden in this view, so lifting the camera only raises the sight line over the nose
     * deck instead of pushing it through the airframe.
     */
    public static float cockpitRiderOffsetBlocks(float liftBlocks) {
        return cockpitEyeBlocks() + liftBlocks - 1.62F;
    }
}
