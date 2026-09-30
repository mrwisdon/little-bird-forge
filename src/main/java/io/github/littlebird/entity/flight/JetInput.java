package io.github.littlebird.entity.flight;

/**
 * One tick of fixed-wing jet pilot intent. Pure data (no Minecraft classes).
 *
 * <p>pitchInput is -1..1; positive means nose down, which is also Minecraft's positive
 * xRot, so the value can be reused directly. rollInput is -1..1 with positive meaning
 * roll right: this is the mouse X axis in the Battlefield 2042 jet preset. yawInput is
 * -1..1, positive yaw right, feeding the A/D rudder. throttleInput is -1..1 and is
 * integrated by the model into the persistent throttle state, so the throttle holds
 * where the pilot left it instead of behaving like a momentary axis.
 */
public record JetInput(
        float pitchInput,
        float rollInput,
        float yawInput,
        float throttleInput,
        boolean afterburner,
        boolean vtol,
        boolean onGround) {

    public static JetInput idle(boolean onGround) {
        return new JetInput(0.0F, 0.0F, 0.0F, 0.0F, false, false, onGround);
    }

    /** Clamps every analogue axis and replaces non-finite values with 0. */
    public JetInput sanitized() {
        return new JetInput(
                clampAxis(pitchInput),
                clampAxis(rollInput),
                clampAxis(yawInput),
                clampAxis(throttleInput),
                afterburner,
                vtol,
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
