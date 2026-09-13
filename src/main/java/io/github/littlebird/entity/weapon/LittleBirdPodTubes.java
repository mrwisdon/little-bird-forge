package io.github.littlebird.entity.weapon;

/**
 * 70mm pod tube layout: one centre tube plus six around it, matching the model.
 * Kept free of Minecraft classes so the spent-tube visuals are unit-tested.
 */
public final class LittleBirdPodTubes {
    public static final int TUBES = 7;
    public static final float RING_RADIUS = 2.4F;

    private LittleBirdPodTubes() {
    }

    /** Tubes fire from index 0 outwards, so index < remaining means still loaded. */
    public static boolean loaded(int tube, int remaining) {
        return tube >= 0 && tube < TUBES && tube < remaining;
    }

    public static float offsetX(int tube) {
        return tube <= 0 ? 0.0F : (float) (RING_RADIUS * Math.cos(angle(tube)));
    }

    public static float offsetY(int tube) {
        return tube <= 0 ? 0.0F : (float) (RING_RADIUS * Math.sin(angle(tube)));
    }

    private static double angle(int tube) {
        return Math.toRadians(60.0D * (tube - 1));
    }
}