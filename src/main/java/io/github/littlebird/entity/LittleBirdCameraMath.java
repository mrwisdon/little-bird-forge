package io.github.littlebird.entity;

/**
 * Camera offset maths for the over-the-shoulder view, kept free of Minecraft
 * classes so the direction conventions can be unit-tested.
 *
 * Minecraft forwards a yaw as (-sin yaw, 0, cos yaw); the camera's right is then
 * (-cos yaw, 0, -sin yaw), so the aircraft's left is (cos yaw, 0, sin yaw). Shifting
 * the hidden rider towards the left puts the airframe on the right of the screen,
 * which is the framing the reference screenshot uses.
 */
public final class LittleBirdCameraMath {
    private LittleBirdCameraMath() {
    }

    public record Offset(double x, double y, double z) {
    }

    /** side > 0 moves towards the aircraft's left shoulder. */
    public static Offset shoulderOffset(float yawDeg, double side, double up) {
        double yaw = Math.toRadians(yawDeg);
        double leftX = Math.cos(yaw);
        double leftZ = Math.sin(yaw);
        return new Offset(leftX * Math.abs(side), up, leftZ * Math.abs(side));
    }

    /**
     * Cockpit eye position: forward along the airframe's nose axis, which means the
     * pitch has to be applied too. Minecraft's nose axis is (sin yaw * cos pitch,
     * -sin pitch, cos yaw * cos pitch) negated on x, matching
     * Vec3.directionFromRotation.
     */
    public static Offset cockpitSeatOffset(float yawDeg, float pitchDeg, double forward) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosPitch = Math.cos(pitch);
        return new Offset(-Math.sin(yaw) * forward * cosPitch,
                -Math.sin(pitch) * forward,
                Math.cos(yaw) * forward * cosPitch);
    }

    /** True when the shoulder view should be applied at all. */
    public static boolean applies(boolean enabled, boolean piloting, boolean thirdPerson,
                                  boolean cockpitView, boolean holdingAim) {
        return enabled && piloting && thirdPerson && !cockpitView && holdingAim;
    }
}