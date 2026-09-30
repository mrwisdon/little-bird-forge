package io.github.littlebird.entity.weapon;

/**
 * Pure lock-acquisition state for a guided launcher: a candidate has to stay in the seeker cone
 * for an uninterrupted window before a shot may be released, and a target that slips out of the
 * cone keeps the lock for a short grace period before it is lost.
 */
public final class TargetLock {
    /** One seeker's timing. IR locks quickly, radar takes longer but forgives more. */
    public record Spec(int acquireTicks, int loseGraceTicks) {
        public static final Spec IR = new Spec(30, 24);
        public static final Spec RADAR = new Spec(45, 40);
    }

    public record Snapshot(int targetId, int progress, int grace, boolean locked) {
        public static final Snapshot NONE = new Snapshot(-1, 0, 0, false);

        public boolean hasTarget() {
            return targetId >= 0;
        }
    }

    private TargetLock() {
    }

    /** Advances one tick towards, or away from, the given candidate entity id. */
    public static Snapshot advance(Snapshot current, int candidateId, boolean valid, Spec spec) {
        Snapshot c = current == null ? Snapshot.NONE : current;
        Spec s = spec == null ? Spec.IR : spec;

        if (valid && candidateId >= 0) {
            if (c.targetId() == candidateId) {
                int progress = Math.min(s.acquireTicks(), c.progress() + 1);
                return new Snapshot(candidateId, progress, s.loseGraceTicks(),
                        c.locked() || progress >= s.acquireTicks());
            }
            return new Snapshot(candidateId, 1, s.loseGraceTicks(), false);
        }

        if (c.locked()) {
            int grace = c.grace() - 1;
            return grace <= 0 ? Snapshot.NONE : new Snapshot(c.targetId(), c.progress(), grace, true);
        }
        if (c.hasTarget()) {
            // The candidate slipped away before the lock matured: progress decays, the id is kept
            // for one more tick so a wobbling crosshair does not thrash between targets.
            return new Snapshot(c.targetId(), Math.max(0, c.progress() - 1), c.grace(), false);
        }
        return c;
    }
}
