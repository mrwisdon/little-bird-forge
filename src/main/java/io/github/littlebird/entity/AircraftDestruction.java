package io.github.littlebird.entity;

/**
 * One shot destruction guard for an airframe.
 *
 * Writing an aircraft off explodes it. That blast damages whatever is next to it, so a second
 * aircraft dies in turn, its blast damages the first one again - which is still mid destruction
 * and still at or below zero health - and the two of them take turns exploding until the stack
 * runs out. The crash report that produced this class was a StackOverflowError bouncing between
 * ApacheEntity.destroyAircraft and ApacheEntity.hurt through Explosion.
 *
 * This remembers that the airframe is already gone, so its destruction runs exactly once and
 * further damage is ignored. Pure rules: no Minecraft classes, so the cycle is unit tested.
 */
public final class AircraftDestruction {
    private boolean destroyed;

    /** True the first time only: the explosion that writes the airframe off runs once. */
    public boolean beginDestruction() {
        if (destroyed) {
            return false;
        }
        destroyed = true;
        return true;
    }

    /** False once the airframe is gone, so a blast cannot restart the chain. */
    public boolean acceptsDamage() {
        return !destroyed;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
