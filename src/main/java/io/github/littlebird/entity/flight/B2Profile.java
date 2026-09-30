package io.github.littlebird.entity.flight;

/**
 * B-2 Spirit flight profile: a subsonic flying wing.
 *
 * <p>A flying wing has no horizontal tail to trim with, so it pays for its span with a
 * lower roll and yaw rate than the two fighters and stalls earlier - the same trade the
 * Battlefield flying wing makes. There is no afterburner on the real aircraft: the second
 * speed is the military thrust ceiling, kept so the shared jet code has one throttle
 * range to work with.
 */
public final class B2Profile {
    /** Cruise speed, blocks per tick. */
    public static final double CRUISE_SPEED = 1.5D;
    /** Military thrust ceiling, blocks per tick. */
    public static final double AFTERBURNER_SPEED = 2.0D;

    private static final JetConfig CONFIG = JetConfig.builder()
            .maxSpeed(CRUISE_SPEED)
            .afterburnerSpeed(AFTERBURNER_SPEED)
            .forwardAccel(0.034D)
            .afterburnerAccel(0.012D)
            .bankedTurnDegPerTick(6.2D)
            .rudderYawRateDegPerTick(1.6D)
            .pitchRateDegPerTick(4.6D)
            .rollRateDegPerTick(6.6D)
            .stallSpeedFraction(0.26D)
            .horizontalDrag(0.017D)
            .build();

    private B2Profile() {
    }

    public static JetConfig config() {
        return CONFIG;
    }

    /** Cruise speed on the HUD, in km/h, using the same blocks/tick convention as the mod. */
    public static int topSpeedKmh() {
        return (int) Math.round(CRUISE_SPEED * 20.0D * 3.6D);
    }

    public static int afterburnerTopSpeedKmh() {
        return (int) Math.round(AFTERBURNER_SPEED * 20.0D * 3.6D);
    }
}
