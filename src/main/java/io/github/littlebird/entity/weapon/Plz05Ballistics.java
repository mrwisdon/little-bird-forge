package io.github.littlebird.entity.weapon;

/**
 * PLZ-05 155mm howitzer trajectory, kept free of Minecraft classes.
 *
 * <p>A howitzer round is a single ballistic body: there is no motor, so the shell leaves the muzzle
 * at full speed and gravity alone bends the arc. The client's impact marker and the server's flying
 * shell step this one code path, which is the only way the ground marker can be guaranteed to agree
 * with where the round lands.
 *
 * <p>The 155mm reaches noticeably further than a 370mm rocket and lands with a much heavier charge,
 * which is why the crater knobs live here: one shell tears a hole far larger than one rocket.
 */
public final class Plz05Ballistics {
    /**
     * Muzzle speed, blocks per tick. Tuned with the gravity below so a 45 degree shot tops out at
     * two hundred blocks: the howitzer is a 200 m weapon now, not a 288 m one.
     */
    public static final double INITIAL_SPEED = 3.0D;
    /** Downward acceleration per tick: a heavy shell, so the arc is long and clearly curved. */
    public static final double GRAVITY = 0.045D;
    /** Flight time limit before the round self destructs. */
    public static final int LIFETIME_TICKS = 400;

    /** Gun limits, degrees. A howitzer both fires flat and lobs. */
    public static final double MIN_ELEVATION_DEG = 3.0D;
    public static final double MAX_ELEVATION_DEG = 70.0D;
    /** One elevation step per tick while slewing: fine laying, 0.3 degrees per tick (6 deg/s). */
    public static final double SLEW_DEGREES_PER_TICK = 0.3D;

    /** One shell in the breech. */
    public static final int SHELLS = 1;
    /** Ticks between two shots; the reload dominates, but a held trigger cannot double fire. */
    public static final int FIRE_INTERVAL_TICKS = 10;

    // ------------------------------------------------------------------
    // Reload quick-time event: a two second bar that keeps sweeping for as long as the reload is
    // running. A press inside the 1.6 - 1.8 second band takes ten seconds off and ends the event;
    // a press anywhere else restarts the reload.
    // ------------------------------------------------------------------
    public static final int QTE_TICKS = 40;
    public static final int QTE_WINDOW_START = 32;
    public static final int QTE_WINDOW_END = 36;
    /** Ticks taken off the reload by a good press: ten seconds. */
    public static final int QTE_REWARD_TICKS = 200;
    /** The reward never takes the clock below this, so the normal refill path always runs. */
    public static final int QTE_MIN_REMAINING = 20;

    /** Ticks before the breech is loaded again: fifteen seconds. */
    public static final int RELOAD_TICKS = 300;

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

    /** Remaining ticks after a good press; never zero so the refill tick always runs. */
    public static int qteReward(int remainingTicks) {
        if (remainingTicks <= QTE_MIN_REMAINING) {
            return remainingTicks;
        }
        return Math.max(QTE_MIN_REMAINING, remainingTicks - QTE_REWARD_TICKS);
    }

    /** A 155mm shell's damage: direct hit and splash. */
    public static final float DIRECT_DAMAGE = 80.0F;
    public static final float VEHICLE_DAMAGE = 60.0F;
    public static final float HEAVY_DAMAGE = 60.0F;
    /** Kill radius: the blast still reaches ten blocks out. */
    public static final float SPLASH_RADIUS = 10.0F;
    public static final float SPLASH_FRACTION = 0.6F;
    /** Vanilla explosion power (entity blast): nine, well past the rocket's seven. */
    public static final float EXPLOSION_POWER = 9.0F;
    /** Default crater radius in blocks: one shell dwarfs one rocket. */
    public static final double DEFAULT_CRATER_RADIUS = 8.0D;

    private Plz05Ballistics() {
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

    /** Clamps an elevation request into the gun's travel. */
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

    /** Unit launch direction for a Minecraft yaw (0 faces +Z) and a gun elevation. */
    public static V3 direction(float yawDeg, double elevationDeg) {
        double yaw = Math.toRadians(yawDeg);
        double elevation = Math.toRadians(clampElevation(elevationDeg));
        double horizontal = Math.cos(elevation);
        return new V3(-Math.sin(yaw) * horizontal, Math.sin(elevation),
                Math.cos(yaw) * horizontal);
    }

    /** Velocity of a freshly launched shell: full muzzle speed, no motor. */
    public static V3 launchVelocity(V3 heading) {
        return heading.normalize().scale(INITIAL_SPEED);
    }

    /** One tick of flight: gravity only. */
    public static V3 advance(V3 velocity, V3 heading, int ageTicks) {
        return velocity.add(new V3(0.0D, -GRAVITY, 0.0D));
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

    /** Minecraft yaw that points from the origin along (dx, dz): yaw zero faces +Z. */
    public static double yawTo(double dx, double dz) {
        return Math.toDegrees(Math.atan2(-dx, dz));
    }
}
