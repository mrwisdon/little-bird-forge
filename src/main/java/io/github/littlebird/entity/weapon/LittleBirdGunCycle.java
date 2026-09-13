package io.github.littlebird.entity.weapon;

/**
 * Server-authoritative twin rapid-fire gun timing. Pure state machine, free of
 * Minecraft classes so it can be unit-tested (same approach as the reference
 * project's drone gun cycle).
 *
 * Fires one round every FIRE_INTERVAL_TICKS ticks, alternating left and right
 * barrels, for an effective 20 rounds per second.
 */
public final class LittleBirdGunCycle {
    public static final int IDLE = 0;
    public static final int WARMING = 1;
    public static final int FIRING = 2;
    public static final int OVERHEATED = 3;

    public static final int WARMUP_TICKS = 8;
    public static final int MAX_HEAT = 120;
    public static final int OVERHEAT_LOCK_TICKS = 80;
    /** One round per tick: 20 rounds per second, alternating between the guns. */
    public static final int FIRE_INTERVAL_TICKS = 1;
    /** Halved from 2 when the rate doubled, so six seconds of fire still overheats. */
    public static final int HEAT_PER_SHOT = 1;
    public static final int COOL_PER_TICK = 1;
    public static final int OVERHEAT_COOL_PER_TICK = 2;
    public static final int BARRELS = 2;

    private LittleBirdGunCycle() {
    }

    public record Snapshot(
            int state,
            int warmupTicks,
            int heat,
            int overheatLockTicks,
            int fireCooldown,
            int nextBarrel) {

        public static Snapshot initial() {
            return new Snapshot(IDLE, 0, 0, 0, 0, 0);
        }
    }

    public record Step(Snapshot snapshot, boolean fire, int barrel, boolean overheatedStarted) {
    }

    public static Step advance(boolean held, Snapshot current) {
        Snapshot c = current == null ? Snapshot.initial() : current;

        if (c.state() == OVERHEATED) {
            int lock = Math.max(0, c.overheatLockTicks() - 1);
            int heat = Math.max(0, c.heat() - OVERHEAT_COOL_PER_TICK);
            if (lock > 0) {
                return new Step(new Snapshot(OVERHEATED, 0, heat, lock, 0, c.nextBarrel()),
                        false, 0, false);
            }
            return new Step(new Snapshot(held ? WARMING : IDLE, 0, heat, 0, 0, c.nextBarrel()),
                    false, 0, false);
        }

        int cooledHeat = c.state() == FIRING && held
                ? c.heat()
                : Math.max(0, c.heat() - COOL_PER_TICK);
        if (!held) {
            return new Step(new Snapshot(IDLE, 0, cooledHeat, 0, 0, c.nextBarrel()),
                    false, 0, false);
        }

        if (c.state() != FIRING) {
            int warmup = c.warmupTicks() + 1;
            if (warmup < WARMUP_TICKS) {
                return new Step(new Snapshot(WARMING, warmup, cooledHeat, 0, 0, c.nextBarrel()),
                        false, 0, false);
            }
            int heat = cooledHeat + HEAT_PER_SHOT;
            if (heat >= MAX_HEAT) {
                return overheat(c.nextBarrel());
            }
            return new Step(new Snapshot(FIRING, WARMUP_TICKS, heat, 0,
                    FIRE_INTERVAL_TICKS - 1, other(c.nextBarrel())),
                    true, c.nextBarrel(), false);
        }

        if (c.fireCooldown() > 0) {
            return new Step(new Snapshot(FIRING, WARMUP_TICKS, c.heat(), 0,
                    c.fireCooldown() - 1, c.nextBarrel()), false, 0, false);
        }
        int heat = c.heat() + HEAT_PER_SHOT;
        if (heat >= MAX_HEAT) {
            return overheat(c.nextBarrel());
        }
        return new Step(new Snapshot(FIRING, WARMUP_TICKS, heat, 0,
                FIRE_INTERVAL_TICKS - 1, other(c.nextBarrel())),
                true, c.nextBarrel(), false);
    }

    private static Step overheat(int nextBarrel) {
        return new Step(new Snapshot(OVERHEATED, 0, MAX_HEAT, OVERHEAT_LOCK_TICKS, 0, nextBarrel),
                false, 0, true);
    }

    public static int other(int barrel) {
        return (barrel + 1) % BARRELS;
    }
}
