package io.github.littlebird.entity.flight;

/**
 * First-order damping for the control axes, kept free of Minecraft classes.
 *
 * Keyboard axes are on/off and raw mouse deltas are noisy, so the aircraft reads
 * every axis through this filter: each tick the smoothed value moves a fraction
 * (the config damping factor) of the way to the commanded value. 1.0 disables
 * damping, smaller values make the helicopter feel heavier and smoother.
 */
public final class HelicopterInputDamping {
    /** Roll command below this counts as "key released". */
    private static final float ROLL_RELEASE_EPSILON = 1.0E-3F;

    public record State(float yawDeg, float pitch, float roll, float collective) {
        public static State initial(float yawDeg) {
            return new State(yawDeg, 0.0F, 0.0F, 0.0F);
        }
    }

    private HelicopterInputDamping() {
    }

    /** Advances the smoothed axes by one tick. */
    public static State step(State previous, HelicopterInput rawInput, FlightConfig config) {
        HelicopterInput raw = rawInput.sanitized();
        State state = previous == null ? State.initial(raw.yawDeg()) : previous;
        float yaw = state.yawDeg() + (float) (HelicopterFlightModel.wrapDegrees(
                (float) (raw.yawDeg() - state.yawDeg())) * factor(config.dampingYaw));
        float pitch = approach(state.pitch(), raw.pitchInput(), factor(config.dampingPitch));
        // Roll is a rate axis rather than a deflection. It ramps in through this filter so
        // that a key tap does not snap the rotor disk over, but it drops to zero the instant
        // the key comes up: otherwise the decaying filter would keep rolling the aircraft for
        // another fifth of a second and the pilot could never hold a bank angle.
        float roll = Math.abs(raw.rollInput()) < ROLL_RELEASE_EPSILON
                ? 0.0F
                : approach(state.roll(), raw.rollInput(), factor(config.dampingRoll));
        float collective = approach(state.collective(), raw.collectiveInput(),
                factor(config.dampingCollective));
        return new State(yaw, pitch, roll, collective);
    }

    /** Rebuilds the pilot input from the damped axes, preserving ground contact. */
    public static HelicopterInput apply(HelicopterInput rawInput, State state) {
        HelicopterInput raw = rawInput.sanitized();
        State s = state == null ? State.initial(raw.yawDeg()) : state;
        return new HelicopterInput(s.yawDeg(), s.pitch(), s.roll(), s.collective(),
                raw.onGround());
    }

    /**
     * Rotates the held yaw with the airframe, used by the coordinated turn. The filter has
     * to follow the aircraft, otherwise it would read the turn itself as a yaw error and
     * drag the nose back to where the mouse left it.
     */
    public static State rotateYaw(State state, float yawDeltaDeg) {
        if (state == null || yawDeltaDeg == 0.0F) {
            return state;
        }
        return new State(HelicopterFlightModel.wrapDegrees(state.yawDeg() + yawDeltaDeg),
                state.pitch(), state.roll(), state.collective());
    }

    private static float approach(float current, float target, float fraction) {
        return current + (target - current) * fraction;
    }

    private static float factor(double configured) {
        if (Double.isNaN(configured)) {
            return 1.0F;
        }
        return (float) Math.max(0.01D, Math.min(1.0D, configured));
    }
}
