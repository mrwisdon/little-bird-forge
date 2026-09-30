package io.github.littlebird.entity.ground;

/**
 * Turns a noisy sampled grade into a smooth hull pitch.
 *
 * <p>The heightmap probe is discrete: crossing a block boundary can flip the sampled grade by
 * several degrees in a single tick, and feeding that straight into the render pitch made the hull
 * shiver. This smooths the grade, drops the small wobble on near flat ground and rate limits the
 * pitch, so the hull reads like it has suspension instead of sampling noise. Pure rules: no
 * Minecraft classes, so it is unit tested.
 */
public final class HullTilt {
    /** Raw grades are clamped here first so a bad probe cannot slam the hull over. */
    public static final double MAX_SAMPLED_GRADE_DEGREES = 60.0D;

    private final double maxPitchDegrees;
    private final double maxRateDegreesPerTick;
    private final double deadbandDegrees;
    private final double smoothing;

    private double grade;
    private double pitch;

    public HullTilt(double maxPitchDegrees, double maxRateDegreesPerTick, double deadbandDegrees,
                    double smoothing) {
        this.maxPitchDegrees = Math.max(0.0D, maxPitchDegrees);
        this.maxRateDegreesPerTick = Math.max(0.0D, maxRateDegreesPerTick);
        this.deadbandDegrees = Math.max(0.0D, deadbandDegrees);
        this.smoothing = Math.min(1.0D, Math.max(0.0D, smoothing));
    }

    /** Feed one tick of raw sampled grade (positive uphill); returns the MC hull pitch. */
    public double update(double rawGradeDegrees) {
        double clamped = clamp(rawGradeDegrees, -MAX_SAMPLED_GRADE_DEGREES,
                MAX_SAMPLED_GRADE_DEGREES);
        grade += (clamped - grade) * smoothing;
        double target = -clamp(grade, -maxPitchDegrees, maxPitchDegrees);
        if (Math.abs(target) < deadbandDegrees) {
            target = 0.0D;
        }
        pitch += clamp(target - pitch, -maxRateDegreesPerTick, maxRateDegreesPerTick);
        return pitch;
    }

    /** The pitch handed out last tick. */
    public double pitch() {
        return pitch;
    }

    /** Forget the slope, for when the driver leaves. */
    public void reset() {
        grade = 0.0D;
        pitch = 0.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
