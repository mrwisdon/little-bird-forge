package io.github.littlebird.client;

/**
 * Camera shock handed to the client by a heavy shell impact.
 *
 * <p>Kept as a plain trauma accumulator rather than a random shake so the same tick always paints
 * the same camera offset: the renderer samples it several times per frame (yaw, pitch, HUD), and
 * anything random would tear the view apart between samples and make the effect untestable. One
 * impact adds its already distance-attenuated intensity, every client tick bleeds the accumulator
 * away, and the visible deflection is shaped from that single number.
 *
 * <p>The maths is deliberately free of any Minecraft type so {@code ImpactShakeTest} can pin the
 * clamps, the decay and the finite output without a running game.
 */
public final class ImpactShake {
    /** Largest yaw deflection one impact can hand the camera, in degrees. */
    public static final float MAX_YAW_DEG = 2.0F;

    /** Largest pitch deflection one impact can hand the camera, in degrees. */
    public static final float MAX_PITCH_DEG = 1.6F;

    /** Trauma kept per tick: a short, hard punch rather than a lingering wobble. */
    static final float DECAY_PER_TICK = 0.86F;

    /** Trauma below this is treated as spent, so the offsets settle on exactly zero. */
    static final float TRAUMA_EPSILON = 0.001F;

    /** Accumulated shock in {@code [0, 1]}; squared when shaped so it snaps in and fades out. */
    private static float trauma;

    /** Completed client ticks, the phase of the deterministic wobble. */
    private static long tickCount;

    private ImpactShake() {
    }

    /**
     * Records one impact. The intensity arrives already attenuated by distance on the server, so
     * this only stacks it and clamps: several near-simultaneous hits must not push the camera past
     * the documented caps.
     */
    public static void add(float intensity) {
        if (!Float.isFinite(intensity) || intensity <= 0.0F) {
            return;
        }
        trauma = clamp01(trauma + intensity);
    }

    /** Bleeds the accumulator once per client tick; the shock is gone within about two seconds. */
    public static void tick() {
        tickCount++;
        trauma *= DECAY_PER_TICK;
        if (trauma < TRAUMA_EPSILON) {
            trauma = 0.0F;
        }
    }

    /** This frame's yaw deflection in degrees, meant to be added to the camera's own yaw. */
    public static float yawOffsetDeg(float partialTick) {
        return offsetDeg(MAX_YAW_DEG, 0.0F, partialTick);
    }

    /** This frame's pitch deflection in degrees, meant to be added to the camera's own pitch. */
    public static float pitchOffsetDeg(float partialTick) {
        return offsetDeg(MAX_PITCH_DEG, 1.7F, partialTick);
    }

    /** Drops the shock without waiting for it to decay, for world unloads and respawns. */
    public static void reset() {
        trauma = 0.0F;
        tickCount = 0L;
    }

    /** Current accumulated shock, exposed so callers and tests can see the state behind the offsets. */
    public static float trauma() {
        return trauma;
    }

    /**
     * The whole shaping rule: amplitude times squared trauma times unit noise. Pure, so a test can
     * prove the output never leaves the cap and never turns into NaN for any partial tick.
     */
    static float offsetDeg(float maxDeg, float channel, float partialTick) {
        return shapedOffsetDeg(maxDeg, trauma, tickCount, channel, partialTick);
    }

    /** Pure form of {@link #offsetDeg}, kept static and side effect free for the unit test. */
    static float shapedOffsetDeg(
            float maxDeg, float trauma, long tickCount, float channel, float partialTick) {
        if (!Float.isFinite(maxDeg) || !Float.isFinite(partialTick)) {
            return 0.0F;
        }
        float clampedTrauma = clamp01(trauma);
        float sway = wobble(tickCount, channel, partialTick);
        return maxDeg * clampedTrauma * clampedTrauma * sway;
    }

    /**
     * Deterministic unit noise in {@code [-1, 1]} from three sine partials whose weights sum to one,
     * so the bound holds by construction. {@code partialTick} slides the phase between ticks, which
     * is what keeps the shake smooth instead of stepping at 20 Hz.
     */
    static float wobble(long tickCount, float channel, float partialTick) {
        float tickFraction = Float.isFinite(partialTick) ? clamp01(partialTick) : 0.0F;
        double t = tickCount + tickFraction;
        double slow = Math.sin(t * 1.000D + channel * 2.399D);
        double mid = Math.sin(t * 2.317D + channel * 5.111D);
        double fast = Math.sin(t * 4.733D + channel * 0.771D);
        return (float) (slow * 0.55D + mid * 0.30D + fast * 0.15D);
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
