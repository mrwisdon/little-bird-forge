package io.github.littlebird.entity.weapon;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;

/**
 * Wire guided missile flight: the operator keeps the sight on the target and the missile is
 * flown onto the spot the sight line ends at (SACLOS). Each tick the missile turns towards the
 * current aim point by at most the missile's turn rate, so moving the sight drags the missile
 * with it, and it detonates when it arrives rather than when it happens to hit something.
 *
 * Minecraft has no actual wire, so this is the behavioural equivalent, not a simulation of
 * the command link. Pure rules: no Minecraft classes.
 */
public final class ApacheMissileBallistics {
    /**
     * The missile was halved: 0.60 blocks a tick off the rail and 1.40 at cruise, against 1.20
     * and 3.00 before. A TOW that crosses the map in a couple of seconds reads as a hitscan
     * shot rather than a missile, and the smoke trail had nothing to show.
     */
    public static final double INITIAL_SPEED = 0.60D;
    public static final double CRUISE_SPEED = 1.40D;
    public static final int SPIN_UP_TICKS = 10;
    /** 140 degrees per second: a missile can be steered, but not like a rocket. */
    public static final double MAX_TURN_DEGREES_PER_TICK = 7.0D;
    /** Small drop the guidance has to fight, which is what a guided missile looks like. */
    public static final double GRAVITY = 0.004D;
    /**
     * Long enough to fly the whole sight range at cruise speed and still have margin, so the
     * lifetime is the backstop it should be rather than the thing that sets the range.
     */
    public static final int LIFETIME_TICKS = 170;
    /**
     * How far along the sight line the operator's aim point is looked for. This is the TOW's
     * effective reach: aiming at open sky puts the aim point this far out and the missile
     * detonates there rather than sailing off to the end of its lifetime.
     */
    public static final double SIGHT_RANGE_BLOCKS = 64.0D;
    /** The aim point counts as reached inside this radius, so the missile detonates on it. */
    public static final double REACH_RADIUS_BLOCKS = 1.5D;
    /** A hostile this close to the missile's nose is hit, whatever the aim point is doing. */
    public static final double PROXIMITY_FUSE_BLOCKS = 1.0D;

    private ApacheMissileBallistics() {
    }

    /** Launch velocity: straight down the sight line, slow at first. */
    public static Vector3 launchVelocity(Vector3 sightLine) {
        return sightLine.normalize().scale(INITIAL_SPEED);
    }

    /**
     * The point on the sight line the missile is being flown at: the first block the line hits,
     * or the end of the sight range over open ground or sky. The distance is returned with it so
     * every later tick can be re-raycast at the same depth - that is what makes the aim point
     * slide with the operator's sight instead of sitting wherever the missile happens to be.
     */
    public static AimPoint aimPoint(Vector3 eye, Vector3 sightLine, double sightRangeBlocks,
                                    double blockDistanceBlocks) {
        Vector3 direction = unitOrDefault(sightLine, new Vector3(0.0D, 0.0D, 1.0D));
        double range = finitePositive(sightRangeBlocks, SIGHT_RANGE_BLOCKS);
        double distance = Double.isFinite(blockDistanceBlocks) && blockDistanceBlocks > 0.0D
                ? Math.min(blockDistanceBlocks, range) : range;
        return new AimPoint(eye.add(direction.scale(distance)), distance);
    }

    /**
     * True once the missile has arrived at the aim point. This is the TOW's fusing: the operator
     * is not shooting along a line and hoping, they are flying the missile onto a picked spot.
     */
    public static boolean reachedAimPoint(Vector3 position, Vector3 aimPoint,
                                          double reachRadiusBlocks) {
        double radius = finitePositive(reachRadiusBlocks, REACH_RADIUS_BLOCKS);
        return position.distanceTo(aimPoint) <= radius;
    }

    /** Distance at which a hostile trips the proximity fuse. */
    public static double proximityFuseRadius() {
        return PROXIMITY_FUSE_BLOCKS;
    }

    /** A direct hit on a rotorcraft takes the rotor off rather than chipping its health bar. */
    public static float directHitDamage(float remainingHealth) {
        return Math.max(1.0F, remainingHealth + 1.0F);
    }

    /** Where the operator's sight line ends, and how far along it that is. */
    public record AimPoint(Vector3 point, double distanceBlocks) {
    }

    private static Vector3 unitOrDefault(Vector3 value, Vector3 fallback) {
        if (value == null || value.length() < 1.0E-9D) {
            return fallback;
        }
        return value.normalize();
    }

    private static double finitePositive(double value, double fallback) {
        return Double.isFinite(value) && value > 0.0D ? value : fallback;
    }

    /** Speed the missile should hold at this age. */
    public static double speedAt(int ageTicks) {
        double span = Math.max(1, SPIN_UP_TICKS);
        double fraction = Math.min(1.0D, Math.max(0, ageTicks) / span);
        return INITIAL_SPEED + (CRUISE_SPEED - INITIAL_SPEED) * fraction;
    }

    /**
     * One tick of guided flight: the velocity is turned towards the sight line, the speed
     * follows the spin-up ramp, and a small drop is applied for the guidance to correct.
     */
    public static Vector3 guide(Vector3 velocity, Vector3 sightLine, int ageTicks) {
        double length = velocity.length();
        if (length < 1.0E-9D) {
            return launchVelocity(sightLine);
        }
        double sightLength = sightLine.length();
        if (sightLength < 1.0E-9D) {
            return velocity;
        }

        Vector3 heading = velocity.scale(1.0D / length);
        Vector3 perturbed = heading.add(new Vector3(0.0D, -GRAVITY, 0.0D));
        if (perturbed.length() < 1.0E-9D) {
            perturbed = heading;
        }
        Vector3 steered = turnTowards(perturbed.normalize(), sightLine.scale(1.0D / sightLength),
                MAX_TURN_DEGREES_PER_TICK);
        return steered.scale(speedAt(ageTicks));
    }

    /** Rotates a unit heading towards another unit direction, by at most maxDegrees. */
    public static Vector3 turnTowards(Vector3 heading, Vector3 target, double maxDegrees) {
        double dot = clamp(heading.dot(target), -1.0D, 1.0D);
        double degrees = Math.toDegrees(Math.acos(dot));
        if (degrees <= 1.0E-4D) {
            return target;
        }
        if (degrees <= maxDegrees) {
            return target;
        }
        double fraction = maxDegrees / degrees;
        return heading.scale(1.0D - fraction).add(target.scale(fraction)).normalize();
    }

    public static boolean expired(int ageTicks) {
        return ageTicks >= LIFETIME_TICKS;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
