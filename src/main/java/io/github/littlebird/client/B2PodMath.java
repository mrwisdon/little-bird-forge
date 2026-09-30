package io.github.littlebird.client;

/**
 * Pure rules for the bombing electro-optical pod: gimbal limits, the mouse-to-gimbal mapping, the
 * zoom curve, the ray/ground intersection behind the designation and the reticle offset between
 * where the camera looks and where the gimbal points.
 *
 * <p>Angles follow Minecraft's convention: yaw 0 looks towards +Z and grows clockwise seen from
 * above, pitch is positive downwards.
 */
public final class B2PodMath {
    /** How fast the camera follows the gimbal; the gap is what makes the reticle drift. */
    public static final double CAMERA_FOLLOW = 0.55D;

    public record Gimbal(double yawDeg, double pitchDeg) {
    }

    /** Whatever the config says, the sight keeps at least this much downward reach. */
    public static final double MIN_SIGHT_DOWN_DEG = 45.0D;
    /** ... and this much upward reach, so the horizon can always be framed. */
    public static final double MIN_SIGHT_UP_DEG = 2.0D;

    /**
     * The vertical travel the sight really gets, in degrees of magnitude.
     *
     * <p>Minecraft's pitch is positive downwards, which is also the convention the pod camera uses,
     * so the clamp window is {@code [-up, +down]}. A stale or hand-edited config cannot leave the
     * pod unable to look at the ground: the floor above always applies.
     *
     * @return {upDeg, downDeg}, both positive
     */
    public static double[] pitchWindow(double configuredUpDeg, double configuredDownDeg) {
        double up = Math.max(Math.max(0.0D, configuredUpDeg), MIN_SIGHT_UP_DEG);
        double down = Math.max(Math.max(0.0D, configuredDownDeg), MIN_SIGHT_DOWN_DEG);
        return new double[]{up, down};
    }

    /**
     * Player look travel to sight travel.
     *
     * <p>Minecraft grows {@code xRot} downwards, and the gimbal pitch is positive downwards too, so
     * the two agree: pushing the mouse down tilts the sight down. (The first cut negated this and
     * the sight moved the wrong way.)
     */
    public static double[] mouseToGimbal(double deltaYawDeg, double deltaPitchDeg) {
        return new double[]{deltaYawDeg, deltaPitchDeg};
    }

    /**
     * Field of view the sight renders with, in degrees, clamped into a window a crew can use.
     *
     * <p>Both ends are degrees: the tight end is a magnified sight picture, the wide end is close to
     * the vanilla lens. Values outside the window (an old config, a typo) are pulled back in rather
     * than rendering the world through a one degree straw.
     */
    public static double sightFov(double t, double tightFovDeg, double wideFovDeg) {
        double tight = clamp(tightFovDeg, 2.0D, 100.0D);
        double wide = clamp(wideFovDeg, 20.0D, 120.0D);
        if (tight >= wide) {
            tight = Math.max(2.0D, wide * 0.25D);
        }
        return fovFor(t, tight, wide);
    }


    private B2PodMath() {
    }

    public static Gimbal clamp(double yawDeg, double pitchDeg,
                               double yawLimit, double pitchMin, double pitchMax) {
        return new Gimbal(clamp(yawDeg, -yawLimit, yawLimit),
                clamp(pitchDeg, pitchMin, pitchMax));
    }

    /** Mouse delta (degrees) applied to the current gimbal, then clamped. */
    public static Gimbal drag(Gimbal current, double deltaYawDeg, double deltaPitchDeg,
                              double yawLimit, double pitchMin, double pitchMax) {
        return clamp(current.yawDeg() + deltaYawDeg, current.pitchDeg() + deltaPitchDeg,
                yawLimit, pitchMin, pitchMax);
    }

    /** The camera chases the gimbal; the residual is drawn as a moving reticle. */
    public static Gimbal chase(Gimbal camera, Gimbal target, double follow) {
        double t = clamp(follow, 0.0D, 1.0D);
        return new Gimbal(camera.yawDeg() + (target.yawDeg() - camera.yawDeg()) * t,
                camera.pitchDeg() + (target.pitchDeg() - camera.pitchDeg()) * t);
    }

    /** Exponential zoom curve: t=0 is the widest view, t=1 the tightest. */
    public static double fovFor(double t, double fovMin, double fovMax) {
        double clamped = clamp(t, 0.0D, 1.0D);
        return fovMax * Math.pow(fovMin / fovMax, clamped);
    }

    /** Inverse of {@link #fovFor}. */
    public static double zoomFor(double fov, double fovMin, double fovMax) {
        double f = clamp(fov, Math.min(fovMin, fovMax), Math.max(fovMin, fovMax));
        return Math.log(f / fovMax) / Math.log(fovMin / fovMax);
    }

    /** Unit look vector for a gimbal angle, Minecraft axes. */
    public static double[] lookVector(double yawDeg, double pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosPitch = Math.cos(pitch);
        return new double[]{-Math.sin(yaw) * cosPitch, -Math.sin(pitch),
                Math.cos(yaw) * cosPitch};
    }

    /**
     * Where the pod's line of sight meets a horizontal plane.
     *
     * @return {x, y, z} in world space, or {@code null} when the sight line climbs away from it.
     */
    public static double[] groundIntersection(double camX, double camY, double camZ,
                                              double yawDeg, double pitchDeg, double groundY) {
        double[] dir = lookVector(yawDeg, pitchDeg);
        if (dir[1] >= -1.0E-6D) {
            return null;
        }
        double t = (groundY - camY) / dir[1];
        if (t <= 0.0D) {
            return null;
        }
        return new double[]{camX + dir[0] * t, camY + dir[1] * t, camZ + dir[2] * t};
    }

    /** Distance from the pod to a point, in blocks; negative results mean "no point". */
    public static double range(double camX, double camY, double camZ, double[] point) {
        if (point == null) {
            return -1.0D;
        }
        double dx = point[0] - camX;
        double dy = point[1] - camY;
        double dz = point[2] - camZ;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Screen-space reticle offset in degrees: where the pod points minus where the camera looks. */
    public static double[] reticleOffset(Gimbal camera, Gimbal gimbal) {
        return new double[]{wrap(gimbal.yawDeg() - camera.yawDeg()),
                gimbal.pitchDeg() - camera.pitchDeg()};
    }

    /** True when the gimbal is inside the lock window, so a designation may be taken. */
    public static boolean insideLockWindow(Gimbal camera, Gimbal gimbal, double windowDeg) {
        double[] offset = reticleOffset(camera, gimbal);
        return Math.abs(offset[0]) <= windowDeg && Math.abs(offset[1]) <= windowDeg;
    }

    /** Clamps an integer into a range. */
    public static int clampInt(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    /**
     * Projects an angular offset off the sight axis onto the screen, in pixels.
     *
     * @return {x, y}, clamped into the screen so a marker never leaves the glass.
     */
    public static int[] projectToScreen(double offsetYawDeg, double offsetPitchDeg, double fovDeg,
                                       int width, int height) {
        double halfFov = Math.toRadians(Math.max(1.0D, fovDeg)) * 0.5D;
        double tangent = Math.tan(halfFov);
        // The configured field of view is vertical, so the horizontal half angle is wider by the
        // screen aspect ratio: without this the marker drifts sideways on a 16:9 window.
        double aspect = height <= 0 ? 1.0D : (double) width / (double) height;
        double x = width * 0.5D
                + Math.tan(Math.toRadians(offsetYawDeg)) / (tangent * aspect) * width * 0.5D;
        double y = height * 0.5D
                + Math.tan(Math.toRadians(offsetPitchDeg)) / tangent * height * 0.5D;
        return new int[]{clampInt((int) Math.round(x), 0, Math.max(0, width - 1)),
                clampInt((int) Math.round(y), 0, Math.max(0, height - 1))};
    }

    /** True when the angular offset falls outside the sight's field of view. */
    public static boolean offScreen(double offsetYawDeg, double offsetPitchDeg, double fovDeg) {
        double halfFov = Math.max(1.0D, fovDeg) * 0.5D;
        return Math.abs(offsetYawDeg) > halfFov || Math.abs(offsetPitchDeg) > halfFov;
    }

    /** Angular offset of a world point from a camera position and sight axis, {yaw, pitch}. */
    public static double[] angularOffset(double camX, double camY, double camZ,
                                         double camYawDeg, double camPitchDeg,
                                         double[] point) {
        if (point == null) {
            return null;
        }
        double dx = point[0] - camX;
        double dz = point[2] - camZ;
        double dy = point[1] - camY;
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double targetPitch = Math.toDegrees(-Math.atan2(dy, horizontal));
        return new double[]{wrap(targetYaw - camYawDeg), targetPitch - camPitchDeg};
    }

    public static double wrap(double degrees) {
        double wrapped = degrees % 360.0D;
        if (wrapped >= 180.0D) {
            wrapped -= 360.0D;
        }
        if (wrapped < -180.0D) {
            wrapped += 360.0D;
        }
        return wrapped;
    }

    public static double clamp(double value, double min, double max) {
        return value < min ? min : Math.min(value, max);
    }
}
