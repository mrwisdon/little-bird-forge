package io.github.littlebird.entity.ground;

/**
 * Ground drive model shared by the pack's tracked and wheeled vehicles.
 *
 * <p>The driver's client is authoritative, exactly as it is for the aircraft: this class turns the
 * two axes a ground vehicle has - throttle and steer - into a new speed, a new heading and the
 * delta movement the entity then hands to {@code move}. It is deliberately parameterised rather
 * than fixed: a main battle tank and a wheeled IFV use the same maths with different specs, but
 * the numbers themselves are the unit's own (see each entity's spec factory).
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class GroundDrive {
    /**
     * Handling of one vehicle. Speeds are blocks per tick, turn rate is degrees per tick.
     * {@code turnAtStandstillFraction} is how much of the full turn rate is available when the
     * vehicle is stopped: a tracked hull can pivot on the spot, a wheeled one cannot.
     */
    public record Spec(double maxSpeedBlocksPerTick, double acceleration, double braking,
                       double turnRateDegreesPerTick, double turnAtStandstillFraction,
                       double maxGradeDegrees) {
        /** Convenience for callers that do not care about the grade limit. */
        public Spec(double maxSpeedBlocksPerTick, double acceleration, double braking,
                    double turnRateDegreesPerTick, double turnAtStandstillFraction) {
            this(maxSpeedBlocksPerTick, acceleration, braking, turnRateDegreesPerTick,
                    turnAtStandstillFraction, DEFAULT_MAX_GRADE_DEGREES);
        }

        public Spec {
            maxSpeedBlocksPerTick = Math.max(0.0D, maxSpeedBlocksPerTick);
            acceleration = Math.max(0.0D, acceleration);
            braking = Math.max(0.0D, braking);
            turnRateDegreesPerTick = Math.max(0.0D, turnRateDegreesPerTick);
            turnAtStandstillFraction = clamp(turnAtStandstillFraction, 0.0D, 1.0D);
            maxGradeDegrees = clamp(maxGradeDegrees, 1.0D, MAX_GRADE_DEGREES);
        }
    }

    /** Grade limit used when a caller does not supply one. */
    public static final double DEFAULT_MAX_GRADE_DEGREES = 30.0D;
    /** Hard cap so a silly sampled slope cannot produce silly numbers. */
    public static final double MAX_GRADE_DEGREES = 60.0D;

    /** What the hull is doing now: forward speed and heading. */
    public record State(double speedBlocksPerTick, double yawDegrees) {
        public static State still(double yawDegrees) {
            return new State(0.0D, yawDegrees);
        }
    }

    /** The next state plus the world-space delta that state produces. */
    public record Step(State state, double dx, double dz) {
    }

    /** Throttle is ahead. */
    public static final int THROTTLE_FORWARD = 1;
    /** Throttle is astern. */
    public static final int THROTTLE_BACK = 2;
    /** Steering left. */
    public static final int STEER_LEFT = 4;
    /** Steering right. */
    public static final int STEER_RIGHT = 8;

    /** Throttle axis from the client's control bits: -1 astern, 0 idle, +1 ahead. */
    public static double throttle(int controls) {
        return ((controls & THROTTLE_FORWARD) != 0 ? 1.0D : 0.0D)
                - ((controls & THROTTLE_BACK) != 0 ? 1.0D : 0.0D);
    }

    /** Steering axis from the client's control bits: -1 left, 0 straight, +1 right. */
    public static double steer(int controls) {
        return ((controls & STEER_RIGHT) != 0 ? 1.0D : 0.0D)
                - ((controls & STEER_LEFT) != 0 ? 1.0D : 0.0D);
    }

    /**
     * One tick of driving. {@code throttle} is -1 (full reverse) to +1 (full ahead), {@code steer}
     * is -1 (left) to +1 (right); both are clamped.
     */
    public static Step advance(State state, double throttle, double steer, Spec spec) {
        return advance(state, throttle, steer, spec, 0.0D);
    }

    /**
     * One tick of driving on a grade. {@code gradeDegrees} is the slope the hull has travelled,
     * positive uphill: the climb eats into the top speed and a drop adds a little. A grade past
     * {@code spec.maxGradeDegrees()} is not something the hull stalls on; the speed just sits at
     * the floor, so a steep climb is slow but never a stuck-in-place loop.
     */
    public static Step advance(State state, double throttle, double steer, Spec spec,
            double gradeDegrees) {
        double grade = clamp(gradeDegrees, -MAX_GRADE_DEGREES, MAX_GRADE_DEGREES);
        double target = clamp(throttle, -1.0D, 1.0D) * spec.maxSpeedBlocksPerTick()
                * gradeSpeedFactor(grade, spec.maxGradeDegrees());
        double speed = state.speedBlocksPerTick();

        if (Math.abs(target - speed) < 1.0E-9D) {
            speed = target;
        } else {
            boolean sameWay = speed == 0.0D || Math.signum(target) == Math.signum(speed);
            double rate = sameWay ? spec.acceleration() : spec.braking();
            if (Math.abs(target) < Math.abs(speed)) {
                rate = spec.braking();
            }
            speed = target > speed
                    ? Math.min(target, speed + rate)
                    : Math.max(target, speed - rate);
        }

        double speedFraction = spec.maxSpeedBlocksPerTick() <= 0.0D
                ? 0.0D
                : Math.min(1.0D, Math.abs(speed) / spec.maxSpeedBlocksPerTick());
        double turnFraction = spec.turnAtStandstillFraction()
                + (1.0D - spec.turnAtStandstillFraction()) * speedFraction;
        double yaw = wrapDegrees(state.yawDegrees()
                + clamp(steer, -1.0D, 1.0D) * spec.turnRateDegreesPerTick() * turnFraction);

        double radians = Math.toRadians(yaw);
        double dx = -Math.sin(radians) * speed;
        double dz = Math.cos(radians) * speed;
        return new Step(new State(speed, yaw), dx, dz);
    }

    /** Grades under this fraction of the vehicle's limit are free: tracks take them as flat. */
    public static final double FREE_GRADE_FRACTION = 0.15D;
    /** Fraction of top speed left AT the vehicle's own grade limit. A crawl, never a stop. */
    public static final double CLIMB_FLOOR = 0.35D;
    /** ...and left at the hard cap, for ground past the limit that still has to move. */
    public static final double STEEPEST_FLOOR = 0.12D;
    /** How much gravity helps on the way down, at the vehicle's own limit. */
    public static final double DESCENT_GAIN = 0.15D;

    /**
     * How much of the flat top speed survives a grade.
     *
     * <p>Three ranges, so a slope reads the way a driver expects it to:
     * <ul>
     *   <li>under {@code FREE_GRADE_FRACTION} of the limit the ground is effectively flat, because
     *       tracks and big tyres do not notice the first few degrees;</li>
     *   <li>up to the limit the cost rises quadratically, so the first degrees of a real climb are
     *       cheap and the last ones are expensive, ending at {@code CLIMB_FLOOR};</li>
     *   <li>past the limit it keeps falling towards {@code STEEPEST_FLOOR} instead of saturating,
     *       so a cliff is genuinely slower than a climb the hull is rated for - the old curve
     *       clamped everything at the limit and made a wall cost exactly as much as a rated slope.
     *       It never reaches zero: the design promise is that steep ground crawls, it never stalls.
     * </ul>
     */
    public static double gradeSpeedFactor(double gradeDegrees, double maxGradeDegrees) {
        double max = Math.max(1.0D, maxGradeDegrees);
        double grade = clamp(gradeDegrees, -MAX_GRADE_DEGREES, MAX_GRADE_DEGREES);
        if (grade < 0.0D) {
            // Rolling downhill: gravity helps, capped at the vehicle's own limit.
            return 1.0D + DESCENT_GAIN * (Math.min(-grade, max) / max);
        }
        double free = max * FREE_GRADE_FRACTION;
        if (grade <= free) {
            return 1.0D;
        }
        if (grade <= max) {
            double t = (grade - free) / (max - free);
            return 1.0D - (1.0D - CLIMB_FLOOR) * t * t;
        }
        double span = Math.max(1.0D, MAX_GRADE_DEGREES - max);
        double over = Math.min(1.0D, (grade - max) / span);
        return CLIMB_FLOOR - (CLIMB_FLOOR - STEEPEST_FLOOR) * over;
    }

    /** Heading into +/-180 so a long drive cannot drift the synced angle. */
    public static double wrapDegrees(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        double wrapped = (degrees + 180.0D) % 360.0D;
        if (wrapped < 0.0D) {
            wrapped += 360.0D;
        }
        return wrapped - 180.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    private GroundDrive() {
    }
}
