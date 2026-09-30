package io.github.littlebird.entity.weapon;

/**
 * Pure flare dispenser state: a finite number of decoys, a cooldown between deployments, and a
 * short window after each deployment in which an incoming seeker can be seduced away.
 */
public final class Countermeasures {
    public record Spec(int flares, int cooldownTicks, int activeWindowTicks, double seduceChance) {
    }

    /** Eight bundles, a second between them, a 1.5s window, 60% spoof. */
    public static final Spec DEFAULT = new Spec(8, 20, 30, 0.6D);

    public record Snapshot(int flares, int cooldown, int activeWindow) {
        public static Snapshot full(Spec spec) {
            Spec s = spec == null ? DEFAULT : spec;
            return new Snapshot(Math.max(0, s.flares()), 0, 0);
        }
    }

    private Countermeasures() {
    }

    /** Ticks the cooldown and the seduction window down by one. */
    public static Snapshot advance(Snapshot current) {
        Snapshot c = current == null ? new Snapshot(0, 0, 0) : current;
        return new Snapshot(c.flares(), Math.max(0, c.cooldown() - 1),
                Math.max(0, c.activeWindow() - 1));
    }

    /** Deploys one decoy, or returns the state unchanged when empty or still cooling. */
    public static Snapshot deploy(Snapshot current, Spec spec) {
        Snapshot c = advance(current);
        Spec s = spec == null ? DEFAULT : spec;
        if (c.flares() <= 0 || c.cooldown() > 0) {
            return c;
        }
        return new Snapshot(c.flares() - 1, s.cooldownTicks(), s.activeWindowTicks());
    }

    /** True when a deploy window is open and this roll falls inside the seduction chance. */
    public static boolean seduces(Snapshot current, Spec spec, double roll) {
        Snapshot c = current == null ? new Snapshot(0, 0, 0) : current;
        Spec s = spec == null ? DEFAULT : spec;
        return c.activeWindow() > 0 && roll >= 0.0D && roll < s.seduceChance();
    }

    /** Tops the dispenser up from a crate, never past its capacity. */
    public static Snapshot refill(Snapshot current, Spec spec, int units) {
        Snapshot c = current == null ? new Snapshot(0, 0, 0) : current;
        Spec s = spec == null ? DEFAULT : spec;
        return new Snapshot(Math.min(Math.max(0, s.flares()), c.flares() + Math.max(0, units)),
                c.cooldown(), c.activeWindow());
    }
}
