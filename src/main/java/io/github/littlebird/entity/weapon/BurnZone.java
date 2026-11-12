package io.github.littlebird.entity.weapon;

/**
 * The life of one patch of burning ground, as pure arithmetic: how long it burns, when the fire
 * needs relighting, and when it is due to hurt whatever is standing in it.
 *
 * <p>Two clocks run off one countdown. Damage lands once a second
 * ({@link #DAMAGE_INTERVAL_TICKS}); the ground is relit every two seconds
 * ({@link #RELIGHT_INTERVAL_TICKS}), because a vanilla fire block on bare stone or dirt - the only
 * kind this weapon can light - goes out on its own long before the fire is meant to be over.
 * Keeping both clocks here instead of inside the ticking code is what makes the behaviour testable.
 *
 * <p>The zone is done when {@link #expired()} turns true. The step returned by that last
 * {@link #advance()} still has to be carried out, so callers apply the step and then drop the zone.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class BurnZone {

    /** Ticks between damage pulses: one second of hurt. */
    public static final int DAMAGE_INTERVAL_TICKS = 20;

    /** Ticks between relights. Bare-ground fire is gone well before this. */
    public static final int RELIGHT_INTERVAL_TICKS = 40;

    /** What the owner of this zone owes the world on the tick just advanced. */
    public enum Step {
        IDLE,
        RELIGHT,
        DAMAGE,
        DAMAGE_AND_RELIGHT,
        EXPIRE
    }

    private final int lifetimeTicks;
    private int remainingTicks;
    private int sinceDamage;
    private int sinceRelight;
    private boolean expired;

    /** A zone that burns for {@code lifetimeTicks}; anything below zero counts as zero. */
    public BurnZone(int lifetimeTicks) {
        this.lifetimeTicks = Math.max(0, lifetimeTicks);
        this.remainingTicks = this.lifetimeTicks;
    }

    public int lifetimeTicks() {
        return lifetimeTicks;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public boolean expired() {
        return expired;
    }

    /**
     * Advances one tick. The first pulse lands a full interval in, not on the tick the canister
     * went off, because the blast itself has just been settled.
     */
    public Step advance() {
        if (expired) {
            return Step.EXPIRE;
        }
        remainingTicks--;
        sinceDamage++;
        sinceRelight++;
        boolean damage = sinceDamage >= DAMAGE_INTERVAL_TICKS;
        boolean relight = sinceRelight >= RELIGHT_INTERVAL_TICKS;
        if (damage) {
            sinceDamage = 0;
        }
        if (relight) {
            sinceRelight = 0;
        }
        if (remainingTicks <= 0) {
            expired = true;
        }
        if (damage && relight) {
            return Step.DAMAGE_AND_RELIGHT;
        }
        if (damage) {
            return Step.DAMAGE;
        }
        if (relight) {
            return Step.RELIGHT;
        }
        return expired ? Step.EXPIRE : Step.IDLE;
    }
}
