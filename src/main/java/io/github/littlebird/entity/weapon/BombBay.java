package io.github.littlebird.entity.weapon;

/**
 * The B-2 bay's release schedule: how much a trigger pull commits, how long the bay then stays
 * shut and when it may open again.
 *
 * <p>A salvo is a ripple, not a single blow: the bay opens and drops one bomb every
 * {@link #RELEASE_INTERVAL_TICKS} until {@link #SALVO_COUNT} are gone, which is why a salvo also
 * locks the bay for the length of that ripple while a single stick frees it after one interval.
 *
 * <p>Pure rules: no Minecraft classes, no state - the caller owns the remaining count and the
 * cooldown, so the schedule is unit testable on its own.
 */
public final class BombBay {
    /** Bombs one salvo releases. */
    public static final int SALVO_COUNT = 2;
    /** Ticks between two bombs of the same ripple. */
    public static final int RELEASE_INTERVAL_TICKS = 6;

    private BombBay() {
    }

    /** True when there is a bomb left and the bay is not still working through its cooldown. */
    public static boolean canRelease(int remaining, int cooldownTicks) {
        return remaining > 0 && cooldownTicks <= 0;
    }

    /** Cooldown a release leaves behind: a salvo keeps the bay shut for its whole ripple. */
    public static int nextCooldown(boolean salvo) {
        return RELEASE_INTERVAL_TICKS * (salvo ? SALVO_COUNT : 1);
    }

    /** Bombs this trigger pull commits: a salvo takes {@link #SALVO_COUNT}, a single takes one. */
    public static int bombsToDrop(boolean salvo, int remaining) {
        int wanted = salvo ? SALVO_COUNT : 1;
        return Math.max(0, Math.min(wanted, Math.max(0, remaining)));
    }
}
