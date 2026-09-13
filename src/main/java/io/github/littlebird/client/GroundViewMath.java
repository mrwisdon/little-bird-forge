package io.github.littlebird.client;

/**
 * Which FOV multiplier a ground vehicle camera should use, kept free of Minecraft classes so the
 * isolation between the aircraft camera, the ground exterior camera and the turret interior view is
 * pinned by unit tests rather than by hope.
 */
public final class GroundViewMath {
    private GroundViewMath() {
    }

    /**
     * @param groundVehicle  the local player is riding a tank or an IFV
     * @param interiorView   that vehicle is showing its turret interior camera
     * @param thirdPersonFov the configured exterior multiplier for ground vehicles
     * @param interiorFov    the configured turret interior multiplier
     * @return the multiplier to apply, or 1.0 when this camera family does not own the view
     */
    public static double fovMultiplier(boolean groundVehicle, boolean interiorView,
                                       double thirdPersonFov, double interiorFov) {
        if (!groundVehicle) {
            return 1.0D;
        }
        double wanted = interiorView ? interiorFov : thirdPersonFov;
        if (!Double.isFinite(wanted) || wanted <= 0.0D) {
            return 1.0D;
        }
        return wanted;
    }

    /**
     * The yaw of the camera's frame. While the turret interior view is up the rider and the camera
     * are measured from the turret, so both use the turret's own absolute yaw; every other camera
     * (and the exterior ground view) keeps the hull's yaw, exactly as before.
     *
     * <p>One value drives both the rider offset and the camera yaw, which is what keeps the turret
     * rigidly fixed in the lens: a yaw-only follow would still orbit the shell around a camera that
     * sits off the turret pivot.
     *
     * @param interiorView  that vehicle is showing its turret interior camera
     * @param hullYawDeg    the hull's yaw, used whenever the interior view is not up
     * @param turretYawDeg  the synched absolute turret yaw
     */
    public static float followYawDeg(boolean interiorView, float hullYawDeg, double turretYawDeg) {
        if (!interiorView || !Double.isFinite(turretYawDeg)) {
            return hullYawDeg;
        }
        return (float) turretYawDeg;
    }
}
