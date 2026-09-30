package io.github.littlebird.entity.flight;

/**
 * Su-57 Felon flight profile: no VTOL, so it trades the hover mode for a higher top
 * speed and a tighter turn. Mirrors docs/plan/jet-plan.md section 5.
 */
public final class Su57Profile {
    public static final double CRUISE_SPEED = 1.9D;
    public static final double AFTERBURNER_SPEED = 2.7D;

    private static final JetConfig CONFIG = JetConfig.builder()
            .maxSpeed(CRUISE_SPEED)
            .afterburnerSpeed(AFTERBURNER_SPEED)
            .forwardAccel(0.040D)
            .afterburnerAccel(0.016D)
            .bankedTurnDegPerTick(7.5D)
            .rudderYawRateDegPerTick(2.2D)
            .pitchRateDegPerTick(5.6D)
            .rollRateDegPerTick(7.0D)
            .stallSpeedFraction(0.28D)
            .horizontalDrag(0.019D)
            .build();

    private Su57Profile() {
    }

    public static JetConfig config() {
        return CONFIG;
    }

    public static int topSpeedKmh() {
        return (int) Math.round(CRUISE_SPEED * 20.0D * 3.6D);
    }

    public static int afterburnerTopSpeedKmh() {
        return (int) Math.round(AFTERBURNER_SPEED * 20.0D * 3.6D);
    }
}
