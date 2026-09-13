package io.github.littlebird.entity.ground;

import io.github.littlebird.entity.flight.HelicopterFlightModel;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;

/**
 * The main battle tank's 120mm turret: a slow, heavy single-shot gun.
 *
 * <p>It is a different beast from the Apache's {@code ChainGunTurret}, and deliberately not built
 * on it: that one is a fast, heat-limited autocannon, while this one slews its turret slowly,
 * holds a small magazine and spends seconds between rounds. Keeping the cadence model separate is
 * what stops a tank from inheriting an autocannon's feel (and vice versa).
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class MainGun {
    /**
     * The mount's stops, and the authoritative pair for the tank's turret: 30 degrees of elevation,
     * 10 of depression. Minecraft pitch is positive downwards, so the clamp range is [-30, +10].
     */
    public static final double MAX_ELEVATION_UP_DEG = 30.0D;
    public static final double MAX_ELEVATION_DOWN_DEG = 10.0D;
    /** Slow traverse: 60 degrees per second at 20 ticks per second. */
    public static final double SLEW_DEGREES_PER_TICK = 3.0D;

    /** Where the shooter is asking the barrel to point, in absolute world angles. */
    public record Aim(double yawDegrees, double pitchDegrees) {
    }

    /** The barrel's own attitude plus the time left before it can fire again. */
    public record State(double aimYawDegrees, double aimPitchDegrees, int cooldownTicks) {
        public static State initial() {
            return new State(0.0D, 0.0D, 0);
        }
    }

    public record Step(State state, boolean fire) {
    }

    /** The driver's look direction, clamped to the mount's elevation stops. */
    public static Aim desiredAim(float yawDegrees, float pitchDegrees) {
        return new Aim(wrap(yawDegrees), clampPitch(pitchDegrees));
    }

    /** Advances the turret one tick and reports whether the gun went off. */
    public static Step advance(State state, Aim desired, boolean trigger, int fireIntervalTicks) {
        double yaw = wrap(HelicopterFlightModel.approachAngle(
                state.aimYawDegrees(), targetYaw(desired), SLEW_DEGREES_PER_TICK));
        double pitch = clampPitch(HelicopterFlightModel.approach(
                state.aimPitchDegrees(), clampPitch(desired.pitchDegrees()),
                SLEW_DEGREES_PER_TICK));

        int cooldown = Math.max(0, state.cooldownTicks());
        boolean fire = false;
        if (trigger) {
            if (cooldown <= 0) {
                fire = true;
                cooldown = Math.max(1, fireIntervalTicks);
            }
            cooldown--;
        } else {
            cooldown = 0;
        }
        return new Step(new State(yaw, pitch, cooldown), fire);
    }

    /** World direction the barrel points, Minecraft's yaw/pitch convention. */
    public static LittleBirdBallistics.Vector3 aimDirection(double yawDegrees, double pitchDegrees) {
        double yaw = Math.toRadians(wrap(yawDegrees));
        double pitch = Math.toRadians(clampPitch(pitchDegrees));
        double cosPitch = Math.cos(pitch);
        return new LittleBirdBallistics.Vector3(
                -Math.sin(yaw) * cosPitch,
                -Math.sin(pitch),
                Math.cos(yaw) * cosPitch);
    }

    /** World muzzle position: turret ring on the hull, barrel along the aim. */
    public static LittleBirdBallistics.Vector3 muzzleOrigin(
            LittleBirdBallistics.Vector3 pivot,
            LittleBirdBallistics.Vector3 hullForward,
            LittleBirdBallistics.Vector3 hullUp,
            double pivotHeightBlocks, double pivotForwardBlocks, double barrelBlocks,
            double aimYawDegrees, double aimPitchDegrees) {
        LittleBirdBallistics.Vector3 forward = unit(hullForward);
        LittleBirdBallistics.Vector3 up = unit(hullUp);
        LittleBirdBallistics.Vector3 ring = pivot
                .add(up.scale(pivotHeightBlocks))
                .add(forward.scale(pivotForwardBlocks));
        return ring.add(aimDirection(aimYawDegrees, aimPitchDegrees).scale(barrelBlocks));
    }

    /** True when the barrel has run into either elevation stop. */
    public static boolean atElevationStop(double aimPitchDegrees) {
        if (!Double.isFinite(aimPitchDegrees)) {
            return false;
        }
        return aimPitchDegrees >= MAX_ELEVATION_DOWN_DEG - 0.5D
                || aimPitchDegrees <= -MAX_ELEVATION_UP_DEG + 0.5D;
    }

    private static double targetYaw(Aim desired) {
        return Double.isFinite(desired.yawDegrees()) ? desired.yawDegrees() : 0.0D;
    }

    private static double wrap(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        return GroundDrive.wrapDegrees(degrees);
    }

    private static double clampPitch(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        return Math.max(-MAX_ELEVATION_UP_DEG, Math.min(MAX_ELEVATION_DOWN_DEG, degrees));
    }

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

    private MainGun() {
    }
}
