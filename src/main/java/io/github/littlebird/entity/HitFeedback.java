package io.github.littlebird.entity;

import io.github.littlebird.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * One place a weapon reports a landed hit: the shooter's client gets a hit marker, the target's
 * name and how much of it is left. No weapon needs to know the target's class - a pack hull
 * answers through {@link GunshipRider}, everything living through vanilla, and
 * {@link HitReadout} turns the pair of numbers into the line the HUD prints.
 */
public final class HitFeedback {
    private HitFeedback() {
    }

    /** Sends the hit marker and the target readout for one landed hit. */
    public static void report(ServerPlayer shooter, Entity target) {
        if (shooter == null) {
            // A warhead can outlive the credit it was fired under (the caller left the seat or the
            // world). There is no client to show a marker on, so report nothing.
            return;
        }
        float fraction = healthFractionOf(target);
        ModNetwork.sendHitFeedback(shooter, !target.isAlive(),
                HitReadout.targetName(target.getDisplayName().getString()),
                healthOf(target), maxHealthOf(target, fraction));
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

    /** Remaining integrity in points, or 0 for a target that publishes none. */
    public static float healthOf(Entity target) {
        if (target instanceof GunshipRider rider) {
            return Math.max(0.0F, rider.getHealth());
        }
        if (target instanceof LivingEntity living) {
            return Math.max(0.0F, living.getHealth());
        }
        return 0.0F;
    }

    /** Full integrity in points, or 0 when it cannot be known. */
    public static float maxHealthOf(Entity target, float fraction) {
        if (target instanceof LivingEntity living) {
            return Math.max(0.0F, living.getMaxHealth());
        }
        return HitReadout.maxHealthFrom(healthOf(target), fraction);
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
