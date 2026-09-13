package io.github.littlebird.client;

/**
 * Reticle geometry for the ground vehicles, kept free of Minecraft classes so the off-axis coax
 * reticle is pinned by unit tests instead of eyeballed.
 *
 * <p>The main gun is laid by the driver, so its crosshair is always the centre of the screen. The
 * coaxial gun shares the turret line, which the driver lays, so from the gunner's seat the coax
 * reticle can sit well off centre - these helpers decide the bearing and whether it is still on
 * screen.
 */
public final class GroundReticleMath {
    private GroundReticleMath() {
    }

    /** Aim bearing relative to the viewer's heading, wrapped to +/-180 degrees. */
    public static double relativeBearingDeg(double aimYawDeg, double viewYawDeg) {
        return wrapSigned(finite(aimYawDeg) - finite(viewYawDeg));
    }

    /** True when a bearing still falls inside a horizontal field of view. */
    public static boolean withinFov(double relativeBearingDeg, double horizontalFovDeg) {
        if (!Double.isFinite(relativeBearingDeg) || !Double.isFinite(horizontalFovDeg)) {
            return false;
        }
        return Math.abs(relativeBearingDeg) <= Math.abs(horizontalFovDeg) * 0.5D;
    }

    /**
     * Crosshair arm gap for a 0..1 spread reading, clamped into [minGap, maxGap]. A tight gap reads
     * as a rapid-fire weapon, a wide one as a slow heavy gun.
     */
    public static int gapPixels(double spreadFraction, int minGap, int maxGap) {
        int low = Math.min(minGap, maxGap);
        int high = Math.max(minGap, maxGap);
        if (!Double.isFinite(spreadFraction)) {
            return low;
        }
        double clamped = Math.max(0.0D, Math.min(1.0D, spreadFraction));
        return (int) Math.round(low + (high - low) * clamped);
    }

    /** Half the horizontal FOV, derived from the vertical option and the viewport aspect. */
    public static double horizontalFovDeg(double verticalFovDeg, int width, int height) {
        if (height <= 0 || !Double.isFinite(verticalFovDeg)) {
            return 0.0D;
        }
        double halfVertical = Math.toRadians(verticalFovDeg) * 0.5D;
        double halfHorizontal = Math.atan(Math.tan(halfVertical) * (width / (double) height));
        return Math.toDegrees(halfHorizontal) * 2.0D;
    }

    private static double wrapSigned(double degrees) {
        double wrapped = (degrees + 180.0D) % 360.0D;
        if (wrapped < 0.0D) {
            wrapped += 360.0D;
        }
        return wrapped - 180.0D;
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
