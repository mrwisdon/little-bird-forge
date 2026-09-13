package io.github.littlebird.client;

/**
 * Projects a world point onto the screen for the aim reticles, kept free of
 * Minecraft classes so the camera conventions are pinned by unit tests rather
 * than guessed from matrix state.
 *
 * Minecraft forwards a yaw as (-sin yaw, 0, cos yaw) and a pitch as positive
 * looking down, so the camera basis below is built from the same convention.
 */
public final class AimProjection {
    private AimProjection() {
    }

    public record Vector(double x, double y, double z) {
        public Vector subtract(Vector other) {
            return new Vector(x - other.x, y - other.y, z - other.z);
        }

        public double dot(Vector other) {
            return x * other.x + y * other.y + z * other.z;
        }
    }

    /** x and y are GUI pixels; visible is false when the point is behind the camera. */
    public record Screen(double x, double y, boolean visible) {
    }

    public static Screen project(
            Vector point, Vector eye,
            float yawDeg, float pitchDeg, float rollDeg,
            double verticalFovDeg, int width, int height) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cosPitch = Math.cos(pitch);
        Vector forward = new Vector(
                -Math.sin(yaw) * cosPitch, -Math.sin(pitch), Math.cos(yaw) * cosPitch);
        Vector right = new Vector(-Math.cos(yaw), 0.0D, -Math.sin(yaw));
        Vector up = new Vector(
                right.y * forward.z - right.z * forward.y,
                right.z * forward.x - right.x * forward.z,
                right.x * forward.y - right.y * forward.x);

        Vector relative = point.subtract(eye);
        double depth = relative.dot(forward);
        if (depth <= 0.05D) {
            return new Screen(0.0D, 0.0D, false);
        }
        double x = relative.dot(right);
        double y = relative.dot(up);

        double roll = Math.toRadians(rollDeg);
        double cosRoll = Math.cos(roll);
        double sinRoll = Math.sin(roll);
        double rolledX = x * cosRoll - y * sinRoll;
        double rolledY = x * sinRoll + y * cosRoll;

        double halfFov = Math.tan(Math.toRadians(verticalFovDeg) * 0.5D);
        double aspect = height <= 0 ? 1.0D : width / (double) height;
        double ndcX = (rolledX / depth) / (halfFov * aspect);
        double ndcY = (rolledY / depth) / halfFov;
        return new Screen(
                (ndcX * 0.5D + 0.5D) * width,
                (1.0D - (ndcY * 0.5D + 0.5D)) * height,
                true);
    }
}