package io.github.littlebird.client;

/**
 * Pure timing math for the client-only tracer animation, kept free of Minecraft
 * classes so the flight of a bullet is unit-tested rather than eyeballed.
 *
 * A tracer travels at SPEED_PER_TICK blocks per tick and keeps a short trail
 * behind its head, then fades out shortly after reaching the impact point.
 */
public final class LittleBirdTracerTimeline {
    public static final double SPEED_PER_TICK = 14.0D;
    public static final double TRAIL_LENGTH = 0.85D;
    public static final double POST_ARRIVAL_FADE_TICKS = 0.2D;
    public static final double MUZZLE_FLASH_TICKS = 0.16D;
    public static final double MAX_UNRENDERED_TICKS = 20.0D;

    private LittleBirdTracerTimeline() {
    }

    public static double travelTicks(double length) {
        return Math.max(0.0D, length) / SPEED_PER_TICK;
    }

    public static double frontDistance(double elapsed, double length) {
        return Math.min(Math.max(0.0D, length),
                Math.max(0.0D, elapsed) * SPEED_PER_TICK);
    }

    public static double tailDistance(double frontDistance) {
        return Math.max(0.0D, frontDistance - TRAIL_LENGTH);
    }

    /** 1 while the bullet is still flying, fading to 0 after it arrives. */
    public static double arrivalFade(double elapsed, double length) {
        double afterArrival = Math.max(0.0D, elapsed - travelTicks(length));
        return clamp01(1.0D - afterArrival / POST_ARRIVAL_FADE_TICKS);
    }

    public static boolean hasArrived(double elapsed, double length) {
        return elapsed >= travelTicks(length);
    }

    public static boolean isExpired(double elapsed, double length) {
        return elapsed >= travelTicks(length) + POST_ARRIVAL_FADE_TICKS;
    }

    public static boolean isStaleWithoutRender(double elapsed) {
        return elapsed >= MAX_UNRENDERED_TICKS;
    }

    public static double muzzleFlashFade(double age) {
        return clamp01(1.0D - Math.max(0.0D, age) / MUZZLE_FLASH_TICKS);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }
}