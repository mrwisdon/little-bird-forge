package io.github.littlebird.entity.weapon;

/**
 * A single ammunition pool: the 7.62mm belt, the 30mm magazine. Unlike the rocket magazines this
 * has no ready/reserve split - a belt is a belt - so it is just a count with a capacity.
 *
 * Resupply is not modelled here: nothing refills by itself any more, the ammunition crate does
 * it (see AmmoSupply). Pure rules: no Minecraft classes.
 */
public final class AmmoPool {
    private final int capacity;
    private int rounds;

    private AmmoPool(int capacity, int rounds) {
        this.capacity = Math.max(0, capacity);
        this.rounds = Math.max(0, Math.min(this.capacity, rounds));
    }

    /** A full belt. */
    public static AmmoPool full(int capacity) {
        return new AmmoPool(capacity, capacity);
    }

    public static AmmoPool empty(int capacity) {
        return new AmmoPool(capacity, 0);
    }

    public int capacity() {
        return capacity;
    }

    public int rounds() {
        return rounds;
    }

    public boolean isEmpty() {
        return rounds <= 0;
    }

    /** True when the shot was taken; an empty belt refuses. */
    public boolean fire(int roundsPerShot) {
        int cost = Math.max(1, roundsPerShot);
        if (rounds < cost) {
            return false;
        }
        rounds -= cost;
        return true;
    }

    /** Tops the pool up from a crate, never past its capacity. Returns the rounds taken. */
    public int refill(int units) {
        int wanted = Math.max(0, units);
        int taken = Math.min(wanted, capacity - rounds);
        rounds += taken;
        return taken;
    }

    /** 0..1 fill, for the HUD. */
    public float fraction() {
        return capacity <= 0 ? 0.0F : rounds / (float) capacity;
    }
}
