package io.github.littlebird.entity.weapon;

/**
 * Bombing made easy: where a stick released right now would land, and whether the release is good
 * enough to press the button.
 *
 * <p>The cockpit does not ask the crew to do ballistics in their head. It runs {@link
 * BombBallistics} forward from the aeroplane's own state until the round reaches the ground, marks
 * that spot on the sight, and says READY only when the prediction falls inside the release
 * tolerance of the designation. Arming the automatic release is the answer to pressing early, and
 * a dedicated key does it.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class B2BombAim {
    /**
     * A prediction this close to the designation counts as a good release, in blocks.
     *
     * <p>Three blocks is about a tenth of a second of flight at cruise - two ticks - and it is
     * measured on the prediction that already leads the release delay, so the window really is
     * about the stick that will leave the bay rather than the one the sight drew a moment ago. The
     * crew asked for a tight window: the marker only turns green almost on the line.
     */
    public static final double RELEASE_TOLERANCE_BLOCKS = 3.0D;
    /** Prediction budget; the flight model gives up at the same tick count. */
    public static final int MAX_TICKS = BombBallistics.EXPIRY_TICKS;
    /** How many ticks of the aeroplane's own velocity the release inherits, one to one. */
    public static final double INHERITED_VELOCITY = 1.0D;
    /**
     * Fastest release we believe, in blocks per tick. The B-2 cruises at 1.5 and tops out near 2.0,
     * so anything past this is a teleport or a lost packet rather than flight.
     */
    public static final double MAX_RELEASE_SPEED = 4.0D;

    private B2BombAim() {
    }

    /**
     * Flies a virtual round forward from the release state.
     *
     * @return {impactX, impactZ, ticks} of the predicted ground contact, or {@code null} when the
     *         round never reaches {@code groundY} within {@link #MAX_TICKS}.
     */
    public static double[] predictImpact(double[] releasePos, double[] releaseVel, BombKind kind,
                                         double[] target, boolean guided, double groundY) {
        double[] pos = releasePos == null ? new double[]{0.0D, 0.0D, 0.0D} : releasePos.clone();
        double[] vel = releaseVel == null ? new double[]{0.0D, 0.0D, 0.0D} : releaseVel.clone();
        for (int tick = 1; tick <= MAX_TICKS; tick++) {
            double[] next = BombBallistics.step(pos, vel, kind, target, guided);
            pos[0] = next[0];
            pos[1] = next[1];
            pos[2] = next[2];
            vel[0] = next[3];
            vel[1] = next[4];
            vel[2] = next[5];
            if (BombBallistics.shouldDetonate(pos[1], pos[1] <= groundY)) {
                return new double[]{pos[0], pos[2], tick};
            }
        }
        return null;
    }

    /**
     * The velocity a stick inherits: the aeroplane's own displacement over the last sample.
     *
     * <p>This must come from the position history, never from {@code getDeltaMovement()} or from
     * vanilla's {@code xo/yo/zo}: a ridden vehicle is simulated by the rider's client and the server
     * only follows the reported positions, and {@code absMoveTo} rewrites those history fields to
     * the position it was just handed. Both read zero on the server, which is exactly how the first
     * two versions dropped bombs straight down while the sight predicted a lead.
     */
    public static double[] releaseVelocity(double previousX, double previousY, double previousZ,
                                           double x, double y, double z) {
        return clampReleaseVelocity(x - previousX, y - previousY, z - previousZ);
    }

    /** Bounds a release velocity, so a teleport cannot launch a supersonic stick. */
    public static double[] clampReleaseVelocity(double vx, double vy, double vz) {
        if (!Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) {
            return new double[]{0.0D, 0.0D, 0.0D};
        }
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed <= MAX_RELEASE_SPEED) {
            return new double[]{vx, vy, vz};
        }
        double scale = MAX_RELEASE_SPEED / speed;
        return new double[]{vx * scale, vy * scale, vz * scale};
    }

    /**
     * Mean velocity over the ticks a sample covers.
     *
     * <p>Position samples arrive once per server tick at best, and a dropped packet covers two (or
     * more) ticks at once. Dividing by the ticks actually covered gives the true per tick speed: no
     * lag in a turn, and no doubled speed when a packet goes missing.
     */
    public static double[] averageReleaseVelocity(double dx, double dy, double dz, int ticks) {
        int steps = Math.max(1, ticks);
        return clampReleaseVelocity(dx / steps, dy / steps, dz / steps);
    }

    /**
     * Ticks between the crew seeing "release" and the stick actually leaving the bay: the order has
     * to reach the server (one way latency) and the server releases on its next tick.
     *
     * <p>Every prediction has to be flown from where the aeroplane will be then, otherwise the
     * marker describes a drop the aeroplane never makes and the bomb lands long by the delay times
     * the speed - the "it always drifts" complaint.
     */
    public static int releaseLeadTicks(int pingMillis) {
        int oneWay = (int) Math.round(Math.max(0, pingMillis) / 50.0D);
        return Math.max(0, Math.min(20, 1 + oneWay));
    }

    /** Where the airframe will be after {@code leadTicks} of flying on this velocity. */
    public static double[] leadReleasePosition(double x, double y, double z,
                                               double vx, double vy, double vz, int leadTicks) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(vx) || !Double.isFinite(vy) || !Double.isFinite(vz)) {
            return new double[]{x, y, z};
        }
        double ticks = Math.max(0, Math.min(40, leadTicks));
        return new double[]{x + vx * ticks, y + vy * ticks, z + vz * ticks};
    }

    /** A bomb released from a moving aeroplane carries its velocity; the factor is the tuning knob. */
    public static double[] inheritedVelocity(double vx, double vy, double vz) {
        return new double[]{vx * INHERITED_VELOCITY, vy * INHERITED_VELOCITY,
                vz * INHERITED_VELOCITY};
    }

    /** Horizontal distance from a predicted impact to the designation, in blocks. */
    public static double missDistance(double[] impact, double targetX, double targetZ) {
        if (impact == null) {
            return Double.MAX_VALUE;
        }
        double dx = impact[0] - targetX;
        double dz = impact[1] - targetZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** True when releasing now would put the round within the tolerance of the target. */
    public static boolean releaseReady(double[] impact, double targetX, double targetZ,
                                       double tolerance) {
        return impact != null && missDistance(impact, targetX, targetZ) <= Math.max(0.0D, tolerance);
    }

    /**
     * True when the release needs the automatic trigger because the prediction is still long.
     */
    public static boolean needsAutoRelease(double[] impact, double targetX, double targetZ,
                                           double tolerance) {
        return impact != null && !releaseReady(impact, targetX, targetZ, tolerance);
    }

    /**
     * Whether pressing release early should arm the automatic trigger.
     *
     * <p>Only when the predicted impact is still walking towards the designation. Pressing after
     * the aeroplane has already flown past the target would arm a trigger that can never fire,
     * which reads as "the button is broken".
     *
     * @param miss         current prediction error, in blocks
     * @param previousMiss the same figure one sample ago, {@code NaN} when there is no history
     * @param tolerance    the release tolerance, in blocks
     */
    public static boolean shouldArm(double miss, double previousMiss, double tolerance) {
        if (Double.isNaN(miss) || Double.isNaN(previousMiss)) {
            return false;
        }
        if (miss <= Math.max(0.0D, tolerance)) {
            return false;
        }
        return miss < previousMiss - 1.0E-6D;
    }

    /**
     * Whether the crew may arm the automatic trigger with the dedicated key.
     *
     * <p>Unlike {@link #shouldArm(double, double, double)} this tolerates having no history yet: the
     * key is explicit, so a sight that was just laid on a target must not be refused, and it only
     * refuses a solution that is strictly opening (the aeroplane has already flown past).
     */
    public static boolean canArm(double miss, double previousMiss, double tolerance) {
        if (Double.isNaN(miss) || miss <= Math.max(0.0D, tolerance)) {
            return false;
        }
        if (Double.isNaN(previousMiss)) {
            return true;
        }
        return miss <= previousMiss + 1.0E-6D;
    }

    /**
     * One press does everything: with no designation yet, the sight point becomes the target so
     * the crew never has to designate and then release with two separate keys.
     */
    public static boolean shouldAutoDesignate(boolean hasDesignation, double[] sightPoint) {
        return !hasDesignation && sightPoint != null;
    }
}
