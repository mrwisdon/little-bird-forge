package io.github.littlebird.entity;

import io.github.littlebird.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * One place a weapon reports a landed hit: the shooter's client gets a hit marker, and for a pack
 * hull the target's remaining integrity for the enemy bar. No weapon needs to know the target's
 * class - adding a vehicle means implementing {@link GunshipRider#healthFraction()}.
 */
public final class HitFeedback {
    private HitFeedback() {
    }

    /** Sends the hit marker and, for a hull, the target readout for one landed hit. */
    public static void report(ServerPlayer shooter, Entity target) {
        ModNetwork.sendHitFeedback(shooter, !target.isAlive(),
                target instanceof GunshipRider, healthFractionOf(target));
    }

    /** 0..1 hull integrity of the target, whatever kind of thing it is. */
    public static float healthFractionOf(Entity target) {
        if (target instanceof GunshipRider rider) {
            return clamp01(rider.healthFraction());
        }
        if (target instanceof LivingEntity living && living.getMaxHealth() > 0.0F) {
            return clamp01(living.getHealth() / living.getMaxHealth());
        }
        return 1.0F;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
