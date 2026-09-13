package io.github.littlebird.entity.flight;

/**
 * Immutable flight state. Pure data (no Minecraft classes).
 *
 * pitchDeg is positive nose-down and matches Minecraft's positive xRot, so the
 * entity can apply setXRot(pitchDeg) without a sign flip.
 */
public record HelicopterState(
        float yawDeg,
        float pitchDeg,
        float rollDeg,
        double vx,
        double vy,
        double vz,
        float rotorRpm,
        boolean engineOn) {

    public static HelicopterState initial(float yawDeg) {
        return new HelicopterState(yawDeg, 0.0F, 0.0F, 0.0D, 0.0D, 0.0D, 0.0F, false);
    }

    public HelicopterState withEngineOn(boolean value) {
        return new HelicopterState(
                yawDeg, pitchDeg, rollDeg, vx, vy, vz, rotorRpm, value);
    }

    public double horizontalSpeed() {
        return Math.sqrt(vx * vx + vz * vz);
    }
}
