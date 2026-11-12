package io.github.littlebird.entity.ground;

import io.github.littlebird.entity.HitFeedback;
import io.github.littlebird.entity.weapon.CanisterShot;
import io.github.littlebird.entity.AirframeTargets;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import net.minecraft.server.level.ServerPlayer;
import io.github.littlebird.network.LittleBirdTracerPacket;
import io.github.littlebird.network.ModNetwork;
import io.github.littlebird.registry.ModSounds;
import java.util.function.Function;
import javax.annotation.Nullable;
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
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Server-side hitscan weapons for ground vehicles. Each shot sweeps from the muzzle and applies
 * damage according to the weapon and the target's armour class.
 */
public final class GroundWeapons {
    /** Ground guns do not shoot past this, even across open ground. */
    public static final double RANGE_BLOCKS = 128.0D;
    private static final double PELLET_SWEEP_SEGMENT_BLOCKS = 6.0D;
    private static final double SWEEP_QUERY_PADDING = 1.0E-6D;

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
            io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon,
            double splashRadius, float splashFraction, float soundPitch) {
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
            applyDamage(server, shooter, entityHit.getEntity(), weapon);
        }
        splash(server, vehicle, shooter, impact, entityHit == null ? null : entityHit.getEntity(),
                weapon, splashRadius, splashFraction);

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
     * Fires one 120mm canister round. Each pellet sweeps its own volume, so cover and individual
     * targets stop pellets independently. Muzzle smoke is emitted once for the whole round.
     */
    public static void fireCanister(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 muzzle, Vec3 aim,
            int pellets, double horizontalSpreadDegrees, double verticalSpreadDegrees,
            io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon,
            double range, double pelletRadius) {
        Vec3 direction = aim.normalize();
        if (direction.lengthSqr() <= 1.0E-9D) {
            return;
        }
        RandomSource random = server.getRandom();
        int count = Math.max(1, pellets);
        double rangeBlocks = Math.max(1.0D, range);
        float radiusBlocks = Double.isFinite(pelletRadius) ? (float) Math.max(0.0D, pelletRadius) : 0.0F;
        LittleBirdBallistics.Vector3 aimVector =
                new LittleBirdBallistics.Vector3(direction.x, direction.y, direction.z);
        for (int i = 0; i < count; i++) {
            LittleBirdBallistics.Vector3 pellet = CanisterShot.direction(aimVector,
                    horizontalSpreadDegrees, verticalSpreadDegrees,
                    random.nextDouble(), random.nextDouble());
            firePellet(server, vehicle, shooter, muzzle,
                    new Vec3(pellet.x(), pellet.y(), pellet.z()),
                    weapon, rangeBlocks, radiusBlocks);
        }
        server.sendParticles(ParticleTypes.SMOKE, muzzle.x, muzzle.y, muzzle.z,
                14, 0.25D, 0.25D, 0.25D, 0.01D);
    }

    /** One pellet damages the first live entity reached before cover. */
    private static void firePellet(
            ServerLevel server, Entity vehicle, Player shooter, Vec3 muzzle, Vec3 direction,
            io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon,
            double range, float radius) {
        Vec3 far = muzzle.add(direction.scale(range));
        Vec3 blockImpact = pelletBlockImpact(server, vehicle, muzzle, far, radius);
        Vec3 impact = blockImpact == null ? far : blockImpact;
        int impactType = blockImpact == null
                ? LittleBirdTracerPacket.IMPACT_MISS : LittleBirdTracerPacket.IMPACT_BLOCK;

        AABB sweptBounds = new AABB(muzzle, impact).inflate(radius + SWEEP_QUERY_PADDING);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(server, vehicle, muzzle,
                impact, sweptBounds, candidate -> candidate != vehicle && candidate != shooter
                        && !vehicle.hasPassenger(candidate)
                        && candidate.isAlive() && !candidate.isSpectator(), radius);
        if (entityHit != null) {
            Entity target = entityHit.getEntity();
            // ProjectileUtil ranks by the entry point but reports the entity's position.
            Vec3 contact = target.getBoundingBox().inflate(radius).clip(muzzle, impact).orElse(null);
            if (contact != null && (blockImpact == null
                    || muzzle.distanceToSqr(contact) < muzzle.distanceToSqr(blockImpact))) {
                impact = contact;
                impactType = LittleBirdTracerPacket.IMPACT_ENTITY;
                applyDamage(server, shooter, target, weapon);
            }
        }
        ModNetwork.sendTracer(vehicle, new LittleBirdTracerPacket(
                muzzle.x, muzzle.y, muzzle.z, impact.x, impact.y, impact.z, impactType,
                LittleBirdTracerPacket.STATION_CANISTER));
    }

    @Nullable
    private static Vec3 pelletBlockImpact(
            ServerLevel server, Entity vehicle, Vec3 start, Vec3 end, double radius) {
        if (radius == 0.0D) {
            BlockHitResult hit = server.clip(new ClipContext(
                    start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, vehicle));
            return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
        }
        return sweepPelletBlocks(start, end, radius, query -> server.getBlockCollisions(vehicle, query));
    }

    @Nullable
    static Vec3 sweepPelletBlocks(Vec3 start, Vec3 end, double radius,
                                  Function<AABB, Iterable<VoxelShape>> blockCollisions) {
        // Bound each collision query so diagonal shots do not scan the whole enclosing box.
        int segments = (int) Math.ceil(start.distanceTo(end) / PELLET_SWEEP_SEGMENT_BLOCKS);
        Vec3 segmentStart = start;
        for (int i = 1; i <= segments; i++) {
            Vec3 segmentEnd = start.lerp(end, (double) i / segments);
            AABB query = new AABB(segmentStart, segmentEnd).inflate(radius + SWEEP_QUERY_PADDING);
            Vec3 nearest = null;
            double nearestDistanceSqr = start.distanceToSqr(segmentEnd);
            for (VoxelShape shape : blockCollisions.apply(query)) {
                for (AABB box : shape.toAabbs()) {
                    // Inflating the solid by the pellet radius lets us sweep its centre.
                    AABB expanded = box.inflate(radius);
                    if (expanded.distanceToSqr(start) == 0.0D) {
                        return start;
                    }
                    // Use the full ray so contact on a query boundary is still found.
                    Vec3 contact = expanded.clip(start, end).orElse(null);
                    if (contact != null) {
                        double distanceSqr = start.distanceToSqr(contact);
                        if (distanceSqr <= nearestDistanceSqr) {
                            nearest = contact;
                            nearestDistanceSqr = distanceSqr;
                        }
                    }
                }
            }
            if (nearest != null) {
                return nearest;
            }
            segmentStart = segmentEnd;
        }
        return null;
    }

    private static void applyDamage(ServerLevel server, Player shooter, Entity target,
                                    io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon) {
        float damage = io.github.littlebird.entity.weapon.VehicleDamageTable.damage(weapon, target);
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
            io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon,
            double radius, float fraction) {
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
            float damage = io.github.littlebird.entity.weapon.VehicleDamageTable.damage(
                    weapon, nearby) * fraction * (float) falloff;
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
