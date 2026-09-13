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
    /** 600 rpm at 20 ticks per second. */
    public static final int FIRE_INTERVAL_TICKS = 2;
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
    public record State(double aimYawDeg, double aimPitchDeg, int heat, int cooldown,
                        int lockTicks) {
        public static State initial() {
            return new State(0.0D, 0.0D, 0, 0, 0);
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
        return new Aim(normalize(gunnerYawDeg), clampPitch(gunnerPitchDeg));
    }

    /** Aims the barrel at the nose while nobody is in the front seat. */
    public static Aim parkedAim(float airframeYawDeg, float airframePitchDeg) {
        return new Aim(normalize(airframeYawDeg), clampPitch(airframePitchDeg));
    }

    /** Holds the turret on the requested aim, and fires if asked. */
    public static Step advance(State state, Aim desired, boolean trigger) {
        // The barrel holds the gunner's own look directly: no slew delay, so a mouse move is on
        // the crosshair the same tick. Normalize keeps the held angle inside +/-180.
        double yaw = normalize(targetYaw(desired));
        double pitch = targetPitch(desired);

        int heat = Math.max(0, state.heat());
        int cooldown = Math.max(0, state.cooldown());
        int lock = Math.max(0, state.lockTicks());
        boolean fire = false;

        if (lock > 0) {
            // Overheated: the barrel keeps cooling and the trigger does nothing.
            lock--;
            heat = Math.max(0, heat - COOL_PER_TICK);
        } else if (trigger) {
            if (cooldown <= 0) {
                fire = true;
                cooldown = FIRE_INTERVAL_TICKS;
                heat++;
                if (heat >= MAX_HEAT) {
                    heat = MAX_HEAT;
                    lock = OVERHEAT_LOCK_TICKS;
                }
            }
            if (cooldown > 0) {
                // The tick that fires also spends one tick of the interval, so the cadence
                // is exactly FIRE_INTERVAL_TICKS rather than that plus one.
                cooldown--;
            }
        } else {
            cooldown = 0;
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
        double yaw = Math.toRadians(normalize(aimYawDeg));
        double pitch = Math.toRadians(clampPitch(aimPitchDeg));
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
        LittleBirdBallistics.Vector3 forward = unit(airframeForward);
        LittleBirdBallistics.Vector3 up = unit(airframeUp);
        LittleBirdBallistics.Vector3 ring = pivot
                .add(up.scale(pivotHeightBlocks))
                .add(forward.scale(pivotForwardBlocks));
        return ring.add(aimDirection(aimYawDeg, aimPitchDeg).scale(barrelBlocks));
    }

    /** True when the barrel has run into the elevation stop on either axis. */
    public static boolean atElevationStop(double aimPitchDeg) {
        if (!Double.isFinite(aimPitchDeg)) {
            return false;
        }
        return aimPitchDeg >= MAX_ELEVATION_DOWN_DEG - ELEVATION_TOLERANCE_DEG
                || aimPitchDeg <= -MAX_ELEVATION_UP_DEG + ELEVATION_TOLERANCE_DEG;
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
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        return clamp(degrees, -MAX_ELEVATION_UP_DEG, MAX_ELEVATION_DOWN_DEG);
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
