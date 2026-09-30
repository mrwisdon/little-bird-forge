package io.github.littlebird.entity.flight;

/**
 * F-35E Panther flight profile: the VTOL multirole. Slower and wider-turning than the
 * Su-57, but it keeps the hover mode. Mirrors docs/plan/jet-plan.md section 5.
 */
public final class F35Profile {
    public static final double CRUISE_SPEED = 1.8D;
    public static final double AFTERBURNER_SPEED = 2.5D;

    private static final JetConfig CONFIG = JetConfig.builder()
            .maxSpeed(CRUISE_SPEED)
            .afterburnerSpeed(AFTERBURNER_SPEED)
            .forwardAccel(0.038D)
            .afterburnerAccel(0.015D)
            .bankedTurnDegPerTick(7.0D)
            .rudderYawRateDegPerTick(2.0D)
            .pitchRateDegPerTick(5.0D)
            .rollRateDegPerTick(6.0D)
            .stallSpeedFraction(0.30D)
            .build();

    private F35Profile() {
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
