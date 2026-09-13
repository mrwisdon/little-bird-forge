package io.github.littlebird.entity.flight;

/**
 * One tick of pilot intent. Pure data (no Minecraft classes).
 *
 * pitchInput and rollInput are -1..1; pitchInput positive means nose down,
 * which is also Minecraft's positive xRot, so the value can be reused directly.
 * collectiveInput is -1..1, positive meaning climb.
 */
public record HelicopterInput(
        float yawDeg,
        float pitchInput,
        float rollInput,
        float collectiveInput,
        boolean onGround) {

    public static HelicopterInput idle(float yawDeg, boolean onGround) {
        return new HelicopterInput(yawDeg, 0.0F, 0.0F, 0.0F, onGround);
    }

    /** Clamps every analogue axis and replaces non-finite values with 0. */
    public HelicopterInput sanitized() {
        return new HelicopterInput(
                finiteOr(yawDeg, 0.0F),
                clampAxis(pitchInput),
                clampAxis(rollInput),
                clampAxis(collectiveInput),
                onGround);
    }

    public static float clampAxis(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0.0F;
        }
        return Math.max(-1.0F, Math.min(1.0F, value));
    }

    public static float finiteOr(float value, float fallback) {
        return Float.isNaN(value) || Float.isInfinite(value) ? fallback : value;
    }
}
