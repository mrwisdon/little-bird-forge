package io.github.littlebird.entity.ground;

/**
 * The slope the hull has actually travelled, in degrees (positive uphill).
 *
 * <p>A probe of the ground ahead noses the hull up and slows the drive before the vehicle is on the
 * slope, and on blocky ground its reading flips as the probe crosses block edges. Measuring the
 * movement that really happened - vertical over the horizontal the drive asked for, averaged over a
 * short window - cannot do either: a single step averages into the grade it is part of, flat ground
 * reads zero, and standing still levels back out. Pure rules: unit tested.
 */
public final class SlopeTracker {
    /** Weight kept each tick; 0.95 is a window of about twenty ticks. */
    private static final double DECAY = 0.95D;
    /** Faster decay while the hull is not moving, so a parked vehicle levels out. */
    private static final double IDLE_DECAY = 0.80D;
    private static final double MIN_MOVE = 1.0E-4D;

    private double sumDy;
    private double sumDh;
    private double grade;

    /**
     * Feed one tick of movement: {@code dy} is how much the hull actually climbed, {@code dh} is
     * the horizontal distance the drive asked for (not the clipped one, so a blocked lip does not
     * read as a vertical cliff).
     */
    public double update(double dy, double dh) {
        if (!Double.isFinite(dy) || !Double.isFinite(dh) || dh < 0.0D) {
            return grade;
        }
        if (dh < MIN_MOVE) {
            sumDy *= IDLE_DECAY;
            sumDh *= IDLE_DECAY;
            if (Math.abs(sumDy) < MIN_MOVE && sumDh < MIN_MOVE) {
                sumDy = 0.0D;
                sumDh = 0.0D;
                grade = 0.0D;
            }
            return grade;
        }
        sumDy = sumDy * DECAY + dy;
        sumDh = sumDh * DECAY + dh;
        if (sumDh > MIN_MOVE) {
            grade = Math.toDegrees(Math.atan2(sumDy, sumDh));
        }
        return grade;
    }

    public double gradeDegrees() {
        return grade;
    }

    public void reset() {
        sumDy = 0.0D;
        sumDh = 0.0D;
        grade = 0.0D;
    }
}
