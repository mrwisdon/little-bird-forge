package io.github.littlebird.client;

/**
 * Crosshair geometry for the Battlefield-style dynamic reticle, kept free of
 * Minecraft classes.
 *
 * The gun crosshair sits on the predicted impact point and its four arms open up
 * with the weapon's current dispersion, so a hot barrel visibly widens the reticle.
 */
public final class LittleBirdCrosshairMath {
    /** The arms never close completely, or the crosshair reads as a dot. */
    public static final double MIN_ARM_GAP_PIXELS = 3.0D;
    public static final int ARM_LENGTH_PIXELS = 7;

    private LittleBirdCrosshairMath() {
    }

    /** Radius of the dispersion cone at the given distance, in blocks. */
    public static double spreadRadiusBlocks(double distance, double spreadDegrees) {
        double safeDistance = Math.max(0.0D, distance);
        double safeDegrees = Math.max(0.0D, spreadDegrees);
        return Math.tan(Math.toRadians(safeDegrees)) * safeDistance;
    }

    public static double pixelDistance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    /** Arm offset in screen pixels, clamped to the minimum gap. */
    public static double armGapPixels(double centreX, double centreY,
                                      double edgeX, double edgeY) {
        double gap = pixelDistance(centreX, centreY, edgeX, edgeY);
        if (!Double.isFinite(gap)) {
            return MIN_ARM_GAP_PIXELS;
        }
        return Math.max(MIN_ARM_GAP_PIXELS, gap);
    }

    /** True when the named weapon should own the reticle. */
    public static boolean showsGunReticle(
            boolean followsWeapon, int selectedWeapon, boolean gunEnabled) {
        if (!gunEnabled) {
            return false;
        }
        return !followsWeapon || selectedWeapon == 0;
    }

    /** True when the rocket reticle should be drawn. */
    public static boolean showsRocketReticle(
            boolean followsWeapon, int selectedWeapon, boolean rocketEnabled) {
        if (!rocketEnabled) {
            return false;
        }
        return !followsWeapon || selectedWeapon == 1;
    }
    /** Four rows of dots; the reference reticle's inner column skips the top cell. */
    public static final int DOT_ROWS = 4;
    public static final int DOT_COLUMNS = 2;
    /** Each dot column shows one 70mm pod: seven tubes. */
    public static final int TUBES_PER_POD = 7;

    /** column 0 is the outer column, column 1 the inner one; row 0 is the top. */
    public record DotPosition(int column, int row) {
    }

    /**
     * Where a pod tube's indicator dot sits.
     *
     * The reticle must empty left to right and top to bottom, and the pods alternate
     * when firing, so the tube that fires first owns the first dot that disappears:
     * the outer column's top cell. Tubes therefore map to the layout in reverse,
     * outer column top to bottom first, then the inner column below its empty top cell.
     */
    public static DotPosition tubeDot(int tube) {
        if (tube < 0 || tube >= TUBES_PER_POD) {
            throw new IllegalArgumentException("tube out of range: " + tube);
        }
        return sequenceDot(TUBES_PER_POD - 1 - tube);
    }

    /**
     * Dot at the given position in the emptying order (0 empties first).
     *
     * The order runs left to right first and then top to bottom, so each row is
     * consumed across both columns before the next row starts. The inner column's
     * top cell is empty, so the top row only holds the outer dot.
     */
    public static DotPosition sequenceDot(int position) {
        if (position < 0 || position >= TUBES_PER_POD) {
            throw new IllegalArgumentException("position out of range: " + position);
        }
        if (position == 0) {
            return new DotPosition(0, 0);
        }
        int row = 1 + (position - 1) / DOT_COLUMNS;
        int column = (position - 1) % DOT_COLUMNS;
        return new DotPosition(column, row);
    }
    /** Fraction of the way the drawn reticle moves towards its target each frame. */
    public static final double SMOOTHING_FACTOR = 0.5D;

    /** True when a projected point sits behind the camera and cannot be placed at all. */
    public static boolean behindCamera(boolean visible) {
        return !visible;
    }

    /** Fraction of each screen edge kept clear so the reticle never sits on the frame. */
    public static final double SCREEN_MARGIN_FRACTION = 0.06D;

    /** A reticle position in screen pixels. */
    public record ScreenPoint(double x, double y) {
    }

    /**
     * Pulls a projected point back inside the screen frame. A point that leaves the frame
     * pegs at the margin, in the direction it left, instead of jumping to the centre: the
     * impact point hops around by whole blocks between ticks, so the old boolean fallback
     * flipped the crosshair between the edge and the middle every few frames, which read as
     * the reticle jittering against the edge of the screen.
     */
    public static ScreenPoint clampToFrame(double x, double y, int width, int height) {
        double spanX = Math.max(0.0D, width);
        double spanY = Math.max(0.0D, height);
        double marginX = spanX * SCREEN_MARGIN_FRACTION;
        double marginY = spanY * SCREEN_MARGIN_FRACTION;
        double lowestX = Math.min(marginX, spanX - marginX);
        double lowestY = Math.min(marginY, spanY - marginY);
        return new ScreenPoint(
                clamp(finiteOr(x), lowestX, Math.max(lowestX, spanX - marginX)),
                clamp(finiteOr(y), lowestY, Math.max(lowestY, spanY - marginY)));
    }

    /** Widest the fixed centre cross's arms may open. */
    public static final double MAX_CROSSHAIR_GAP_PIXELS = 28.0D;

    /**
     * Arm gap for the fixed centre crosshair: the dispersion cone's radius at the reference
     * distance, seen from the camera and turned into screen pixels. It is the only part of
     * the cross that still moves, so a hot barrel visibly widens it.
     */
    public static double crosshairGapPixels(double spreadDegrees, double referenceDistance,
                                            double verticalFovDeg, int height) {
        double halfFov = Math.tan(Math.toRadians(Math.max(0.0D, verticalFovDeg)) * 0.5D);
        if (!(referenceDistance > 0.0D) || !(halfFov > 0.0D)) {
            return MIN_ARM_GAP_PIXELS;
        }
        double radius = spreadRadiusBlocks(referenceDistance, spreadDegrees);
        double pixels = (radius / referenceDistance) / halfFov * (Math.max(0, height) / 2.0D);
        if (!Double.isFinite(pixels)) {
            return MIN_ARM_GAP_PIXELS;
        }
        return Math.max(MIN_ARM_GAP_PIXELS, Math.min(MAX_CROSSHAIR_GAP_PIXELS, pixels));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double finiteOr(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    /** Frame-rate independent-ish easing without overshoot. */
    public static double smooth(double current, double target, double factor) {
        double f = Math.max(0.0D, Math.min(1.0D, factor));
        return current + (target - current) * f;
    }
}
