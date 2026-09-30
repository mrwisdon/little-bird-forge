package io.github.littlebird.entity.weapon;

/**
 * Pure 70mm rocket pod state: two pods of seven tubes, alternating between pods,
 * single-shot on press and a fixed salvo cadence while held.
 *
 * Free of Minecraft classes so the firing schedule can be unit-tested.
 */
public final class RocketPodState {
    public static final int PODS = 2;
    public static final int TUBES_PER_POD = 7;
    /** Every tube on the airframe, which is what the model draws. */
    public static final int TOTAL_ROCKETS = PODS * TUBES_PER_POD;
    /** Rounds that can fire before reloading: seven in TOTAL, split between the two pods. */
    public static final int READY_CAPACITY = 7;
    /** Rounds the ammunition crate stocks into the reserve. */
    public static final int RESERVE_CAPACITY = 42;

    /** The two pods between them hold the ready rounds, left pod first. */
    public record Ready(int left, int right) {
    }

    /** Splits a ready count over the two pods: seven becomes four and three. */
    public static Ready splitReady(int total) {
        int ready = Math.max(0, Math.min(READY_CAPACITY, total));
        return new Ready((ready + 1) / 2, ready / 2);
    }
    /** Salvo ripple: about seven rockets per second. */
    public static final int SALVO_INTERVAL_TICKS = 3;
    /** Single shots: ten rockets per second at the trigger's own cadence. */
    public static final int SINGLE_INTERVAL_TICKS = 2;

    private RocketPodState() {
    }

    public record Snapshot(
            int leftRemaining,
            int rightRemaining,
            int nextPod,
            int fireCooldown,
            boolean wasHeld) {

        public static Snapshot initial() {
            Ready ready = splitReady(READY_CAPACITY);
            return new Snapshot(ready.left(), ready.right(), 0, 0, false);
        }

        public int totalRemaining() {
            return leftRemaining + rightRemaining;
        }

        public int remaining(int pod) {
            return pod == 0 ? leftRemaining : rightRemaining;
        }

        /** Next tube index that will fire from the given pod, or -1 when empty. */
        public int nextTube(int pod) {
            int remaining = remaining(pod);
            return remaining <= 0 ? -1 : TUBES_PER_POD - remaining;
        }
    }

    /**
     * Reserve-to-pod transfer clock. Pressing reload starts it and the rounds land when it reaches
     * zero, so a crew cannot top the pods up instantly mid-fight.
     */
    public record Reload(int ticksLeft) {
        public static Reload idle() {
            return new Reload(0);
        }

        public boolean running() {
            return ticksLeft > 0;
        }

        /** 0..1 progress for the HUD bar; an idle clock reads as finished. */
        public float fraction(int totalTicks) {
            if (totalTicks <= 0) {
                return 0.0F;
            }
            int left = Math.max(0, Math.min(totalTicks, ticksLeft));
            return 1.0F - left / (float) totalTicks;
        }
    }

    /**
     * Starts the clock, unless one is already running or there is nothing to move. A zero tick
     * clock means the config asked for an instant reload, and is returned as an idle clock.
     */
    public static Reload begin(Reload current, int ticks, int missing, int reserve) {
        if (current != null && current.running()) {
            return current;
        }
        if (missing <= 0 || reserve <= 0) {
            return Reload.idle();
        }
        return new Reload(Math.max(0, ticks));
    }

    /** One tick of the clock. Never goes below zero. */
    public static Reload tick(Reload current) {
        int left = current == null ? 0 : Math.max(0, current.ticksLeft());
        return new Reload(left > 0 ? left - 1 : 0);
    }

    /** True on the tick the clock lands, which is when the pods are filled. */
    public static boolean completes(Reload before, Reload after) {
        return before != null && before.running() && after != null && !after.running();
    }

    /** Rounds the reserve gives up when the clock lands: only what the pods are missing. */
    public static int transferAmount(int readyRemaining, int reserve) {
        int missing = READY_CAPACITY - Math.max(0, readyRemaining);
        return Math.max(0, Math.min(missing, Math.max(0, reserve)));
    }

    public record Step(Snapshot snapshot, boolean launch, int pod, int tube) {
    }

    /**
     * Advances one tick. In salvo mode a held trigger fires on the fixed cadence;
     * in single mode only the rising edge fires.
     */
    public static Step advance(Snapshot current, boolean held, boolean salvoMode) {
        Snapshot c = current == null ? Snapshot.initial() : current;
        int cooldown = Math.max(0, c.fireCooldown() - 1);
        boolean wantsFire = salvoMode ? held : (held && !c.wasHeld());

        if (!wantsFire || cooldown > 0 || c.totalRemaining() <= 0) {
            return new Step(new Snapshot(
                    c.leftRemaining(), c.rightRemaining(), c.nextPod(), cooldown, held),
                    false, 0, -1);
        }

        int pod = pickPod(c);
        int tube = c.nextTube(pod);
        int left = c.leftRemaining();
        int right = c.rightRemaining();
        if (pod == 0) {
            left--;
        } else {
            right--;
        }
        int interval = salvoMode ? SALVO_INTERVAL_TICKS : SINGLE_INTERVAL_TICKS;
        return new Step(new Snapshot(left, right, (pod + 1) % PODS, interval, held),
                true, pod, tube);
    }

    private static int pickPod(Snapshot c) {
        int preferred = c.nextPod() % PODS;
        if (c.remaining(preferred) > 0) {
            return preferred;
        }
        return (preferred + 1) % PODS;
    }

    /**
     * Tops the pods up by at most rounds rockets, alternating between them and
     * starting with the emptier pod so a partial rearm stays balanced. Never
     * exceeds TUBES_PER_POD per pod.
     */
    public static Snapshot rearm(Snapshot current, int rounds) {
        Snapshot c = current == null ? Snapshot.initial() : current;
        int left = c.leftRemaining();
        int right = c.rightRemaining();
        int budget = Math.max(0, rounds);
        int pod = left <= right ? 0 : 1;
        while (budget > 0 && left + right < READY_CAPACITY
                && (left < TUBES_PER_POD || right < TUBES_PER_POD)) {
            if (pod == 0) {
                if (left < TUBES_PER_POD) {
                    left++;
                    budget--;
                }
            } else if (right < TUBES_PER_POD) {
                right++;
                budget--;
            }
            pod = (pod + 1) % PODS;
        }
        return new Snapshot(left, right, c.nextPod(), c.fireCooldown(), c.wasHeld());
    }

    public static Snapshot empty() {
        return new Snapshot(0, 0, 0, 0, false);
    }
}
