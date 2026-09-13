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
}
