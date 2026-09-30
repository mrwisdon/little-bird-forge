package io.github.littlebird.entity.ground;

/**
 * Fits the hull to the ground plane under it from a small set of probe heights.
 *
 * <p>Four probes at the hull's corners (front-left, front-right, back-left, back-right) are
 * enough to read both the pitch along the hull and the roll across it. Because the probes are
 * the corners of a rectangle, the front/back pair and the left/right pair are independent, so
 * two single-axis differences are enough and no full least-squares plane is needed.
 *
 * <p>Heights are in blocks. Positive pitch is nose up; positive roll is the right side lower.
 * A probe that missed (NaN), a non-finite reading, or a grade past {@link #MAX_GRADE_DEGREES}
 * is reported invalid so the caller keeps its last pose or levels out instead of snapping.
 *
 * <p>Pure rules: no Minecraft classes, so it is unit tested.
 */
public final class GroundContact {
    /** Grades past this are treated as a wall or a bad probe, not a drivable surface. */
    public static final double MAX_GRADE_DEGREES = 60.0D;

    /** One fitted contact plane. */
    public record Sample(double pitchDegrees, double rollDegrees, boolean valid) {
        /** No usable contact: a missed probe or a grade past the limit. */
        public static final Sample LEVEL = new Sample(0.0D, 0.0D, false);
    }

    /**
     * Fits the plane through the four corner heights. {@code halfLengthBlocks} and
     * {@code halfWidthBlocks} are the distances from the hull centre to the probes.
     */
    public static Sample fit(double frontLeft, double frontRight, double backLeft,
                             double backRight, double halfLengthBlocks, double halfWidthBlocks) {
        if (!finite(frontLeft) || !finite(frontRight) || !finite(backLeft) || !finite(backRight)
                || !(halfLengthBlocks > 0.0D) || !(halfWidthBlocks > 0.0D)) {
            return Sample.LEVEL;
        }
        double front = (frontLeft + frontRight) * 0.5D;
        double back = (backLeft + backRight) * 0.5D;
        double left = (frontLeft + backLeft) * 0.5D;
        double right = (frontRight + backRight) * 0.5D;
        double pitch = Math.toDegrees(Math.atan2(front - back, 2.0D * halfLengthBlocks));
        double roll = Math.toDegrees(Math.atan2(left - right, 2.0D * halfWidthBlocks));
        if (!finite(pitch) || !finite(roll)
                || Math.abs(pitch) > MAX_GRADE_DEGREES || Math.abs(roll) > MAX_GRADE_DEGREES) {
            return Sample.LEVEL;
        }
        return new Sample(pitch, roll, true);
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    private GroundContact() {
    }
}
