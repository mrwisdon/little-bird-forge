package io.github.littlebird.entity.ground;

/** Visual hull attitude and wheel travel fitted to the running gear's contact points. */
public final class GroundSuspension {
    private static final double UNITS_PER_BLOCK = 16.0D;
    private static final double MAX_BODY_DROP = 0.35D;
    private static final double MAX_BODY_LIFT = 0.25D;
    private static final double BODY_RESPONSE = 0.30D;
    private static final double WHEEL_RESPONSE = 0.5D;

    @FunctionalInterface
    interface HeightProbe {
        /** Ground height relative to the entity origin, with positive lateral to the right. */
        double height(double forward, double lateral);
    }

    /** Positive pitch raises the nose, positive roll lowers the right side. */
    public record Pose(float pitchDegrees, float rollDegrees, float liftBlocks) {
    }

    private final double[] axles;
    private final double halfTrack;
    private final double wheelTravel;
    private final double[] leftHeights;
    private final double[] rightHeights;
    private final float[] wheelOffsets;
    private final float[] previousWheelOffsets;
    private final HullPose attitude = new HullPose(45.0D, 28.0D, 3.5D, 0.5D, BODY_RESPONSE);
    private Pose previous = new Pose(0.0F, 0.0F, 0.0F);
    private Pose current = previous;

    public GroundSuspension(float[] axleForwardUnits, float halfTrackUnits, double wheelTravelBlocks) {
        axles = new double[axleForwardUnits.length];
        for (int axle = 0; axle < axles.length; axle++) {
            axles[axle] = axleForwardUnits[axle] / UNITS_PER_BLOCK;
        }
        halfTrack = halfTrackUnits / UNITS_PER_BLOCK;
        wheelTravel = wheelTravelBlocks;
        leftHeights = new double[axles.length];
        rightHeights = new double[axles.length];
        wheelOffsets = new float[wheelTravel > 0.0D ? 2 * axles.length : 0];
        previousWheelOffsets = new float[wheelOffsets.length];
    }

    public Pose pose(float partialTick) {
        if (partialTick == 1.0F) {
            return current;
        }
        return new Pose(lerp(previous.pitchDegrees(), current.pitchDegrees(), partialTick),
                lerp(previous.rollDegrees(), current.rollDegrees(), partialTick),
                lerp(previous.liftBlocks(), current.liftBlocks(), partialTick));
    }

    /** Positive travel raises the tyre toward the chassis. */
    public float wheelOffsetUnits(int axle, boolean left, float partialTick) {
        if (wheelOffsets.length == 0) {
            return 0.0F;
        }
        int wheel = 2 * axle + (left ? 0 : 1);
        return lerp(previousWheelOffsets[wheel], wheelOffsets[wheel], partialTick);
    }

    void update(HeightProbe probe) {
        previous = current;
        double totalForward = 0.0D;
        double totalHeight = 0.0D;
        double totalCrossSlope = 0.0D;
        int supports = 0;
        for (int axle = 0; axle < axles.length; axle++) {
            double left = probe == null ? Double.NaN : probe.height(axles[axle], -halfTrack);
            double right = probe == null ? Double.NaN : probe.height(axles[axle], halfTrack);
            leftHeights[axle] = left;
            rightHeights[axle] = right;
            if (Double.isFinite(left) && Double.isFinite(right)) {
                supports++;
                totalForward += axles[axle];
                totalHeight += (left + right) * 0.5D;
                totalCrossSlope += (left - right) / (2.0D * halfTrack);
            }
        }

        // Regress the axle midpoints to obtain the longitudinal slope. Use paired contacts so
        // one wheel dropping over an edge cannot make the entire axle read as tilted.
        double liftTarget = 0.0D;
        GroundContact.Sample contact = GroundContact.Sample.LEVEL;
        if (supports >= 2) {
            double meanForward = totalForward / supports;
            double meanHeight = totalHeight / supports;
            double variance = 0.0D;
            double covariance = 0.0D;
            for (int axle = 0; axle < axles.length; axle++) {
                if (Double.isFinite(leftHeights[axle]) && Double.isFinite(rightHeights[axle])) {
                    double forward = axles[axle] - meanForward;
                    variance += forward * forward;
                    covariance += forward * ((leftHeights[axle] + rightHeights[axle]) * 0.5D - meanHeight);
                }
            }
            if (variance > 0.0D) {
                double alongSlope = covariance / variance;
                double pitch = Math.atan(alongSlope);
                double roll = Math.atan(totalCrossSlope / supports * Math.cos(pitch));
                double pitchDegrees = Math.toDegrees(pitch);
                double rollDegrees = Math.toDegrees(roll);
                if (Math.abs(pitchDegrees) <= GroundContact.MAX_GRADE_DEGREES
                        && Math.abs(rollDegrees) <= GroundContact.MAX_GRADE_DEGREES) {
                    contact = new GroundContact.Sample(pitchDegrees, rollDegrees, true);
                    liftTarget = clamp(meanHeight - alongSlope * meanForward,
                            -MAX_BODY_DROP, MAX_BODY_LIFT);
                }
            }
        }
        attitude.update(contact);
        current = new Pose((float) attitude.pitchDegrees(), (float) attitude.rollDegrees(),
                lerp(previous.liftBlocks(), (float) liftTarget, BODY_RESPONSE));

        if (wheelOffsets.length > 0) {
            double pitch = Math.toRadians(current.pitchDegrees());
            double roll = Math.toRadians(current.rollDegrees());
            double pitchSlope = Math.tan(pitch);
            double crossRise = halfTrack * Math.tan(roll) / Math.cos(pitch);
            // Project a vertical gap between the ground and the chassis plane onto
            // the suspension axis. A uniform slope needs no extra wheel travel.
            double verticalProjection = Math.cos(pitch) * Math.cos(roll);
            System.arraycopy(wheelOffsets, 0, previousWheelOffsets, 0, wheelOffsets.length);
            for (int axle = 0; axle < axles.length; axle++) {
                double bottom = current.liftBlocks() + axles[axle] * pitchSlope;
                updateWheel(2 * axle, leftHeights[axle] - (bottom + crossRise), verticalProjection);
                updateWheel(2 * axle + 1, rightHeights[axle] - (bottom - crossRise), verticalProjection);
            }
        }
    }

    private void updateWheel(int wheel, double heightError, double verticalProjection) {
        double travel = Double.isFinite(heightError)
                ? clamp(heightError * verticalProjection, -wheelTravel, wheelTravel)
                : -0.5D * wheelTravel;
        wheelOffsets[wheel] = lerp(previousWheelOffsets[wheel],
                (float) (travel * UNITS_PER_BLOCK), WHEEL_RESPONSE);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float lerp(float start, float end, double fraction) {
        return (float) (start + (end - start) * fraction);
    }
}
