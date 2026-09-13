package io.github.littlebird.client;

/**
 * Rotation conventions for the AH-64 model, kept free of Minecraft classes so they can be
 * unit-tested. The model shares the Little Bird's conventions - nose along -Z, a yaw of
 * 180 - entityYaw - and adds the two things only this airframe has: the mast mounted radar
 * and the chain gun turret, which turns relative to the airframe.
 */
public final class ApacheModelMath {
    /** The Longbow radar sweeps once every few seconds; slow enough to read as a scan. */
    public static final float RADAR_DEGREES_PER_TICK = 3.0F;
    /** Tail rotor turns faster than the main rotor, as on the real aircraft. */
    public static final float TAIL_ROTOR_MULTIPLIER = 2.2F;

    private ApacheModelMath() {
    }

    /** Radar sweep angle in radians, wrapped to one turn. */
    public static float radarAngleRadians(float ageInTicks) {
        return (float) Math.toRadians(LittleBirdModelMath.wrap360(
                ageInTicks * RADAR_DEGREES_PER_TICK));
    }

    /**
     * Chain gun turret yaw relative to the airframe. The barrel's attitude is held as an absolute
     * world angle - it follows the gunner's aim rather than the airframe - so the swing the mesh
     * has to show is the difference between the two. The model is mirrored (nose along -Z), so
     * the model rotation is the negative of the relative bearing.
     */
    public static float turretYawRadians(double aimYawDeg, float airframeYawDeg) {
        return (float) Math.toRadians(-relativeBearing(aimYawDeg, airframeYawDeg));
    }

    /**
     * Chain gun turret pitch relative to the airframe. Positive X rotation lifts a -Z barrel, and
     * Minecraft pitch is positive downwards, so the local elevation is the airframe pitch minus
     * the (absolute) aiming pitch.
     */
    public static float turretPitchRadians(double aimPitchDeg, float airframePitchDeg) {
        return (float) Math.toRadians(relativeElevation(aimPitchDeg, airframePitchDeg));
    }

    /** Barrel bearing relative to the nose, wrapped to +/-180 degrees. */
    public static double relativeBearing(double aimYawDeg, float airframeYawDeg) {
        return wrapSigned(finite(aimYawDeg) - airframeYawDeg);
    }

    /** How far the barrel is lifted above the airframe's own pitch, wrapped to +/-180 degrees. */
    public static double relativeElevation(double aimPitchDeg, float airframePitchDeg) {
        return wrapSigned(airframePitchDeg - finite(aimPitchDeg));
    }

    /** Wraps an angle into +/-180: a relative turret angle must never take the long way round. */
    public static double wrapSigned(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        double wrapped = (degrees + 180.0D) % 360.0D;
        if (wrapped < 0.0D) {
            wrapped += 360.0D;
        }
        return wrapped - 180.0D;
    }

    /** True when a rocket pod tube is loaded: tubes empty in firing order. */
    public static boolean tubeLoaded(int tube, int loaded) {
        return tube < Math.max(0, loaded);
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }
}
