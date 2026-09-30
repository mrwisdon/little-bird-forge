package io.github.littlebird.entity.weapon;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;

/**
 * Fire control for the sighted guns: where the round sitting in the barrel right now would land.
 *
 * <p>The flight model is the shell entity's own - gravity first, then the move, no drag - run
 * forward from the muzzle against the terrain instead of against the world. That is what makes the
 * marker honest: change the muzzle speed or the gravity in the config and the sight follows,
 * because the prediction is not a second physics engine.
 *
 * <p>Pure rules: no Minecraft classes, so the solver is pinned by unit tests against flat ground,
 * ridges, the textbook drop and the sight's own mil convention.
 */
public final class MainGunFireControl {
    /** Terrain height under a world column, in blocks. */
    @FunctionalInterface
    public interface Ground {
        double heightAt(double x, double z);
    }

    /**
     * Where the round meets the ground, or where it ran out of life.
     *
     * @param flightTicks  ticks from the muzzle to this point
     * @param rangeBlocks  horizontal distance from the muzzle
     * @param dropBlocks   how far below the muzzle the round is
     * @param reached      true when the terrain stopped it, false when the tick budget ran out
     */
    public record Impact(double x, double y, double z, int flightTicks, double rangeBlocks,
                         double dropBlocks, boolean reached) {
    }

    /** A mil is a thousandth of a radian: 17.45 to the degree, which is the sight's scale. */
    public static final double MILS_PER_DEGREE = 1000.0D * Math.PI / 180.0D;

    private MainGunFireControl() {
    }

    /**
     * Flies one round along a direction until the ground rises to meet it.
     *
     * <p>The step is exactly {@link MainGunShellBallistics}: gravity is pulled first and the round
     * then moves by the new velocity, so after n ticks at a level attitude it has fallen
     * {@code gravity / 2 * n * (n + 1)} blocks. Mirroring the order matters - predicting with the
     * continuous {@code g*t*t/2} instead reads about half a block long at 400 blocks, which is
     * exactly the error a fire control must not have.
     */
    public static Impact predict(double muzzleX, double muzzleY, double muzzleZ,
            double directionX, double directionY, double directionZ,
            double speedBlocksPerTick, double gravityPerTick, int maxTicks, Ground ground) {
        double length = Math.sqrt(directionX * directionX + directionY * directionY
                + directionZ * directionZ);
        if (ground == null || !(length > 1.0E-9D) || !(speedBlocksPerTick > 0.0D)
                || !(gravityPerTick > 0.0D) || maxTicks <= 0
                || !Double.isFinite(muzzleX) || !Double.isFinite(muzzleY)
                || !Double.isFinite(muzzleZ)) {
            return new Impact(muzzleX, muzzleY, muzzleZ, 0, 0.0D, 0.0D, false);
        }
        double scale = speedBlocksPerTick / length;
        double vx = directionX * scale;
        double vy = directionY * scale;
        double vz = directionZ * scale;
        double x = muzzleX;
        double y = muzzleY;
        double z = muzzleZ;
        for (int tick = 1; tick <= maxTicks; tick++) {
            vy -= gravityPerTick;
            x += vx;
            y += vy;
            z += vz;
            double groundY = ground.heightAt(x, z);
            if (Double.isFinite(groundY) && y <= groundY) {
                return new Impact(x, Math.max(y, groundY), z, tick,
                        Math.hypot(x - muzzleX, z - muzzleZ), muzzleY - y, true);
            }
        }
        return new Impact(x, y, z, maxTicks, Math.hypot(x - muzzleX, z - muzzleZ),
                muzzleY - y, false);
    }

    /**
     * Ground range a round covers when fired at this elevation from level ground, inside the tick
     * budget. Level fire lands at the muzzle (range zero), the range grows to its peak near 45
     * degrees and falls away past it, which is what the crew's own sense of the gun expects.
     */
    public static double rangeAtElevation(double elevationDeg, double speedBlocksPerTick,
                                          double gravityPerTick, int maxTicks) {
        if (!Double.isFinite(elevationDeg) || !(speedBlocksPerTick > 0.0D)
                || !(gravityPerTick > 0.0D) || maxTicks <= 0) {
            return 0.0D;
        }
        double radians = Math.toRadians(Math.max(0.0D, Math.min(89.9D, elevationDeg)));
        double vx = Math.cos(radians) * speedBlocksPerTick;
        double vy = Math.sin(radians) * speedBlocksPerTick;
        double x = 0.0D;
        double y = 0.0D;
        for (int tick = 1; tick <= maxTicks; tick++) {
            double previous = y;
            vy -= gravityPerTick;
            x += vx;
            y += vy;
            if (y <= 0.0D) {
                double fraction = previous <= 0.0D ? 0.0D : previous / (previous - y);
                return x - vx * (1.0D - fraction);
            }
        }
        return x;
    }

    /**
     * Angular offset of a world point from a sight line, in mils: {right, down}.
     *
     * <p>Minecraft's yaw grows clockwise from +Z and its pitch grows downwards, so this reports what
     * the gunner sees: positive right, positive down. Divide by 1000 and multiply by the pixels per
     * mil the sight is drawn at to place a marker.
     */
    public static double[] angularOffsetMils(double originX, double originY, double originZ,
            double yawDeg, double pitchDeg, double targetX, double targetY, double targetZ) {
        double dx = targetX - originX;
        double dy = targetY - originY;
        double dz = targetZ - originZ;
        if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)) {
            return new double[]{0.0D, 0.0D};
        }
        double horizontal = Math.hypot(dx, dz);
        if (horizontal < 1.0E-9D && Math.abs(dy) < 1.0E-9D) {
            return new double[]{0.0D, 0.0D};
        }
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double targetPitch = Math.toDegrees(Math.atan2(-dy, horizontal));
        return new double[]{wrapDegrees(targetYaw - yawDeg) * MILS_PER_DEGREE,
                wrapDegrees(targetPitch - pitchDeg) * MILS_PER_DEGREE};
    }

    /** Heading into +/-180 so a long shot cannot fold the bearing. */
    public static double wrapDegrees(double degrees) {
        if (!Double.isFinite(degrees)) {
            return 0.0D;
        }
        double wrapped = (degrees + 180.0D) % 360.0D;
        if (wrapped < 0.0D) {
            wrapped += 360.0D;
        }
        return wrapped - 180.0D;
    }
}
