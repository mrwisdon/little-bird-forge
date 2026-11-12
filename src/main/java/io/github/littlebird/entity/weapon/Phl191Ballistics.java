package io.github.littlebird.entity.weapon;

/**
 * PHL-191 rocket trajectory, kept free of Minecraft classes.
 *
 * <p>The server projectile and the client impact marker both step this one code path, which is the
 * only way the ground marker can be guaranteed to agree with where the salvo actually lands. The
 * motor burns for {@link #BURN_TICKS} along the launch axis and the round is ballistic afterwards;
 * gravity is a separate accumulating component, exactly like the Little Bird's 70mm rockets but
 * with a much heavier round, so the arc is visibly curved instead of flat.
 */
public final class Phl191Ballistics {
    /** Muzzle speed, blocks per tick (40 m/s). */
    public static final double INITIAL_SPEED = 2.0D;
    /** Motor acceleration along the launch axis while it burns. */
    public static final double THRUST_PER_TICK = 0.06D;
    /** Speed the motor drives the round to, blocks per tick. */
    public static final double CRUISE_SPEED = 2.72D;
    /** Ticks of motor burn. */
    public static final int BURN_TICKS = 12;
    /** Downward acceleration per tick, heavier than a 70mm rocket so the arc reads as artillery. */
    public static final double GRAVITY = 0.05D;
    /** Flight time limit before the round self destructs. */
    public static final int LIFETIME_TICKS = 240;

    /** Launcher limits, degrees. */
    public static final double MIN_ELEVATION_DEG = 5.0D;
    public static final double MAX_ELEVATION_DEG = 65.0D;
    /** One elevation step per tick while slewing to the driver's aim. */
    public static final double SLEW_DEGREES_PER_TICK = 2.5D;

    /** Eight tubes, two 2x2 pods. */
    public static final int TUBES = 8;
    /** Ticks between two rockets of one ripple. */
    public static final int FIRE_INTERVAL_TICKS = 6;
    // ------------------------------------------------------------------
    // Reload quick-time event: a two second bar that keeps sweeping for as long as the reload is
    // running. A press inside the 1.6 - 1.8 second band takes fifteen seconds off and ends the
    // event; a press anywhere else restarts the reload. The bar only goes away on a good press or
    // when the reload finishes on its own.
    // ------------------------------------------------------------------
    /** One sweep of the QTE bar, in ticks (two seconds). */
    public static final int QTE_TICKS = 40;
    /** Start of the scoring window inside a sweep (1.6 s). */
    public static final int QTE_WINDOW_START = 32;
    /** End of the scoring window inside a sweep (1.8 s). */
    public static final int QTE_WINDOW_END = 36;
    /** Ticks taken off the reload by a good press: fifteen seconds. */
    public static final int QTE_REWARD_TICKS = 300;
    /** The reward never takes the clock below this, so the normal refill path always runs. */
    public static final int QTE_MIN_REMAINING = 20;

    /** Ticks of reload already served. */
    public static int qteElapsed(int remainingTicks) {
        return RELOAD_TICKS - Math.max(0, remainingTicks);
    }

    /** Position inside the current two second sweep, 0..QTE_TICKS-1. */
    public static int qtePhase(int remainingTicks) {
        return Math.floorMod(qteElapsed(remainingTicks), QTE_TICKS);
    }

    /** The bar keeps sweeping until a good press ends it or the reload finishes. */
    public static boolean qteActive(int remainingTicks, boolean rewarded) {
        return !rewarded && remainingTicks > 0;
    }

    /** True when a press lands inside the 1.6 - 1.8 second band of the current sweep. */
    public static boolean qteInWindow(int remainingTicks) {
        int phase = qtePhase(remainingTicks);
        return phase >= QTE_WINDOW_START && phase < QTE_WINDOW_END;
    }

    /**
     * Remaining ticks after a good press. Never zero: a reward that finished the reload outright
     * would skip the tick that puts the rockets back in the tubes, which left the hull showing
     * 0/8 with a finished clock.
     */
    public static int qteReward(int remainingTicks) {
        if (remainingTicks <= QTE_MIN_REMAINING) {
            return remainingTicks;
        }
        return Math.max(QTE_MIN_REMAINING, remainingTicks - QTE_REWARD_TICKS);
    }

    /** Ticks before the empty pods are reloaded: twenty seconds. */
    public static final int RELOAD_TICKS = 400;

    /** A rocket's damage: direct hit and splash. */
    public static final float DIRECT_DAMAGE = 60.0F;
    public static final float VEHICLE_DAMAGE = 40.0F;
    public static final float HEAVY_DAMAGE = 40.0F;
    public static final float SPLASH_RADIUS = 7.0F;
    public static final float SPLASH_FRACTION = 0.6F;

    private Phl191Ballistics() {
    }

    /** Immutable 3-vector, the same shape the other weapon rules use. */
    public record V3(double x, double y, double z) {
        public V3 add(V3 other) {
            return new V3(x + other.x, y + other.y, z + other.z);
        }

        public V3 scale(double factor) {
            return new V3(x * factor, y * factor, z * factor);
        }

        public double dot(V3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        public double length() {
            return Math.sqrt(x * x + y * y + z * z);
        }

        public V3 normalize() {
            double len = length();
            return len < 1.0E-9D ? new V3(0.0D, 0.0D, 1.0D)
                    : new V3(x / len, y / len, z / len);
        }
    }

    /** Clamps an elevation request into the launcher's travel. */
    public static double clampElevation(double degrees) {
        if (!Double.isFinite(degrees)) {
            return MIN_ELEVATION_DEG;
        }
        return Math.max(MIN_ELEVATION_DEG, Math.min(MAX_ELEVATION_DEG, degrees));
    }

    /** One slew step towards a requested angle, never overshooting. */
    public static double slew(double current, double requested) {
        double target = clampElevation(requested);
        double delta = target - current;
        if (Math.abs(delta) <= SLEW_DEGREES_PER_TICK) {
            return target;
        }
        return current + Math.signum(delta) * SLEW_DEGREES_PER_TICK;
    }

    /** One held-key elevation step: +1 raises, -1 lowers, 0 holds. */
    public static double slewBy(double current, int direction) {
        if (direction == 0) {
            return clampElevation(current);
        }
        return clampElevation(current + Math.signum(direction) * SLEW_DEGREES_PER_TICK);
    }

    /** Unit launch direction for a Minecraft yaw (0 faces +Z) and a launcher elevation. */
    public static V3 direction(float yawDeg, double elevationDeg) {
        double yaw = Math.toRadians(yawDeg);
        double elevation = Math.toRadians(clampElevation(elevationDeg));
        double horizontal = Math.cos(elevation);
        return new V3(-Math.sin(yaw) * horizontal, Math.sin(elevation),
                Math.cos(yaw) * horizontal);
    }

    /** Velocity of a freshly launched rocket. */
    public static V3 launchVelocity(V3 heading) {
        return heading.normalize().scale(INITIAL_SPEED);
    }

    /**
     * One tick of flight: motor thrust along the launch axis while it burns, then gravity on top.
     * Thrusting along the current velocity would nose the round over and flatten the arc, so the
     * heading - the direction it was fired in - is what the motor pushes along.
     */
    public static V3 advance(V3 velocity, V3 heading, int ageTicks) {
        V3 axis = heading.length() < 1.0E-6D ? new V3(0.0D, 0.0D, 1.0D) : heading.normalize();
        V3 result = velocity;
        if (ageTicks < BURN_TICKS) {
            double axial = velocity.dot(axis);
            double target = Math.min(CRUISE_SPEED, axial + THRUST_PER_TICK);
            result = velocity.add(axis.scale(target - axial));
        }
        return result.add(new V3(0.0D, -GRAVITY, 0.0D));
    }

    /** True once the round has flown its full lifetime. */
    public static boolean expired(int ageTicks) {
        return ageTicks > LIFETIME_TICKS;
    }

    /**
     * Horizontal range of a shot fired at this elevation over flat ground, in blocks. Steps the
     * same trajectory the projectile uses, so the fire-control hint and the round agree.
     */
    public static double rangeAt(double elevationDeg) {
        V3 heading = direction(0.0F, elevationDeg);
        V3 velocity = launchVelocity(heading);
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
        for (int tick = 1; tick <= LIFETIME_TICKS; tick++) {
            velocity = advance(velocity, heading, tick);
            x += velocity.x();
            y += velocity.y();
            z += velocity.z();
            if (tick > 1 && y <= 0.0D) {
                // Yaw zero fires along +Z, so the horizontal distance spans both axes.
                return Math.sqrt(x * x + z * z);
            }
        }
        return Math.sqrt(x * x + z * z);
    }

    /**
     * Lowest elevation that reaches this many blocks, or the maximum stop when the distance is out
     * of range. Searches the same range function the round flies, half a degree at a time.
     */
    public static double requiredElevation(double distanceBlocks) {
        if (!Double.isFinite(distanceBlocks) || distanceBlocks <= 0.0D) {
            return MIN_ELEVATION_DEG;
        }
        double best = MIN_ELEVATION_DEG;
        double bestError = Double.MAX_VALUE;
        for (double degrees = MIN_ELEVATION_DEG; degrees <= MAX_ELEVATION_DEG; degrees += 0.5D) {
            double error = Math.abs(rangeAt(degrees) - distanceBlocks);
            if (error < bestError) {
                bestError = error;
                best = degrees;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------
    // Salvo dispersion, as a firing-angle deviation from the hull's heading: every tube leaves on
    // a slightly different bearing (yaw), and the second half of the ripple also carries a small
    // elevation tolerance (pitch), so the eight impacts spread across the target in both
    // directions. The lateral slots sit on a four column grid, the depth on two rows; the yaw is
    // sized from the range the round will actually fly, so the lateral span stays the same near or
    // far, while the pitch trim is a fixed tolerance - it is a mounting tolerance, not a way to
    // re-aim the salvo.
    // ------------------------------------------------------------------

    /** Columns of the salvo grid; the rows are low/high elevation groups. */
    public static final int SALVO_COLUMNS = 4;

    /** Lateral slot for one tube, in blocks, evenly spread from -radius to +radius. */
    public static double salvoLateralOffset(int tube, int tubes, double radius, double jitter) {
        int columns = Math.min(SALVO_COLUMNS, Math.max(1, tubes));
        if (!(radius > 0.0D) || !Double.isFinite(radius) || columns < 2) {
            return 0.0D;
        }
        int column = Math.floorMod(tube, columns);
        double fraction = column / (double) (columns - 1) * 2.0D - 1.0D;
        double step = 2.0D * radius / (columns - 1);
        return radius * fraction + (jitter - 0.5D) * step * 0.5D;
    }

    /** True when this tube belongs to the high row of the salvo grid. */
    public static boolean salvoHighRow(int tube, int tubes) {
        int columns = Math.min(SALVO_COLUMNS, Math.max(1, tubes));
        int index = Math.floorMod(tube, Math.max(1, tubes));
        return (index / columns) % 2 == 1;
    }

    /** Elevation tolerance for one tube: the low row fires at -tolerance, the high at +tolerance. */
    public static double salvoPitchTrim(int tube, int tubes, double toleranceDeg, double jitter) {
        if (!(toleranceDeg > 0.0D) || !Double.isFinite(toleranceDeg) || tubes <= SALVO_COLUMNS) {
            return 0.0D;
        }
        double sign = salvoHighRow(tube, tubes) ? 1.0D : -1.0D;
        return sign * toleranceDeg * (0.8D + 0.4D * jitter);
    }

    /** Yaw offset in degrees that slides the impact sideways by this many blocks at this range. */
    public static double yawOffsetForLateral(double rangeBlocks, double lateralBlocks) {
        if (!(rangeBlocks > 0.0D) || !Double.isFinite(rangeBlocks)) {
            return 0.0D;
        }
        return Math.toDegrees(Math.atan2(lateralBlocks, rangeBlocks));
    }

    /** Lateral impact offset produced by a yaw offset at this range, in blocks. */
    public static double lateralImpact(double rangeBlocks, double yawOffsetDeg) {
        return rangeBlocks * Math.tan(Math.toRadians(yawOffsetDeg));
    }

    /** Minecraft yaw that points from the origin along (dx, dz): yaw zero faces +Z. */
    public static double yawTo(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }
}
