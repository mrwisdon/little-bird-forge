package io.github.littlebird.entity.weapon;

/**
 * Pure twin-gun muzzle, convergence and spread math, kept free of Minecraft
 * classes so the server-side raycast can be unit-tested.
 *
 * Both barrels sit on the nose, converge at CONVERGENCE_DISTANCE and share one
 * spread cone whose radius grows with gun heat.
 */
public final class LittleBirdBallistics {
    public static final double MUZZLE_LATERAL_OFFSET =
            io.github.littlebird.entity.LittleBirdGeometry.gunLateralBlocks();
    /** The entity already lifts the origin to gun height, so no extra drop. */
    public static final double MUZZLE_DROP = 0.0D;
    public static final double MUZZLE_FORWARD =
            io.github.littlebird.entity.LittleBirdGeometry.gunForwardBlocks();
    public static final double CONVERGENCE_DISTANCE = 32.0D;
    public static final double RANGE = 128.0D;
    public static final double MIN_SPREAD_DEGREES = 0.35D;
    public static final double MAX_SPREAD_DEGREES = 1.10D;

    private LittleBirdBallistics() {
    }

    public static Vector3 muzzleCenter(Vector3 origin, Vector3 aim) {
        Vector3 forward = aim.normalize();
        return origin.add(new Vector3(0.0D, -MUZZLE_DROP, 0.0D))
                .add(forward.scale(MUZZLE_FORWARD));
    }

    /** Barrel 0 is the left gun, barrel 1 is the right gun. */
    public static Vector3 muzzle(Vector3 center, Vector3 right, int barrel) {
        return center.add(right.normalize().scale(MUZZLE_LATERAL_OFFSET * barrelSign(barrel)));
    }

    public static double barrelSign(int barrel) {
        return barrel == 0 ? -1.0D : 1.0D;
    }

    public static Vector3 convergencePoint(Vector3 center, Vector3 aim) {
        return center.add(aim.normalize().scale(CONVERGENCE_DISTANCE));
    }

    public static Vector3 convergingDirection(Vector3 muzzle, Vector3 convergencePoint) {
        return convergencePoint.subtract(muzzle).normalize();
    }

    /** Spread radius in degrees grows linearly from cold to fully heated. */
    public static double spreadDegrees(int heat) {
        double fraction = clamp(heat / (double) LittleBirdGunCycle.MAX_HEAT, 0.0D, 1.0D);
        return MIN_SPREAD_DEGREES + (MAX_SPREAD_DEGREES - MIN_SPREAD_DEGREES) * fraction;
    }

    /**
     * Applies an angular offset sampled uniformly by area from a circular disk.
     * Both samples must be in [0, 1].
     */
    public static Vector3 spreadDirection(
            Vector3 baseDirection, double maxDegrees, double radialSample, double angleSample) {
        Vector3 forward = baseDirection.normalize();
        Vector3 reference = Math.abs(forward.y()) < 0.999D
                ? new Vector3(0.0D, 1.0D, 0.0D)
                : new Vector3(1.0D, 0.0D, 0.0D);
        Vector3 right = forward.cross(reference).normalize();
        Vector3 up = right.cross(forward).normalize();
        double radiusDegrees = maxDegrees * Math.sqrt(clamp(radialSample, 0.0D, 1.0D));
        double angle = Math.PI * 2.0D * clamp(angleSample, 0.0D, 1.0D);
        double tangent = Math.tan(Math.toRadians(radiusDegrees));
        return forward
                .add(right.scale(tangent * Math.cos(angle)))
                .add(up.scale(tangent * Math.sin(angle)))
                .normalize();
    }

    /** Which airframe a hit landed on, so each aircraft can be priced on its own hull. */
    public enum Airframe {
        NONE,
        LITTLE_BIRD,
        APACHE
    }

    /**
     * Damage a landed hit deals, priced per target class. Every airframe gets its own,
     * deliberately small, price so a duel is a long burst rather than two rounds, while a
     * non-airframe keeps the anti-personnel price. Negative and non-finite prices are 0.
     */
    public static float damageTo(Airframe airframe, float normalDamage,
                                 float littleBirdDamage, float apacheDamage) {
        float chosen = switch (airframe) {
            case LITTLE_BIRD -> littleBirdDamage;
            case APACHE -> apacheDamage;
            case NONE -> normalDamage;
        };
        return Float.isFinite(chosen) ? Math.max(0.0F, chosen) : 0.0F;
    }

    /**
     * Damage a landed hit deals. Airframes get their own, deliberately small, price so a
     * Little Bird duel is a long burst or a handful of rockets rather than two rounds.
     */
    public static float damageTo(boolean airframe, float normalDamage, float airframeDamage) {
        return damageTo(airframe ? Airframe.LITTLE_BIRD : Airframe.NONE,
                normalDamage, airframeDamage, airframeDamage);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Vector3(double x, double y, double z) {
        public Vector3 add(Vector3 other) {
            return new Vector3(x + other.x, y + other.y, z + other.z);
        }

        public Vector3 subtract(Vector3 other) {
            return new Vector3(x - other.x, y - other.y, z - other.z);
        }

        public Vector3 scale(double scalar) {
            return new Vector3(x * scalar, y * scalar, z * scalar);
        }

        public double dot(Vector3 other) {
            return x * other.x + y * other.y + z * other.z;
        }

        public Vector3 cross(Vector3 other) {
            return new Vector3(
                    y * other.z - z * other.y,
                    z * other.x - x * other.z,
                    x * other.y - y * other.x);
        }

        public double length() {
            return Math.sqrt(dot(this));
        }

        public Vector3 normalize() {
            double length = length();
            if (length < 1.0E-12D) {
                throw new IllegalArgumentException("Cannot normalize a zero-length vector");
            }
            return scale(1.0D / length);
        }

        public double distanceTo(Vector3 other) {
            return subtract(other).length();
        }
    }
}
