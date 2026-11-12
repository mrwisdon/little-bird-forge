package io.github.littlebird.entity.weapon;

/**
 * How an unpowered bomb falls, and when it goes off.
 *
 * <p>The model is deliberately small: gravity pulls, the air drags the horizontal velocity only (a
 * bomb does not glide), the fall speed stops at a terminal value, and a guided round may steer
 * towards one designated point with a capped authority. The cap is what "limited guidance" means -
 * a JDAM released at a spot it cannot reach still lands somewhere sensible instead of turning
 * around.
 *
 * <p>Pure rules: no Minecraft classes. The entity feeds in world coordinates each tick and applies
 * the returned state; the tests drive the same function directly.
 */
public final class BombBallistics {
    /** Downward acceleration, blocks per tick squared. */
    public static final double GRAVITY_BLOCKS_PER_TICK2 = 0.08D;
    /** Fastest a bomb falls, blocks per tick (25 blocks a second: thick air has a bottom). */
    public static final double TERMINAL_FALL_PER_TICK = 1.25D;
    /** How hard a guided round may correct its velocity, blocks per tick squared. */
    public static final double GUIDANCE_AUTHORITY = 0.02D;
    /** Furthest a designation can be and still steer: past this the round is ballistic. */
    public static final double GUIDANCE_RANGE_BLOCKS = 64.0D;
    /** How hard terminal homing may pull, blocks per tick squared: the last few blocks. */
    public static final double TERMINAL_GUIDANCE_AUTHORITY = 0.06D;
    /** A bomb below this is under the build floor: it detonates instead of falling forever. */
    public static final double FLOOR_DETONATE_Y = -70.0D;
    /** Ticks a bomb may fly before it gives up and detonates in mid air. */
    public static final int EXPIRY_TICKS = 1200;

    private BombBallistics() {
    }

    /**
     * One tick of flight.
     *
     * @param pos    {x, y, z}, in blocks; may be null for the origin
     * @param vel    {vx, vy, vz}, in blocks per tick; may be null for at rest
     * @param kind   the round: drag and guidance come from it; null means the JDAM
     * @param target {x, y, z} of the designation, or null for none
     * @param guided whether the fuse is set to steer at the designation
     * @return {posX, posY, posZ, velX, velY, velZ} - the new position followed by the new velocity.
     *         Neither input array is modified.
     */
    public static double[] step(double[] pos, double[] vel, BombKind kind, double[] target,
                                boolean guided) {
        return step(pos, vel, kind, target, guided, GUIDANCE_AUTHORITY);
    }

    /**
     * The same flight with a caller-chosen steering authority, so terminal homing can pull harder
     * than a mid-course designation while the round falls. Passing {@link #GUIDANCE_AUTHORITY}
     * reproduces the single-authority behaviour exactly.
     */
    public static double[] step(double[] pos, double[] vel, BombKind kind, double[] target,
                                boolean guided, double authority) {
        BombKind round = kind == null ? BombKind.JDAM : kind;
        double pull = Double.isFinite(authority) && authority > 0.0D ? authority : GUIDANCE_AUTHORITY;
        double px = pos == null ? 0.0D : pos[0];
        double py = pos == null ? 0.0D : pos[1];
        double pz = pos == null ? 0.0D : pos[2];
        double vx = vel == null ? 0.0D : vel[0];
        double vy = vel == null ? 0.0D : vel[1];
        double vz = vel == null ? 0.0D : vel[2];

        vy = Math.max(vy - GRAVITY_BLOCKS_PER_TICK2, -TERMINAL_FALL_PER_TICK);
        vx *= round.drag();
        vz *= round.drag();

        if (guided && round.guided() && target != null) {
            double dx = target[0] - px;
            double dy = target[1] - py;
            double dz = target[2] - pz;
            double range = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (range > 1.0E-6D && range <= GUIDANCE_RANGE_BLOCKS && speed > 1.0E-6D) {
                double cx = dx / range * speed - vx;
                double cy = dy / range * speed - vy;
                double cz = dz / range * speed - vz;
                double correction = Math.sqrt(cx * cx + cy * cy + cz * cz);
                if (correction > 1.0E-9D) {
                    double scale = Math.min(1.0D, pull / correction);
                    vx += cx * scale;
                    vy += cy * scale;
                    vz += cz * scale;
                }
            }
        }

        return new double[]{px + vx, py + vy, pz + vz, vx, vy, vz};
    }

    /** True when the round has arrived: touching the ground, or under the world floor. */
    public static boolean shouldDetonate(double y, boolean onGround) {
        return onGround || y <= FLOOR_DETONATE_Y;
    }

    /**
     * How far ahead of the aircraft a bomb should be released to land on a spot
     * {@code altitude} blocks below, for the HUD's release cue. This is the JDAM's lead.
     */
    public static double dropLeadDistance(double speedBlocksPerTick, double altitude) {
        return dropLeadDistance(speedBlocksPerTick, altitude, BombKind.JDAM);
    }

    /**
     * The same lead for a named round, which only changes how fast the forward speed bleeds off.
     * The fall time solves the terminal-velocity case in closed form, so the figure matches the
     * stepped flight the entity actually flies.
     */
    public static double dropLeadDistance(double speedBlocksPerTick, double altitude,
                                          BombKind kind) {
        if (altitude <= 0.0D || speedBlocksPerTick <= 0.0D) {
            return 0.0D;
        }
        BombKind round = kind == null ? BombKind.JDAM : kind;
        double fallTicks = fallTicksFor(altitude);
        double drag = round.drag();
        if (drag >= 1.0D) {
            return speedBlocksPerTick * fallTicks;
        }
        // Sum of speed*drag^n over the fall: the geometric series of the decaying push.
        return speedBlocksPerTick * drag * (1.0D - Math.pow(drag, fallTicks)) / (1.0D - drag);
    }

    /**
     * Ticks to fall {@code altitude} blocks from rest, in the same closed form the stepped flight
     * follows: accelerating to terminal velocity, then at it.
     */
    public static double fallTicksFor(double altitude) {
        if (!(altitude > 0.0D)) {
            return 0.0D;
        }
        double terminalHeight = TERMINAL_FALL_PER_TICK * TERMINAL_FALL_PER_TICK
                / (2.0D * GRAVITY_BLOCKS_PER_TICK2);
        return altitude <= terminalHeight
                ? Math.sqrt(2.0D * altitude / GRAVITY_BLOCKS_PER_TICK2)
                : TERMINAL_FALL_PER_TICK / GRAVITY_BLOCKS_PER_TICK2
                        + (altitude - terminalHeight) / TERMINAL_FALL_PER_TICK;
    }

    /**
     * Where a terminal-homing round should aim: the target's position led by the time the round
     * still needs to fall onto it, so the bomb flies at where the vehicle will be rather than at
     * where it was when the seeker last saw it.
     *
     * <p>{@code bombPos}/{@code bombVel} are the round's {x, y, z} and {vx, vy, vz}; the target's
     * velocity is led on all three axes (a ground vehicle's is almost purely horizontal). Null
     * arrays and null entries read as zero, so a caller without a velocity still gets a point.
     */
    public static double[] leadAimPoint(double[] bombPos, double[] bombVel, double[] targetPos,
                                        double[] targetVel) {
        double px = bombPos == null ? 0.0D : bombPos[0];
        double py = bombPos == null ? 0.0D : bombPos[1];
        double pz = bombPos == null ? 0.0D : bombPos[2];
        double tx = targetPos == null ? px : targetPos[0];
        double ty = targetPos == null ? py : targetPos[1];
        double tz = targetPos == null ? pz : targetPos[2];
        double vx = targetVel == null ? 0.0D : targetVel[0];
        double vy = targetVel == null ? 0.0D : targetVel[1];
        double vz = targetVel == null ? 0.0D : targetVel[2];
        double fall = fallTicksFor(py - ty);
        return new double[]{tx + vx * fall, ty + vy * fall, tz + vz * fall};
    }
}
