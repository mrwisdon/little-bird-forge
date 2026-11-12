package io.github.littlebird.entity.weapon;

import io.github.littlebird.entity.AirframeTargets;
import io.github.littlebird.entity.HitFeedback;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Table-priced warhead blast.
 *
 * <p>Vanilla explosions price entities by explosion power and distance and know nothing about
 * armour, which let a near miss out-damage a direct hit on a hull. Every warhead now settles its
 * blast here instead: the same table row as the direct hit, reduced by the blast fraction and the
 * distance falloff. Armoured vehicles therefore take a few points from a near miss, not a vanilla
 * cratering, and an unarmoured target still takes the full anti-personnel price.
 */
public final class WarheadSplash {
    private WarheadSplash() {
    }

    /** The damage one target takes at {@code distance} from a blast of {@code radius}. */
    public static float damageAt(VehicleDamageTable.Weapon weapon, Entity target, double distance,
                                 double radius, float fraction) {
        return damageAt(VehicleDamageTable.row(weapon), AirframeTargets.armorClass(target),
                AirframeTargets.classify(target), distance, radius, fraction);
    }

    /** The same blast maths for a caller that already resolved the target's classes. */
    public static float damageAt(VehicleDamageTable.Row row, ArmorClass armor,
                                 LittleBirdBallistics.Airframe airframe, double distance,
                                 double radius, float fraction) {
        if (!(radius > 0.0D) || !(fraction > 0.0F)) {
            return 0.0F;
        }
        if (isVehicleClass(armor)) {
            // Splash does not touch vehicles at all: a hull is only ever hit directly.
            return 0.0F;
        }
        double falloff = Math.max(0.0D, Math.min(1.0D, 1.0D - distance / radius));
        return VehicleDamageTable.apply(row, armor, airframe) * fraction * (float) falloff;
    }

    private static boolean isVehicleClass(ArmorClass armor) {
        return armor == ArmorClass.HEAVY_ARMOR || armor == ArmorClass.LIGHT_ARMOR
                || armor == ArmorClass.AIRFRAME;
    }

    /** A direct hit's damage source: a player attack when a crew is credited, so armour pricing
     * stays honest, and a plain hit otherwise. */
    public static net.minecraft.world.damagesource.DamageSource source(ServerLevel server,
                                                                        Entity owner) {
        if (owner instanceof ServerPlayer player) {
            return server.damageSources().playerAttack(player);
        }
        if (owner != null && owner.getControllingPassenger() instanceof ServerPlayer pilot) {
            return server.damageSources().playerAttack(pilot);
        }
        return server.damageSources().generic();
    }

    /**
     * Applies the blast to every eligible target around the impact. The direct hit is excluded -
     * the caller has already priced it once - and so are the source round and the crew.
     */
    public static void apply(ServerLevel server, Entity source, Entity directHit, PlayerCredit credit,
                             VehicleDamageTable.Weapon weapon, Vec3 centre, double radius,
                             float fraction) {
        apply(server, source, directHit, credit, weapon, centre, radius, fraction, target -> 1.0D);
    }

    /** Applies a blast scaled by the visible fraction of each target. */
    public static void apply(ServerLevel server, Entity source, Entity directHit, PlayerCredit credit,
                             VehicleDamageTable.Weapon weapon, Vec3 centre, double radius,
                             float fraction, ToDoubleFunction<Entity> exposure) {
        if (!(radius > 0.0D) || !(fraction > 0.0F)) {
            return;
        }
        // A called-in round outlives its caller's seat: when nobody is left to credit, the blast
        // still lands, it just has no hit marker to send (a null client would crash the server).
        ServerPlayer shooter = credit == null ? null : credit.player();
        net.minecraft.world.damagesource.DamageSource blastSource = credit != null
                ? credit.damageSource(server) : server.damageSources().generic();
        AABB box = new AABB(centre, centre).inflate(radius);
        List<Entity> nearby = server.getEntities(source, box,
                candidate -> candidate != directHit && candidate != source && candidate.isAlive()
                        && !candidate.isSpectator()
                        && !(candidate instanceof net.minecraft.world.entity.player.Player player
                                && player == shooter));
        for (Entity target : nearby) {
            float damage = damageAt(weapon, target, target.position().distanceTo(centre), radius,
                    fraction);
            if (damage <= 0.0F) {
                continue;
            }
            double visible = exposure.applyAsDouble(target);
            if (!(visible > 0.0D)) {
                continue;
            }
            boolean applied = target.hurt(blastSource, damage * (float) Math.min(visible, 1.0D));
            if (applied && shooter != null) {
                HitFeedback.report(shooter, target);
            }
        }
    }

    /** The attacker a warhead credits: the player who fired it, or nobody. */
    public record PlayerCredit(ServerPlayer player) {
        /** Damage source for the blast; a direct player attack keeps armour pricing honest. */
        public net.minecraft.world.damagesource.DamageSource damageSource(ServerLevel server) {
            return player != null ? server.damageSources().playerAttack(player)
                    : server.damageSources().generic();
        }
    }
}
