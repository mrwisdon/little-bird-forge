package io.github.littlebird.entity.flight;

/**
 * Flight feel of the AH-64: heavier and slower than the Little Bird everywhere, with a
 * 90 km/h top speed, but the same envelope - the nose still reaches straight up and down and
 * the airframe still rolls all the way over, it just takes its time getting there.
 */
public final class ApacheFlightProfile {
    /** 1.25 blocks per tick = 90 km/h, which is what the HUD reads. */
    public static final double TOP_SPEED_BLOCKS_PER_TICK = 1.25D;

    private ApacheFlightProfile() {
    }

    public static FlightConfig config() {
        return FlightConfig.builder()
                .maxForwardSpeed(TOP_SPEED_BLOCKS_PER_TICK)
                .forwardAccel(0.024D)
                .reverseAccel(0.032D)
                .sideSlipAccel(0.016D)
                .maxClimbSpeed(0.34D)
                .maxDescentSpeed(0.45D)
                .maxFallSpeed(1.10D)
                .verticalAccel(0.030D)
                .verticalDrag(0.12D)
                .maxYawRateDegPerTick(6.0D)
                .bankedTurnDegPerTick(5.0D)
                .pitchMaxDeg(90.0D)
                .pitchRateDegPerTick(4.5D)
                .rollRateDegPerTick(4.5D)
                .rollSpinUpTicks(14.0D)
                .build();
    }

    /** Top speed in the unit the HUD shows. */
    public static int topSpeedKmh() {
        return (int) Math.round(TOP_SPEED_BLOCKS_PER_TICK * 20.0D * 3.6D);
    }
}
