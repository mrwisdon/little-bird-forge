package io.github.littlebird.entity.ground;

/**
 * Turns a noisy sampled ground plane into a smooth hull attitude.
 *
 * <p>The probe is discrete: crossing a block boundary can flip the sampled pitch and roll by
 * several degrees in a single tick, and feeding that straight into the render pose made the
 * hull shiver. This smooths both axes, drops the small wobble on near flat ground, rate limits
 * the output, and levels the hull out whenever the contact sample is not usable (mid air, a
 * missed probe, a wall).
 *
 * <p>Positive pitch is nose up and positive roll is the right side lower, matching
 * {@link GroundContact}. Pure rules: no Minecraft classes, so it is unit tested.
 */
public final class HullPose {
    /** Raw samples are clamped here first so a bad probe cannot slam the hull over. */
    public static final double MAX_SAMPLED_GRADE_DEGREES = 60.0D;

    private final double maxPitchDegrees;
    private final double maxRollDegrees;
    private final double maxRateDegreesPerTick;
    private final double deadbandDegrees;
    private final double smoothing;

    private double sampledPitch;
    private double sampledRoll;
    private double pitch;
    private double roll;

    public HullPose(double maxPitchDegrees, double maxRollDegrees, double maxRateDegreesPerTick,
                    double deadbandDegrees, double smoothing) {
        this.maxPitchDegrees = Math.max(0.0D, maxPitchDegrees);
        this.maxRollDegrees = Math.max(0.0D, maxRollDegrees);
        this.maxRateDegreesPerTick = Math.max(0.0D, maxRateDegreesPerTick);
        this.deadbandDegrees = Math.max(0.0D, deadbandDegrees);
        this.smoothing = Math.min(1.0D, Math.max(0.0D, smoothing));
    }

    /** Feeds one tick of ground contact; an invalid sample levels the hull out. */
    public void update(GroundContact.Sample sample) {
        boolean valid = sample != null && sample.valid();
        double pitchTarget = valid ? clamp(sample.pitchDegrees(), -MAX_SAMPLED_GRADE_DEGREES,
                MAX_SAMPLED_GRADE_DEGREES) : 0.0D;
        double rollTarget = valid ? clamp(sample.rollDegrees(), -MAX_SAMPLED_GRADE_DEGREES,
                MAX_SAMPLED_GRADE_DEGREES) : 0.0D;
        sampledPitch += (pitchTarget - sampledPitch) * smoothing;
        sampledRoll += (rollTarget - sampledRoll) * smoothing;

        double targetPitch = clamp(sampledPitch, -maxPitchDegrees, maxPitchDegrees);
        double targetRoll = clamp(sampledRoll, -maxRollDegrees, maxRollDegrees);
        if (Math.abs(targetPitch) < deadbandDegrees) {
            targetPitch = 0.0D;
        }
        if (Math.abs(targetRoll) < deadbandDegrees) {
            targetRoll = 0.0D;
        }
        pitch += clamp(targetPitch - pitch, -maxRateDegreesPerTick, maxRateDegreesPerTick);
        roll += clamp(targetRoll - roll, -maxRateDegreesPerTick, maxRateDegreesPerTick);
    }

    /** Hull pitch handed out last tick, positive nose up. */
    public double pitchDegrees() {
        return pitch;
    }

    /** Hull roll handed out last tick, positive right side lower. */
    public double rollDegrees() {
        return roll;
    }

    /** Forgets the slope, for when the driver leaves. */
    public void reset() {
        sampledPitch = 0.0D;
        sampledRoll = 0.0D;
        pitch = 0.0D;
        roll = 0.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }
}
