package io.github.littlebird.entity.weapon;

/**
 * A hand thrown smoke canister: how long the fuse runs, how fast the cloud it leaves swells to its
 * full size, and how it fades in and out over its life.
 *
 * <p>The cloud itself is drawn by the client from these numbers, so the shape of the screen it
 * makes is decided here rather than inside the renderer or the entity. Everything is expressed in
 * ticks and fractions of the configured radius, which keeps the balance values in the config where
 * a server owner can reach them.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class SmokeGrenadeRules {
    /** Ticks a thrown canister flies before it vents, even over open ground. */
    public static final int FUSE_TICKS = 100;
    /** Ticks the cloud takes to swell from its starting radius to the configured one. */
    public static final int GROWTH_TICKS = 40;
    /** Radius the cloud starts at, as a fraction of its full radius. */
    public static final float START_RADIUS_FRACTION = 0.45F;
    /**
     * Share of the cloud's life spent fading in. Kept short: a canister that lands in front of you
     * has to hide you within a second, not slowly appear.
     */
    public static final float FADE_IN_FRACTION = 0.06F;
    /** Share of the cloud's life spent fading out, so it dissipates instead of blinking away. */
    public static final float FADE_OUT_FRACTION = 0.25F;
    /** Shortest life a cloud may be given, in ticks. A zero tick cloud is a bug, not a setting. */
    public static final int MIN_LIFETIME_TICKS = 1;

    /** Life of one cloud, in ticks, for a duration in whole seconds. */
    public static int lifetimeTicks(int seconds) {
        return Math.max(MIN_LIFETIME_TICKS, seconds * 20);
    }

    /** True once the fuse has burned down and the canister has to vent. */
    public static boolean fuseExpired(int ageTicks) {
        return ageTicks >= FUSE_TICKS;
    }

    /** True once the cloud has lived out its life and should be removed. */
    public static boolean spent(int ageTicks, int lifetimeTicks) {
        return lifetimeTicks <= 0 || ageTicks >= lifetimeTicks;
    }

    /**
     * Radius of the cloud at an age, in blocks. The swell uses a square root so the cloud fills the
     * space around the canister quickly and then only creeps outwards: a linear ramp reads as a
     * balloon inflating, which is exactly what a smoke screen must not look like.
     */
    public static float radiusAt(int ageTicks, float fullRadius) {
        if (fullRadius <= 0.0F) {
            return 0.0F;
        }
        if (ageTicks <= 0) {
            return fullRadius * START_RADIUS_FRACTION;
        }
        float progress = Math.min(1.0F, ageTicks / (float) GROWTH_TICKS);
        float eased = (float) Math.sqrt(progress);
        float fraction = START_RADIUS_FRACTION + (1.0F - START_RADIUS_FRACTION) * eased;
        return fullRadius * fraction;
    }

    /**
     * How opaque the cloud is at an age, from 0 to 1. The middle of the life is a plateau at full
     * density; only the first and last few percent are ramps, which is what lets the cloud hide a
     * vehicle for the whole configured duration instead of only at its peak.
     */
    public static float opacityAt(int ageTicks, int lifetimeTicks) {
        if (lifetimeTicks <= 0 || ageTicks >= lifetimeTicks) {
            return 0.0F;
        }
        if (ageTicks <= 0) {
            return 0.0F;
        }
        int fadeIn = Math.max(1, Math.round(lifetimeTicks * FADE_IN_FRACTION));
        int fadeOut = Math.max(1, Math.round(lifetimeTicks * FADE_OUT_FRACTION));
        if (ageTicks < fadeIn) {
            return clamp01(ageTicks / (float) fadeIn);
        }
        int remaining = lifetimeTicks - ageTicks;
        if (remaining < fadeOut) {
            return clamp01(remaining / (float) fadeOut);
        }
        return 1.0F;
    }

    /**
     * Radius of a single puff drawn inside the cloud, in blocks. Puffs sit on the shell and travel
     * outwards with the cloud, and a slow wobble keeps the silhouette from reading as a sphere.
     */
    public static float puffRadius(int puffIndex, int ageTicks, float fullRadius) {
        float outer = radiusAt(ageTicks, fullRadius);
        // Staggered by index so the puffs do not breathe in lockstep.
        double phase = ageTicks * 0.09D + puffIndex * 1.7D;
        float wobble = (float) (1.0D + 0.08D * Math.sin(phase));
        return outer * 0.55F * wobble;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private SmokeGrenadeRules() {
    }
}
