package io.github.littlebird.entity.weapon;

/**
 * Pure server-authoritative cannon timing for the jets: the fixed-wing counterpart of
 * {@link LittleBirdGunCycle}. The 25mm and the 30mm are the same state machine with different
 * numbers, which is exactly the Battlefield trade - the bigger round hits harder but fires
 * slower, runs hotter and cools slower.
 */
public final class JetCannon {
    public static final int IDLE = 0;
    public static final int WARMING = 1;
    public static final int FIRING = 2;
    public static final int OVERHEATED = 3;

    /** One gun's timing numbers. */
    public record Spec(
            int warmupTicks,
            int maxHeat,
            int overheatLockTicks,
            int fireIntervalTicks,
            int heatPerShot,
            int coolPerTick,
            int overheatCoolPerTick) {
    }

    /** F-35E 25mm: fast, light round, cool enough for long bursts. */
    public static final Spec F35_25MM = new Spec(6, 120, 60, 2, 4, 2, 3);
    /** Su-57 30mm: slower and hotter, but each round is worth more. */
    public static final Spec SU57_30MM = new Spec(8, 120, 90, 3, 8, 1, 2);

    public record Snapshot(
            int state,
            int warmupTicks,
            int heat,
            int overheatLockTicks,
            int fireCooldown) {

        public static Snapshot initial() {
            return new Snapshot(IDLE, 0, 0, 0, 0);
        }
    }

    public record Step(Snapshot snapshot, boolean fire, boolean overheatedStarted) {
    }

    private JetCannon() {
    }

    public static Step advance(boolean held, Snapshot current, Spec spec) {
        Snapshot c = current == null ? Snapshot.initial() : current;
        Spec s = spec == null ? F35_25MM : spec;

        if (c.state() == OVERHEATED) {
            int lock = Math.max(0, c.overheatLockTicks() - 1);
            int heat = Math.max(0, c.heat() - s.overheatCoolPerTick());
            if (lock > 0) {
                return new Step(new Snapshot(OVERHEATED, 0, heat, lock, 0), false, false);
            }
            return new Step(new Snapshot(held ? WARMING : IDLE, 0, heat, 0, 0), false, false);
        }

        int cooledHeat = c.state() == FIRING && held
                ? c.heat()
                : Math.max(0, c.heat() - s.coolPerTick());
        if (!held) {
            return new Step(new Snapshot(IDLE, 0, cooledHeat, 0, 0), false, false);
        }

        if (c.state() != FIRING) {
            int warmup = c.warmupTicks() + 1;
            if (warmup < s.warmupTicks()) {
                return new Step(new Snapshot(WARMING, warmup, cooledHeat, 0, 0), false, false);
            }
            int heat = cooledHeat + s.heatPerShot();
            if (heat >= s.maxHeat()) {
                return overheat(s);
            }
            return new Step(new Snapshot(FIRING, s.warmupTicks(), heat, 0,
                    s.fireIntervalTicks() - 1), true, false);
        }

        if (c.fireCooldown() > 0) {
            return new Step(new Snapshot(FIRING, s.warmupTicks(), c.heat(), 0,
                    c.fireCooldown() - 1), false, false);
        }
        int heat = c.heat() + s.heatPerShot();
        if (heat >= s.maxHeat()) {
            return overheat(s);
        }
        return new Step(new Snapshot(FIRING, s.warmupTicks(), heat, 0,
                s.fireIntervalTicks() - 1), true, false);
    }

    private static Step overheat(Spec s) {
        return new Step(new Snapshot(OVERHEATED, 0, s.maxHeat(), s.overheatLockTicks(), 0),
                false, true);
    }
}
