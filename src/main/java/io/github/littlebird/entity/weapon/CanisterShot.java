package io.github.littlebird.entity.weapon;

/**
 * The 120mm canister round: a shotgun load of many small pellets instead of one shell.
 *
 * <p>40 pellets, each 40 damage, sprayed out to 40 blocks inside an elliptical cone measured
 * from the crosshair: up to {@link #HORIZONTAL_HALF_DEGREES} either side along the crosshair's
 * horizontal line and {@link #VERTICAL_HALF_DEGREES} up or down from it. Both axes are sampled
 * independently, so no pellet ever exceeds either bound. Close range is brutal; spread makes it
 * fall off fast.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class CanisterShot {
    /** Pellets in one round. */
    public static final int PELLETS = 40;
    /** Half-angle of the horizontal spread, along the crosshair's horizontal line. */
    public static final double HORIZONTAL_HALF_DEGREES = 5.0D;
    /** Half-angle of the vertical spread, above and below the crosshair's horizontal line. */
    public static final double VERTICAL_HALF_DEGREES = 2.0D;
    /** A pellet gives up past this range. */
    public static final double RANGE_BLOCKS = 40.0D;
    public static final float PELLET_DAMAGE = 40.0F;
    public static final float PELLET_VEHICLE_DAMAGE = 40.0F;

    /** One pellet direction with the default 5 wide by 2 tall degree spread. */
    public static LittleBirdBallistics.Vector3 direction(
            LittleBirdBallistics.Vector3 aim, double horizontalSample, double verticalSample) {
        return direction(aim, HORIZONTAL_HALF_DEGREES, VERTICAL_HALF_DEGREES,
                horizontalSample, verticalSample);
    }

    /**
     * One pellet direction from two [0,1] samples: 0.5 is the aim axis itself, 0 and 1 sit on the
     * negative and positive half-angle. The horizontal offset runs along the crosshair's
     * horizontal line; the vertical offset is above or below that line.
     */
    public static LittleBirdBallistics.Vector3 direction(
            LittleBirdBallistics.Vector3 aim, double horizontalHalfDegrees,
            double verticalHalfDegrees, double horizontalSample, double verticalSample) {
        LittleBirdBallistics.Vector3 forward = aim.normalize();
        LittleBirdBallistics.Vector3 right = rightOf(forward);
        LittleBirdBallistics.Vector3 up = right.cross(forward).normalize();
        double horizontal = Math.tan(Math.toRadians(signed(horizontalSample, horizontalHalfDegrees)));
        double vertical = Math.tan(Math.toRadians(signed(verticalSample, verticalHalfDegrees)));
        return forward.add(right.scale(horizontal)).add(up.scale(vertical)).normalize();
    }

    /** Signed angle of a pellet from the aim, along the crosshair's horizontal line. */
    public static double horizontalAngleDegrees(
            LittleBirdBallistics.Vector3 aim, LittleBirdBallistics.Vector3 direction) {
        return angleDegrees(aim, direction, true);
    }

    /** Signed angle of a pellet from the aim, above or below the crosshair's horizontal line. */
    public static double verticalAngleDegrees(
            LittleBirdBallistics.Vector3 aim, LittleBirdBallistics.Vector3 direction) {
        return angleDegrees(aim, direction, false);
    }

    private static double signed(double sample, double halfDegrees) {
        double clamped = Math.max(0.0D, Math.min(1.0D, sample));
        return (clamped * 2.0D - 1.0D) * halfDegrees;
    }

    private static LittleBirdBallistics.Vector3 rightOf(LittleBirdBallistics.Vector3 forward) {
        LittleBirdBallistics.Vector3 reference = Math.abs(forward.y()) < 0.999D
                ? new LittleBirdBallistics.Vector3(0.0D, 1.0D, 0.0D)
                : new LittleBirdBallistics.Vector3(1.0D, 0.0D, 0.0D);
        return forward.cross(reference).normalize();
    }

    private static double angleDegrees(LittleBirdBallistics.Vector3 aim,
                                       LittleBirdBallistics.Vector3 direction, boolean horizontal) {
        LittleBirdBallistics.Vector3 forward = aim.normalize();
        LittleBirdBallistics.Vector3 right = rightOf(forward);
        LittleBirdBallistics.Vector3 axis = horizontal ? right : right.cross(forward).normalize();
        LittleBirdBallistics.Vector3 unit = direction.normalize();
        return Math.toDegrees(Math.atan2(unit.dot(axis), unit.dot(forward)));
    }

    private CanisterShot() {
    }
}
