package io.github.littlebird.entity.weapon;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;

/**
 * Geometry of the TOW's exhaust: two parallel smoke tails streaming back from the nozzle, the
 * double ponytail that makes a wire guided missile recognisable from across the battlefield.
 *
 * Pure rules - no Minecraft classes - so the shape and the range it is broadcast over can be
 * unit tested. The trails are emitted along the flight segment of the tick, not just at the
 * missile's centre, or a missile doing three blocks a tick would leave a dotted line.
 */
public final class ApacheMissileTrail {
    /** Half the gap between the two tails, in blocks: tight enough to read as one flame. */
    public static final double TAIL_SEPARATION = 0.14D;
    /** How far behind the missile's centre the nozzle sits, in blocks. */
    public static final double NOZZLE_BACK_OFFSET = 0.45D;
    /** Two tails, one either side of the axis. */
    public static final int TAILS = 2;
    /**
     * The trail is emitted once every this many ticks, and covers that whole stretch of flight.
     * Emitting along a single tick while only doing so every other tick left a gap in the smoke
     * that a three block per tick missile made very obvious.
     */
    public static final int TRAIL_INTERVAL_TICKS = 2;
    /** How far a bulk ServerLevel#sendParticles packet is delivered: the limit being beaten. */
    public static final double VANILLA_PARTICLE_RANGE = 32.0D;
    /** The same twin trail is broadcast to players this far away at least. */
    public static final double MIN_PARTICLE_RANGE = 64.0D;
    public static final double MAX_PARTICLE_RANGE = 256.0D;
    public static final double DEFAULT_PARTICLE_RANGE = 96.0D;

    private ApacheMissileTrail() {
    }

    /** Unit vector to the missile's right for a given heading. */
    public static Vector3 rightOf(Vector3 forward) {
        Vector3 right = forward.normalize().cross(new Vector3(0.0D, 1.0D, 0.0D));
        if (right.length() < 1.0E-6D) {
            // Straight up or straight down: any horizontal axis reads the same.
            return new Vector3(1.0D, 0.0D, 0.0D);
        }
        return right.normalize();
    }

    /** The two tail emission points, left first. */
    public static Vector3[] tailAnchors(Vector3 centre, Vector3 right) {
        Vector3 offset = right.normalize().scale(TAIL_SEPARATION);
        return new Vector3[] {centre.subtract(offset), centre.add(offset)};
    }

    /** The exhaust nozzle: behind the missile's centre, on its axis. */
    public static Vector3 nozzle(Vector3 centre, Vector3 forward) {
        return centre.subtract(forward.normalize().scale(NOZZLE_BACK_OFFSET));
    }

    /** Points sampled along this tick's flight, oldest first. */
    public static Vector3[] samples(Vector3 from, Vector3 to, int substeps) {
        int steps = Math.max(1, substeps);
        Vector3[] points = new Vector3[steps];
        for (int i = 0; i < steps; i++) {
            points[i] = from.add(to.subtract(from).scale(i / (double) steps));
        }
        return points;
    }

    /**
     * How far the trail is broadcast, in blocks. A vanilla particle broadcast stops at 32
     * blocks, which is why the smoke used to vanish while the missile was still being tracked;
     * the configured value can never come out below the minimum.
     */
    public static double particleRange(double configured) {
        if (Double.isNaN(configured)) {
            return DEFAULT_PARTICLE_RANGE;
        }
        return Math.max(MIN_PARTICLE_RANGE, Math.min(MAX_PARTICLE_RANGE, configured));
    }
}
