package io.github.littlebird.entity.weapon;

/**
 * 30mm chain gun turret under the nose. The gunner aims it with their look direction, and the
 * barrel points where the gunner is looking - not where the airframe happens to be pointing.
 * Pure rules: no Minecraft classes.
 *
 * <p>The angles held here are the barrel's own <em>absolute</em> attitude in the world: a yaw and
 * a pitch, both measured the way Minecraft measures a player's look. Nothing about the airframe
 * enters the barrel's direction, so a pilot banking or yawing underneath the gunner cannot drag
 * the gun off the target; the turret simply swings further round on its ring to compensate, which
 * is also what makes the chin gun usable from the front seat of an aircraft nobody is flying.
 *
 * <p>Yaw is free through the full circle and the barrel holds the gunner's look with no traverse
 * delay. Elevation keeps the real mount's stops, +11 up and 60 down, applied to the aiming angle
 * itself.
 * Cadence is 600 rounds per minute (one round every 2 ticks) with an overheat lock after 5
 * seconds of continuous fire.
 */
public final class ChainGunTurret {
    /** -60 down / +11 up, the real mount's elevation stops. */
    public static final double MAX_ELEVATION_UP_DEG = 11.0D;
    public static final double MAX_ELEVATION_DOWN_DEG = 60.0D;
    /**
     * Elevation stops of one mount, in degrees. The shared chain gun mount is a low-angle one; a
     * vehicle whose turret is built for high angle fire passes its own stops instead.
     */
    public record Stops(double upDeg, double downDeg) {
        /** Apache chain gun and Bradley 25mm: the real low-angle mount. */
        public static final Stops CHAIN_GUN =
                new Stops(MAX_ELEVATION_UP_DEG, MAX_ELEVATION_DOWN_DEG);
        /** The Wildcat's 30mm: a high-angle mount that can point almost straight up. */
        public static final Stops WILDCAT = new Stops(85.0D, 10.0D);
    }

    /** 600 rpm at 20 ticks per second. */
    public static final int FIRE_INTERVAL_TICKS = 2;

    /** The gun cannot cycle faster than this: a caller asking for less gets the pack default. */
    public static final int MIN_FIRE_INTERVAL_TICKS = 2;

    /**
     * A spent interval counts as spent this close to zero. A fractional cadence leaves fractions of a
     * tick owing, and without this a remainder of 1e-15 would read as "still cooling" and slip the
     * next round by a whole tick, which is the drift the carried remainder exists to prevent.
     */
    private static final double COOLDOWN_EPSILON = 1.0E-6D;
    /** One point of heat per round, so the barrel locks after 50 rounds = 5 seconds. */
    public static final int MAX_HEAT = 50;
    public static final int OVERHEAT_LOCK_TICKS = 80;
    public static final int COOL_PER_TICK = 1;

    /** Damage defaults; the Forge config mirrors these so a server can retune them. */
    public static final float DIRECT_DAMAGE = 8.0F;
    public static final float AIRFRAME_DAMAGE = 3.0F;
    public static final double SPLASH_RADIUS_BLOCKS = 1.0D;
    public static final double SPLASH_DAMAGE_FRACTION = 0.5D;
    public static final double MIN_SPREAD_DEGREES = 0.30D;
    public static final double MAX_SPREAD_DEGREES = 1.20D;

    /**
     * Where the gunner is asking the barrel to point, in absolute world angles. {@code yawDeg}
     * is Minecraft yaw, {@code pitchDeg} is Minecraft pitch (positive looking down).
     */
    public record Aim(double yawDeg, double pitchDeg) {
    }

    /**
     * The barrel's own attitude plus the trigger state. {@code aimYawDeg} and {@code aimPitchDeg}
     * are absolute world angles - they are the same numbers a player's look direction is made of.
     */
    public record State(double aimYawDeg, double aimPitchDeg, int heat, double cooldown,
                        int lockTicks) {
        public static State initial() {
            return new State(0.0D, 0.0D, 0, 0.0D, 0);
        }
    }

    public record Step(State state, boolean fire) {
    }

    private ChainGunTurret() {
    }

    /**
     * The gunner's look direction is exactly what the barrel is asked to follow: there is no
     * airframe term, which is the whole point of a turret that aims independently.
     */
    public static Aim desiredAim(float gunnerYawDeg, float gunnerPitchDeg) {
        return desiredAim(gunnerYawDeg, gunnerPitchDeg, Stops.CHAIN_GUN);
    }

    /** Where the gunner is asking the barrel to point, clamped by this mount's own stops. */
    public static Aim desiredAim(float gunnerYawDeg, float gunnerPitchDeg, Stops stops) {
        return new Aim(normalize(gunnerYawDeg), clampPitch(gunnerPitchDeg, stops));
    }

    /** Aims the barrel at the nose while nobody is in the front seat. */
    public static Aim parkedAim(float airframeYawDeg, float airframePitchDeg) {
        return parkedAim(airframeYawDeg, airframePitchDeg, Stops.CHAIN_GUN);
    }

    public static Aim parkedAim(float airframeYawDeg, float airframePitchDeg, Stops stops) {
        return new Aim(normalize(airframeYawDeg), clampPitch(airframePitchDeg, stops));
    }

    /** Holds the turret on the requested aim, and fires if asked at the built-in cadence. */
    public static Step advance(State state, Aim desired, boolean trigger) {
        return advance(state, desired, trigger, FIRE_INTERVAL_TICKS);
    }

    /** Holds the turret within this mount's stops. */
    public static Step advance(State state, Aim desired, boolean trigger, Stops stops) {
        return advance(state, desired, trigger, FIRE_INTERVAL_TICKS, stops);
    }

    /**
     * Same, with the cadence owned by the caller and counted in whole ticks: the built-in 2 ticks is
     * 600 rounds a minute, which is what the Wildcat's 30mm runs.
     */
    public static Step advance(State state, Aim desired, boolean trigger, int intervalTicks) {
        return advance(state, desired, trigger, (double) intervalTicks);
    }

    /**
     * Same, with a cadence that does not have to be a whole number of ticks.
     *
     * <p>A real rate of fire rarely lands on whole ticks: 180 rounds a minute is one round every six
     * and two thirds, and rounding that up to seven would quietly hand the crew 171. The remainder is
     * therefore carried in {@code cooldown} - a shot adds the interval to what is left of the last
     * one instead of resetting it - so the spacing alternates 7, 7, 6 and the average rate is exact
     * over any window instead of drifting slower.
     */
    public static Step advance(State state, Aim desired, boolean trigger, double intervalTicks) {
        return advance(state, desired, trigger, intervalTicks, Stops.CHAIN_GUN);
    }

    /** Same, with the mount's own elevation stops instead of the shared chain gun's. */
    public static Step advance(State state, Aim desired, boolean trigger, double intervalTicks,
                               Stops stops) {
        double interval = Double.isFinite(intervalTicks)
                ? Math.max(MIN_FIRE_INTERVAL_TICKS, intervalTicks)
                : FIRE_INTERVAL_TICKS;
        // The barrel holds the gunner's own look directly: no slew delay, so a mouse move is on
        // the crosshair the same tick. Normalize keeps the held angle inside +/-180.
        double yaw = normalize(targetYaw(desired));
        double pitch = clampPitch(desired.pitchDeg(), stops);

        int heat = Math.max(0, state.heat());
        // Not clamped up to zero: a spent interval can leave a fraction of a tick owing, and that
        // fraction is the whole reason a fractional cadence keeps its rate.
        double cooldown = Double.isFinite(state.cooldown()) ? state.cooldown() : 0.0D;
        int lock = Math.max(0, state.lockTicks());
        boolean fire = false;

        if (lock > 0) {
            // Overheated: the barrel keeps cooling and the trigger does nothing.
            lock--;
            heat = Math.max(0, heat - COOL_PER_TICK);
        } else if (trigger) {
            if (cooldown <= COOLDOWN_EPSILON) {
                fire = true;
                cooldown += interval;
                heat++;
                if (heat >= MAX_HEAT) {
                    heat = MAX_HEAT;
                    lock = OVERHEAT_LOCK_TICKS;
                }
            }
            if (cooldown > 0.0D) {
                // The tick that fires also spends one tick of the interval, so the cadence
                // is exactly the interval rather than that plus one.
                cooldown--;
            }
        } else {
            // Releasing the trigger must not rearm the gun. A chain gun's interval is the time it
            // takes to cycle, not a property of the trigger, so clearing it here handed every fresh
            // click a free shot and let click-spam fire far faster than the configured rate. The
            // countdown therefore keeps running while the trigger is up - clamped at zero so a long
            // idle cannot bank a burst - which is exactly how the tank's main gun has always worked.
            cooldown = Math.max(0.0D, cooldown - 1.0D);
            heat = Math.max(0, heat - COOL_PER_TICK);
        }

        return new Step(new State(yaw, pitch, heat, cooldown, lock), fire);
    }

    /**
     * World direction the barrel points, built from its own attitude alone. Minecraft's
     * convention: x = -sin(yaw)cos(pitch), y = -sin(pitch), z = cos(yaw)cos(pitch), with pitch
     * positive downwards.
     */
    public static LittleBirdBallistics.Vector3 aimDirection(
            double aimYawDeg, double aimPitchDeg) {
        return aimDirection(aimYawDeg, aimPitchDeg, Stops.CHAIN_GUN);
    }

    /** Direction for a mount with its own elevation stops. */
    public static LittleBirdBallistics.Vector3 aimDirection(
            double aimYawDeg, double aimPitchDeg, Stops stops) {
        double yaw = Math.toRadians(normalize(aimYawDeg));
        double pitch = Math.toRadians(clampPitch(aimPitchDeg, stops));
        double cosPitch = Math.cos(pitch);
        return new LittleBirdBallistics.Vector3(
                -Math.sin(yaw) * cosPitch,
                -Math.sin(pitch),
                Math.cos(yaw) * cosPitch);
    }

    /**
     * World muzzle position: the turret ring is bolted to the airframe, so its pivot rides the
     * airframe's own forward and up axes, and only the barrel's reach follows the aim. This is
     * why the muzzle can sit off the aiming line while the round still flies down it.
     */
    public static LittleBirdBallistics.Vector3 muzzleOrigin(
            LittleBirdBallistics.Vector3 pivot,
            LittleBirdBallistics.Vector3 airframeForward,
            LittleBirdBallistics.Vector3 airframeUp,
            double pivotHeightBlocks, double pivotForwardBlocks, double barrelBlocks,
            double aimYawDeg, double aimPitchDeg) {
        return muzzleOrigin(pivot, airframeForward, airframeUp, pivotHeightBlocks,
                pivotForwardBlocks, barrelBlocks, aimYawDeg, aimPitchDeg, Stops.CHAIN_GUN);
    }

    /** Muzzle for a mount with its own elevation stops. */
    public static LittleBirdBallistics.Vector3 muzzleOrigin(
            LittleBirdBallistics.Vector3 pivot,
            LittleBirdBallistics.Vector3 airframeForward,
            LittleBirdBallistics.Vector3 airframeUp,
            double pivotHeightBlocks, double pivotForwardBlocks, double barrelBlocks,
            double aimYawDeg, double aimPitchDeg, Stops stops) {
        LittleBirdBallistics.Vector3 forward = unit(airframeForward);
        LittleBirdBallistics.Vector3 up = unit(airframeUp);
        LittleBirdBallistics.Vector3 ring = pivot
                .add(up.scale(pivotHeightBlocks))
                .add(forward.scale(pivotForwardBlocks));
        return ring.add(aimDirection(aimYawDeg, aimPitchDeg, stops).scale(barrelBlocks));
    }

    /** True when the barrel has run into the elevation stop on either axis. */
    public static boolean atElevationStop(double aimPitchDeg) {
        return atElevationStop(aimPitchDeg, Stops.CHAIN_GUN);
    }

    /** True when the barrel has run into this mount's stop on either end. */
    public static boolean atElevationStop(double aimPitchDeg, Stops stops) {
        if (!Double.isFinite(aimPitchDeg)) {
            return false;
        }
        return aimPitchDeg >= stops.downDeg() - ELEVATION_TOLERANCE_DEG
                || aimPitchDeg <= -stops.upDeg() + ELEVATION_TOLERANCE_DEG;
    }

    /** Dispersion grows linearly with heat, like the Little Bird's gun. */
    public static double spreadDegrees(int heat) {
        double fraction = clamp(heat / (double) MAX_HEAT, 0.0D, 1.0D);
        return MIN_SPREAD_DEGREES + (MAX_SPREAD_DEGREES - MIN_SPREAD_DEGREES) * fraction;
    }

    private static final double ELEVATION_TOLERANCE_DEG = 0.5D;

    private static double targetYaw(Aim desired) {
        return Double.isFinite(desired.yawDeg()) ? desired.yawDeg() : 0.0D;
    }

    private static double targetPitch(Aim desired) {
        return clampPitch(desired.pitchDeg());
    }

    /** Yaw into +/-180; a NaN aiming angle holds the nose-on attitude instead of breaking. */
    private static double normalize(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        double wrapped = (degrees + 180.0D) % 360.0D;
        if (wrapped < 0.0D) {
            wrapped += 360.0D;
        }
        return wrapped - 180.0D;
    }

    /**
     * Minecraft pitch is positive downwards, so the mount's 11 degrees of elevation up is a
     * pitch of -11 and its 60 degrees of depression is a pitch of +60.
     */
    private static double clampPitch(double degrees) {
        return clampPitch(degrees, Stops.CHAIN_GUN);
    }

    private static double clampPitch(double degrees, Stops stops) {
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        return clamp(degrees, -stops.upDeg(), stops.downDeg());
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    /** Unit version of an axis vector; a degenerate axis contributes nothing instead of NaN. */
    private static LittleBirdBallistics.Vector3 unit(LittleBirdBallistics.Vector3 value) {
        if (value == null) {
            return new LittleBirdBallistics.Vector3(0.0D, 0.0D, 0.0D);
        }
        double length = value.length();
        if (!Double.isFinite(length) || length < 1.0E-9D) {
            return new LittleBirdBallistics.Vector3(0.0D, 0.0D, 0.0D);
        }
        return value.scale(1.0D / length);
    }
}
