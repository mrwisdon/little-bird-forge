package io.github.littlebird.entity.weapon;

/**
 * Policy for the B-2's bomb run.
 *
 * <p>While the electro-optical pod is up the airframe flies itself: the mouse no longer reaches
 * the flight model, {@link io.github.littlebird.entity.JetEntity} is told to command a zero roll
 * and a zero pitch, and the model's own leveller rolls the wings flat before the first bomb
 * leaves. This class is the pure half of that contract - the numbers and the "is it level yet"
 * test - so the policy can be pinned without a client.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class B2BombRun {
    /** Wings within this many degrees of flat count as level for the crew readout. */
    public static final double LEVEL_ROLL_TOLERANCE_DEG = 3.0D;
    /** Nose within this many degrees of the horizon counts as level. */
    public static final double LEVEL_PITCH_TOLERANCE_DEG = 4.0D;
    /** Commanded pitch during a pod run: the horizon. */
    public static final float LEVEL_PITCH_COMMAND_DEG = 0.0F;
    /** Commanded roll during a pod run: flat. Zero is also what makes the model self-level. */
    public static final double LEVEL_ROLL_COMMAND = 0.0D;

    private B2BombRun() {
    }

    /** True when the airframe is settled enough to release on the pod's designation. */
    public static boolean holdsLevel(double rollDeg, double pitchDeg) {
        return Math.abs(rollDeg) <= LEVEL_ROLL_TOLERANCE_DEG
                && Math.abs(pitchDeg) <= LEVEL_PITCH_TOLERANCE_DEG;
    }

    /** Roll the leveller still has to take out, in degrees. */
    public static double bankErrorDeg(double rollDeg) {
        return -rollDeg;
    }

    /** True when the wings are far enough from flat that the overlay should say so. */
    public static boolean isBanked(double rollDeg, double toleranceDeg) {
        return Math.abs(rollDeg) > Math.max(0.0D, toleranceDeg);
    }

    public static float pitchCommandDeg() {
        return LEVEL_PITCH_COMMAND_DEG;
    }

    public static double rollCommand() {
        return LEVEL_ROLL_COMMAND;
    }
}
