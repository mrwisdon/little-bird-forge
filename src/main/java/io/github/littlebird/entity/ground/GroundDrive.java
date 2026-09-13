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
                       double turnRateDegreesPerTick, double turnAtStandstillFraction) {
        public Spec {
            maxSpeedBlocksPerTick = Math.max(0.0D, maxSpeedBlocksPerTick);
            acceleration = Math.max(0.0D, acceleration);
            braking = Math.max(0.0D, braking);
            turnRateDegreesPerTick = Math.max(0.0D, turnRateDegreesPerTick);
            turnAtStandstillFraction = clamp(turnAtStandstillFraction, 0.0D, 1.0D);
        }
    }

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
        double target = clamp(throttle, -1.0D, 1.0D) * spec.maxSpeedBlocksPerTick();
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
