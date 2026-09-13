package io.github.littlebird.entity.flight;

/**
 * Roll spin-up damping. Roll is a rate command: the axis the flight model reads is the rate
 * the aircraft should roll at. This slews that axis instead of letting it jump, which is what
 * gives a roll its start-up weight -- the rotor head takes a moment to build the rate up, so
 * the airframe eases into the bank instead of snapping over on the first tick.
 *
 * The release is deliberately not damped. The bank has to stay exactly where the pilot left
 * it, so letting go drops the commanded rate straight to zero.
 *
 * Pure rules, no Minecraft classes.
 */
public final class HelicopterRollDamper {
    private static final float IDLE_EPSILON = 1.0E-3F;

    private HelicopterRollDamper() {
    }

    /**
     * Advances the held roll axis by one tick.
     *
     * @param previousAxis axis the previous tick ended on, -1..1
     * @param commandAxis  smoothed roll command from the damping filter, -1..1
     * @return the axis to hand to the flight model, -1..1
     */
    public static float step(float previousAxis, float commandAxis, FlightConfig config) {
        float command = Float.isFinite(commandAxis) ? commandAxis : 0.0F;
        if (Math.abs(command) < IDLE_EPSILON) {
            return 0.0F;
        }
        float axis = Float.isFinite(previousAxis) ? previousAxis : 0.0F;
        return (float) HelicopterFlightModel.approach(axis, command, stepPerTick(config));
    }

    /** Axis change allowed per tick, from the configured spin-up time. */
    public static double stepPerTick(FlightConfig config) {
        double ticks = config.rollSpinUpTicks;
        if (!(ticks > 0.0D)) {
            return 1.0D;
        }
        return Math.min(1.0D, 1.0D / ticks);
    }
}
