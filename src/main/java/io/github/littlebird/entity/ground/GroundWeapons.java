package io.github.littlebird.entity.ground;

import io.github.littlebird.entity.GunshipRider;
import io.github.littlebird.entity.HitFeedback;
import io.github.littlebird.entity.weapon.CanisterShot;
import io.github.littlebird.entity.AirframeTargets;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import net.minecraft.server.level.ServerPlayer;
import io.github.littlebird.network.LittleBirdTracerPacket;
import io.github.littlebird.network.ModNetwork;
import io.github.littlebird.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
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

    /**
     * True for a hull of this pack, which is what the armoured damage price applies to. Kept as the
     * coarse question ("is this one of ours?"); whether a round can actually hurt it is decided by
     * {@link LittleBirdBallistics#damageTo(DamageType, ArmorClass, float, float)}.
     */
    public static boolean isArmored(Entity entity) {
        return AirframeTargets.armorClass(entity)
                != io.github.littlebird.entity.weapon.ArmorClass.UNARMORED;
    }

    /**
     * Fires one hitscan round and resolves everything it hits: the direct target, the blast at
     * the impact point, the tracer and the muzzle effects.
     */
    /**
     * One hitscan round. The three damage figures are the weapon's row: flesh, aircraft and heavy
     * armour. Machine guns pass zero for heavy armour and are then absorbed by it whatever the
     * other two say; every cannon carries a real penetration figure, and a zero there is legal.
     */
    public static void fireHitscan(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 muzzle, Vec3 direction,
            float directDamage, float armoredDamage, float heavyDamage, double splashRadius,
            float splashFraction, float soundPitch,
            io.github.littlebird.entity.weapon.DamageType type) {
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
            applyDamage(server, shooter, entityHit.getEntity(), directDamage, armoredDamage,
                    heavyDamage, type);
        }
        splash(server, vehicle, shooter, impact, entityHit == null ? null : entityHit.getEntity(),
                directDamage, armoredDamage, heavyDamage, splashRadius, splashFraction, type);

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

    /**
     * Fires one 120mm canister round: a spray of pellet hitscans from the muzzle, each its own
     * block-and-entity ray, so cover and individual targets stop pellets individually. The muzzle
     * sound and smoke are played once here, not per pellet.
     */
    public static void fireCanister(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 muzzle, Vec3 aim,
            int pellets, double horizontalSpreadDegrees, double verticalSpreadDegrees,
            float directDamage, float armoredDamage, float heavyDamage, double range) {
        Vec3 direction = aim.normalize();
        if (direction.lengthSqr() <= 1.0E-9D) {
            return;
        }
        RandomSource random = server.getRandom();
        int count = Math.max(1, pellets);
        double rangeBlocks = Math.max(1.0D, range);
        LittleBirdBallistics.Vector3 aimVector =
                new LittleBirdBallistics.Vector3(direction.x, direction.y, direction.z);
        for (int i = 0; i < count; i++) {
            LittleBirdBallistics.Vector3 pellet = CanisterShot.direction(aimVector,
                    horizontalSpreadDegrees, verticalSpreadDegrees,
                    random.nextDouble(), random.nextDouble());
            firePellet(server, vehicle, shooter, muzzle,
                    new Vec3(pellet.x(), pellet.y(), pellet.z()),
                    directDamage, armoredDamage, heavyDamage, rangeBlocks);
        }
        server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                14, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    /** One pellet: a single hitscan ray, damaging the first live entity it meets. */
    private static void firePellet(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 muzzle, Vec3 direction,
            float directDamage, float armoredDamage, float heavyDamage, double range) {
        Vec3 far = muzzle.add(direction.scale(range));
        BlockHitResult blockHit = server.clip(new ClipContext(
                muzzle, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
        Vec3 impact = blockHit.getType() == HitResult.Type.MISS ? far : blockHit.getLocation();

        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(server, vehicle, muzzle,
                impact, vehicle.getBoundingBox()
                        .expandTowards(direction.scale(muzzle.distanceTo(impact))).inflate(1.0D),
                candidate -> candidate != vehicle && candidate != shooter
                        && !vehicle.hasPassenger(candidate)
                        && candidate.isAlive() && !candidate.isSpectator());
        int impactType;
        if (entityHit != null) {
            impact = entityHit.getLocation();
            impactType = LittleBirdTracerPacket.IMPACT_ENTITY;
            applyDamage(server, shooter, entityHit.getEntity(), directDamage, armoredDamage,
                    heavyDamage, io.github.littlebird.entity.weapon.DamageType.BULLET);
        } else {
            impactType = blockHit.getType() == HitResult.Type.MISS
                    ? LittleBirdTracerPacket.IMPACT_MISS
                    : LittleBirdTracerPacket.IMPACT_BLOCK;
        }
        ModNetwork.sendTracer(vehicle, new LittleBirdTracerPacket(
                muzzle.x, muzzle.y, muzzle.z, impact.x, impact.y, impact.z, impactType, 2));
    }

    private static void applyDamage(ServerLevel server, Player shooter, Entity target,
                                    float direct, float armored, float heavy,
                                    io.github.littlebird.entity.weapon.DamageType type) {
        float damage = LittleBirdBallistics.damageTo(type, AirframeTargets.armorClass(target),
                new LittleBirdBallistics.WeaponDamage(direct, armored, heavy));
        if (damage <= 0.0F || shooter == null) {
            return;
        }
        boolean applied = target.hurt(server.damageSources().playerAttack(shooter), damage);
        if (applied && shooter instanceof ServerPlayer serverPlayer) {
            HitFeedback.report(serverPlayer, target);
        }
    }

    private static void splash(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 impact, Entity directHit,
            float direct, float armored, float heavy, double radius, float fraction,
            io.github.littlebird.entity.weapon.DamageType type) {
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
            float damage = LittleBirdBallistics.damageTo(type, AirframeTargets.armorClass(nearby),
                    new LittleBirdBallistics.WeaponDamage(direct, armored, heavy))
                    * fraction * (float) falloff;
            if (damage > 0.0F && shooter != null) {
                boolean applied = nearby.hurt(
                        server.damageSources().playerAttack(shooter), damage);
                if (applied && shooter instanceof ServerPlayer serverPlayer) {
                    HitFeedback.report(serverPlayer, nearby);
                }
            }
        }
    }
}
