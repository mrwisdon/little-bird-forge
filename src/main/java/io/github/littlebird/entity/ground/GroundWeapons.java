package io.github.littlebird.entity.ground;

import io.github.littlebird.entity.GunshipRider;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.network.LittleBirdTracerPacket;
import io.github.littlebird.network.ModNetwork;
import io.github.littlebird.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side gunfire shared by the two ground vehicles.
 *
 * <p>Every ground weapon is a hitscan ray from the muzzle: the server owns the raycast, so a
 * client cannot claim hits it did not make. What differs between a tank and an IFV is only the
 * numbers passed in - range, damage, splash - never the plumbing, and the damage model is priced
 * by target the same way the aircraft guns are: a vehicle (any {@link GunshipRider}, which every
 * hull in the pack implements) takes the armoured price, anything else the full one.
 */
public final class GroundWeapons {
    /** Ground guns do not shoot past this, even across open ground. */
    public static final double RANGE_BLOCKS = 128.0D;

    private GroundWeapons() {
    }

    /** True for a hull of this pack, which is what the armoured damage price applies to. */
    public static boolean isArmored(Entity entity) {
        return entity instanceof GunshipRider;
    }

    /**
     * Fires one hitscan round and resolves everything it hits: the direct target, the blast at
     * the impact point, the tracer and the muzzle effects.
     */
    public static void fireHitscan(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 muzzle, Vec3 direction,
            float directDamage, float armoredDamage, double splashRadius, float splashFraction,
            float soundPitch) {
        Vec3 far = muzzle.add(direction.scale(RANGE_BLOCKS));
        BlockHitResult blockHit = server.clip(new ClipContext(
                muzzle, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? far : blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(server, vehicle, muzzle,
                impact, vehicle.getBoundingBox()
                        .expandTowards(direction.scale(muzzle.distanceTo(impact))).inflate(1.0D),
                candidate -> candidate != vehicle && candidate != shooter
                        && !vehicle.hasPassenger(candidate)
                        && candidate.isAlive() && !candidate.isSpectator());
        if (entityHit != null) {
            impact = entityHit.getLocation();
            applyDamage(server, shooter, entityHit.getEntity(), directDamage, armoredDamage);
        }
        splash(server, vehicle, shooter, impact, entityHit == null ? null : entityHit.getEntity(),
                directDamage, armoredDamage, splashRadius, splashFraction);

        int impactType = entityHit != null
                ? LittleBirdTracerPacket.IMPACT_ENTITY
                : blockHit.getType() == HitResult.Type.MISS
                        ? LittleBirdTracerPacket.IMPACT_MISS
                        : LittleBirdTracerPacket.IMPACT_BLOCK;
        ModNetwork.sendTracer(vehicle, new LittleBirdTracerPacket(
                muzzle.x, muzzle.y, muzzle.z, impact.x, impact.y, impact.z, impactType, 1));
        server.sendParticles(ParticleTypes.SMOKE,
                muzzle.x, muzzle.y, muzzle.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
        server.playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(),
                ModSounds.GUN_SHOT.get(), SoundSource.PLAYERS, 1.0F, soundPitch);
        if (splashRadius >= 1.5D) {
            server.playSound(null, impact.x, impact.y, impact.z, ModSounds.EXPLOSION.get(),
                    SoundSource.PLAYERS, 0.9F, 0.8F);
        }
    }

    private static void applyDamage(
            ServerLevel server, Player shooter, Entity target, float direct, float armored) {
        boolean armoredTarget = isArmored(target);
        float damage = LittleBirdBallistics.damageTo(armoredTarget, direct, armored);
        if (damage <= 0.0F || shooter == null) {
            return;
        }
        boolean applied = target.hurt(server.damageSources().playerAttack(shooter), damage);
        if (applied && shooter instanceof ServerPlayer serverPlayer) {
            ModNetwork.sendHitFeedback(serverPlayer, !target.isAlive(), armoredTarget,
                    healthFraction(target));
        }
    }

    /** Remaining health of a hit target as a 0..1 fraction, ground hulls included. */
    private static float healthFraction(Entity target) {
        if (target instanceof io.github.littlebird.entity.MainBattleTankEntity tank) {
            return Math.max(0.0F, Math.min(1.0F,
                    tank.getHealth() / io.github.littlebird.entity.MainBattleTankEntity.MAX_HEALTH));
        }
        if (target instanceof io.github.littlebird.entity.InfantryFightingVehicleEntity ifv) {
            return Math.max(0.0F, Math.min(1.0F, ifv.getHealth()
                    / io.github.littlebird.entity.InfantryFightingVehicleEntity.MAX_HEALTH));
        }
        return io.github.littlebird.entity.LittleBirdEntity.healthFractionOf(target);
    }

    private static void splash(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 impact, Entity directHit,
            float direct, float armored, double radius, float fraction) {
        if (radius <= 0.0D) {
            return;
        }
        AABB blast = new AABB(impact, impact).inflate(radius);
        for (Entity nearby : server.getEntities(vehicle, blast,
                candidate -> candidate != directHit && candidate != vehicle && candidate != shooter
                        && !vehicle.hasPassenger(candidate)
                        && candidate.isAlive() && !candidate.isSpectator())) {
            double distance = nearby.position().distanceTo(impact);
            double falloff = Math.max(0.0D, 1.0D - distance / radius);
            boolean armoredTarget = isArmored(nearby);
            float damage = LittleBirdBallistics.damageTo(armoredTarget, direct, armored)
                    * fraction * (float) falloff;
            if (damage > 0.0F) {
                applyDamage(server, shooter, nearby, damage, damage);
            }
        }
    }
}
