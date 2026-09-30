package io.github.littlebird.client;

import io.github.littlebird.entity.B2Geometry;

/**
 * Pure rules for the B-2's rigid ("hard") cockpit view.
 *
 * <p>Everything here is arithmetic on {@link B2Geometry} constants so the eye point, the rigid
 * camera mapping and the screen frame can be pinned by unit tests without a client running.
 */
public final class B2CockpitMath {
    /** Eye above the seat pan: a pilot's eyes sit above the cushion, not on it. */
    public static final double EYE_ABOVE_SEAT_BLOCKS = 0.72D;
    /** Eyes sit slightly behind the seat reference the rider offset uses. */
    public static final double EYE_BEHIND_SEAT_BLOCKS = 0.35D;

    /** The pod hangs under the nose: fraction of the nose reach and of the belly clearance. */
    public static final double POD_NOSE_FRACTION = 0.78D;
    public static final double POD_BELLY_FRACTION = 0.60D;

    /** Cockpit frame: fraction of the screen the windscreen opening takes up. */
    public static final double FRAME_MARGIN_X = 0.13D;
    public static final double FRAME_MARGIN_TOP = 0.10D;
    public static final double FRAME_MARGIN_BOTTOM = 0.26D;
    /** Width of the centre spine between the two windscreen panes. */
    public static final double SPINE_WIDTH_FRACTION = 0.018D;

    private B2CockpitMath() {
    }

    /** Eye offset ahead of the airframe origin, in blocks (nose is +forward). */
    public static double cockpitEyeAheadBlocks() {
        return B2Geometry.seatForwardBlocks() - EYE_BEHIND_SEAT_BLOCKS;
    }

    /** Eye height above the entity origin, in blocks, including the configurable lift. */
    public static double cockpitEyeHeightBlocks(double eyeLiftBlocks) {
        return B2Geometry.seatHeightBlocks() + EYE_ABOVE_SEAT_BLOCKS + Math.max(0.0D, eyeLiftBlocks);
    }

    /** Electro-optical pod station ahead of the origin, in blocks. */
    public static double podStationAheadBlocks() {
        return B2Geometry.noseReachAheadBlocks() * POD_NOSE_FRACTION;
    }

    /** Pod station height above the origin, in blocks: below the belly line. */
    public static double podStationHeightBlocks() {
        return B2Geometry.bellyClearanceBlocks() * POD_BELLY_FRACTION;
    }

    /**
     * Rigid camera mapping. The cockpit camera is bolted to the airframe: no lerp, no head bob,
     * no smoothing. The caller drops the vanilla partial-tick interpolation entirely.
     */
    public static float rigidYawDeg(float entityYawDeg) {
        return wrapDegrees(entityYawDeg);
    }

    public static float rigidPitchDeg(float entityPitchDeg) {
        return (float) clamp(entityPitchDeg, -90.0D, 90.0D);
    }

    public static float rigidRollDeg(float entityRollDeg) {
        return wrapDegrees(entityRollDeg);
    }

    /** Wraps any angle into [-180, 180). */
    public static float wrapDegrees(double degrees) {
        double wrapped = degrees % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return (float) wrapped;
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }

    public static double cockpitFov(double baseFov, double multiplier) {
        return clamp(baseFov * multiplier, 1.0D, 179.0D);
    }

    /** Screen-space windscreen rectangle: {x0, y0, x1, y1}. */
    public static int[] windscreenRect(int width, int height) {
        int x0 = (int) Math.round(width * FRAME_MARGIN_X);
        int x1 = width - x0;
        int y0 = (int) Math.round(height * FRAME_MARGIN_TOP);
        int y1 = height - (int) Math.round(height * FRAME_MARGIN_BOTTOM);
        return new int[]{Math.min(x0, x1 - 1), Math.min(y0, y1 - 1), Math.max(x0 + 1, x1),
                Math.max(y0 + 1, y1)};
    }

    /** Centre spine between the two windscreen panes: {x0, y0, x1, y1}. */
    public static int[] spineRect(int width, int height) {
        int[] frame = windscreenRect(width, height);
        int half = Math.max(1, (int) Math.round(width * SPINE_WIDTH_FRACTION * 0.5D));
        int cx = width / 2;
        return new int[]{cx - half, frame[1], cx + half, frame[3]};
    }

    /** Glare shield across the bottom of the windscreen: {x0, y0, x1, y1}. */
    public static int[] glareShieldRect(int width, int height) {
        int[] frame = windscreenRect(width, height);
        int top = frame[3] - Math.max(6, Math.round(height * 0.045F));
        return new int[]{frame[0], top, frame[2], frame[3]};
    }
}
