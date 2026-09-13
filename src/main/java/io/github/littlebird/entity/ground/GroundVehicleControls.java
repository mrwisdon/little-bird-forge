package io.github.littlebird.entity.ground;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * What the client input and the network layer need from a ground vehicle, without either of them
 * knowing whether they are talking to a tank or an IFV. The two hulls keep their own seat plans
 * and their own weapons; this is only the shared vocabulary.
 */
public interface GroundVehicleControls {
    /** Seat index of a rider, or -1 if they are not aboard. */
    int seatOf(Entity passenger);

    /** True when this seat drives the hull. */
    boolean isDriver(int seat);

    /** True when this seat may fire a weapon. */
    boolean isOperator(int seat);

    /** Client only: the drive axes for this client's driver. */
    void setClientControls(int controls);

    /** Client only: forgets the local prediction when the hull is left behind. */
    void resetClientPrediction();

    /**
     * Server: a rider reports the trigger, one press of the reload key, and how many steps to move
     * through their shell and weapon carousels this tick (0 when the key was not pressed).
     */
    void applyWeaponInput(Player sender, boolean triggerHeld, boolean reload, int shellDelta,
                          int weaponDelta);
}
