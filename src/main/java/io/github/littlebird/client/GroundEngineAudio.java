package io.github.littlebird.client;

/**
 * Maps a ground vehicle's speed to the volume and pitch of its engine loop.
 *
 * <p>Pure rules: no Minecraft classes, so the curve can be pinned by a unit test and tuned without
 * starting a client. The loop only sounds while the driver's seat is occupied; this class decides
 * only how loud and how high it is once it runs.
 *
 * <p>The pack ships one engine sample, a low rotor thump. Pitched well below 1 it reads as a
 * diesel idle, and the top of the range is deliberately short of a turbine whine so a tank at full
 * speed still sounds like a tank.
 */
public final class GroundEngineAudio {
    /** Reference speed, in blocks per tick, at which the loop reaches full loudness. */
    public static final double TOP_SPEED = 0.45D;

    /** Loudness of a stationary engine. */
    private static final float IDLE_VOLUME = 0.32F;
    /** Extra loudness added at {@link #TOP_SPEED}. */
    private static final float VOLUME_RANGE = 0.58F;
    /** Pitch of a stationary engine (a diesel rumble from the rotor sample). */
    private static final float IDLE_PITCH = 0.60F;
    /** Extra pitch added at {@link #TOP_SPEED}. */
    private static final float PITCH_RANGE = 0.35F;

    private GroundEngineAudio() {
    }

    /** How far the engine is pushed, 0 at a standstill and 1 at {@link #TOP_SPEED} or above. */
    public static float load(double horizontalSpeed) {
        if (!(horizontalSpeed > 0.0D)) {
            return 0.0F;
        }
        double fraction = horizontalSpeed / TOP_SPEED;
        return (float) Math.max(0.0D, Math.min(1.0D, fraction));
    }

    public static float volume(float load) {
        return IDLE_VOLUME + VOLUME_RANGE * clamp01(load);
    }

    public static float pitch(float load) {
        return IDLE_PITCH + PITCH_RANGE * clamp01(load);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
