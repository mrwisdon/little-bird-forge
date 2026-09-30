package io.github.littlebird.entity.flight;

/**
 * Immutable fixed-wing jet state. Pure data (no Minecraft classes).
 *
 * <p>pitchDeg is positive nose-down and matches Minecraft's positive xRot. throttle is
 * 0..1 and persists between ticks. afterburnerCharge is 0..1 and drains while the
 * burner runs. vtol is only meaningful for the F-35E.
 */
public record JetState(
        float yawDeg,
        float pitchDeg,
        float rollDeg,
        double vx,
        double vy,
        double vz,
        float throttle,
        float afterburnerCharge,
        boolean vtol) {

    public static JetState initial(float yawDeg) {
        return initial(yawDeg, false);
    }

    public static JetState initial(float yawDeg, boolean vtol) {
        return new JetState(yawDeg, 0.0F, 0.0F, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, vtol);
    }

    public JetState withVtol(boolean value) {
        return new JetState(yawDeg, pitchDeg, rollDeg, vx, vy, vz,
                throttle, afterburnerCharge, value);
    }

    public double horizontalSpeed() {
        return Math.sqrt(vx * vx + vz * vz);
    }

    public double airspeed() {
        return Math.sqrt(vx * vx + vy * vy + vz * vz);
    }
}
