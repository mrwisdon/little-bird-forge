package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.ground.AutocannonImpactFx;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * One 25mm M242 round: a small, fast, ballistic shell with a two block blast, replacing the
 * Bradley's old instant-hit bullets.
 *
 * <p>It is deliberately not the 120mm {@link MainGunShellEntity}. That round shares the main gun's
 * terrain-breaking switch and a full-size impact, which is fine once every few seconds and wrong at
 * 300 rounds a minute: an autocannon must not plough the map. This shell therefore only ever
 * damages entities - blast falloff out to its splash radius - and draws a light tracer instead of a
 * detonation.
 *
 * <p>Server authoritative, like every other projectile in the pack: spawned, stepped and resolved on
 * the server, with ordinary entity tracking carrying the flight out to the clients.
 */
public class AutocannonShellEntity extends Entity {
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 heading = new Vec3(0.0D, 0.0D, 1.0D);
    private UUID ownerUuid;
    private float directDamage;
    private float armoredDamage;
    private float heavyDamage;
    private double splashRadius = 2.0D;
    /** Table row when configured by weapon; null keeps the legacy three numbers. */
    private io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon;

    public AutocannonShellEntity(EntityType<? extends AutocannonShellEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    /** Prices this round: what it does to flesh, what it does to armour, and how wide the blast is. */
    /** Prices this round from the vehicle damage table. */
    public void configure(io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon,
                          double splash) {
        this.weapon = weapon;
        var row = io.github.littlebird.entity.weapon.VehicleDamageTable.row(weapon);
        configure(row.flesh(), row.aircraft(), row.heavy(), splash);
    }

    public void configure(float direct, float armored, float heavy, double splash) {
        this.directDamage = direct;
        this.armoredDamage = armored;
        this.heavyDamage = heavy;
        this.splashRadius = Math.max(0.0D, splash);
    }

    public void launch(Vec3 direction, double speed, Entity owner) {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        Vec3 unit = direction.normalize();
        this.velocity = unit.scale(speed);
        this.heading = unit;
        setYRot((float) Math.toDegrees(Math.atan2(-unit.x, unit.z)));
        setXRot((float) Math.toDegrees(-Math.asin(unit.y)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        if (tickCount > LittleBirdConfig.bradleyAutocannonShellLifetimeTicks()) {
            discard();
            return;
        }
        Vec3 start = position();
        velocity = vec(MainGunShellBallistics.advance(vector(velocity)));
        setDeltaMovement(velocity);
        Vec3 end = start.add(velocity);

        // Sweep the whole tick: a shell that steps several blocks must not tunnel through a target.
        EntityHitResult directHit = findEntityHit(start, end);
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockIsNearer = blockHit.getType() != HitResult.Type.MISS
                && (directHit == null
                        || start.distanceToSqr(blockHit.getLocation())
                                <= start.distanceToSqr(directHit.getLocation()));
        if (blockIsNearer) {
            setPos(blockHit.getLocation());
            detonate(null, Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()));
            return;
        }
        if (directHit != null) {
            setPos(directHit.getLocation());
            detonate(directHit.getEntity(), null);
            return;
        }
        move(MoverType.SELF, velocity);
        if (horizontalCollision || verticalCollision || onGround()) {
            detonate(null, collisionNormal());
            return;
        }
        spawnTracer();
    }

    /** Nearest live entity along this tick's segment, never the shooter or the hull they ride. */
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        Entity owner = owner();
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        return ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(0.5D),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && candidate != this && candidate != owner
                        && candidate != ownerVehicle
                        && !(candidate instanceof AutocannonShellEntity));
    }

    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }

    /**
     * Keeps the round drawn far past vanilla's own cull.
     *
     * <p>{@code Entity.shouldRenderAtSqrDistance} scales its limit by the entity's own bounding box -
     * 64 times the box's largest side - and this round is a 0.25 block bullet, so the vanilla rule
     * stops drawing it sixteen blocks out. That is a fraction of the range the Bradley actually fires
     * at, and well inside the distance its tracer is meant to be read at, so the limit comes from the
     * client config instead and the world's render distance becomes the honest upper bound: nothing
     * in an unloaded chunk can be drawn at all, however far this allows.
     *
     * <p>Only the render dispatcher asks this, so reading a client config value here is safe on a
     * dedicated server, which never calls it.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double limit = LittleBirdConfig.autocannonShellVisibleDistanceBlocks();
        if (!Double.isFinite(limit) || limit <= 0.0D) {
            return super.shouldRenderAtSqrDistance(distance);
        }
        return distance < limit * limit;
    }

    /**
     * The round's own wake: a thin thread of fume instead of a travelling flame.
     *
     * <p>A fireball welded to the round reads as a slow rocket, and at five rounds a second anything
     * brighter would smear a whole burst into one cloud that never clears. One barely spreading smoke
     * puff every other tick keeps the flight path scratched into the air, and the bright read that
     * carries the eye is the client side tracer streak, which does not depend on the server tick rate
     * or on a packet arriving.
     */
    private void spawnTracer() {
        if (!(level() instanceof ServerLevel server) || tickCount % 2 != 0) {
            return;
        }
        server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(), 1,
                0.005D, 0.005D, 0.005D, 0.0D);
    }

    /**
     * Direct hit, then the 25mm impact package and the damage.
     *
     * <p>Damage is resolved here so it lands exactly once whichever way the round ended; the sparks,
     * chips, sharp flash, report and the camera kick that nearby clients get are handed to
     * {@link AutocannonImpactFx}, which is deliberately a smaller, sharper package than the 120mm's:
     * this gun lands five rounds a second, so a heavy hit would read as a permanent smoke wall.
     *
     * @param directHit the entity that was hit, or null for a block or a swept collision
     * @param normal    the surface normal, or null when the hit had no face (an entity, or a
     *                  collision the sweep reported without one)
     */
    private void detonate(Entity directHit, Vec3 normal) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            if (directHit != null && directHit != owner) {
                float damage = weapon != null
                        ? io.github.littlebird.entity.weapon.VehicleDamageTable.damage(
                                weapon, directHit)
                        : LittleBirdBallistics.damageTo(
                                io.github.littlebird.entity.weapon.DamageType.SHELL,
                                io.github.littlebird.entity.AirframeTargets.armorClass(directHit),
                                new LittleBirdBallistics.WeaponDamage(
                                        directDamage, armoredDamage, heavyDamage));
                if (damage > 0.0F) {
                    // Credited to the driver who fired, the way the rocket credits its pilot.
                    boolean applied = directHit.hurt(
                            io.github.littlebird.entity.weapon.WarheadSplash.source(server, owner), damage);
                    if (applied && owner instanceof ServerPlayer serverPlayer) {
                        HitFeedback.report(serverPlayer, directHit);
                    }
                }
            }
            // A hard, tight hit: chips and sparks thrown along the face that was struck, one sharp
            // flash, a little smoke, and the report. Terrain damage stays opt-in, exactly like the
            // rocket - an autocannon that craters the map is unusable.
            AutocannonImpactFx.detonate(server, this, position(), normal, 1.0D, splashRadius,
                    LittleBirdConfig.bradleyAutocannonBreaksBlocks());
        }
        discard();
    }

    /**
     * The face a swept collision ran into, for the impact package. Minecraft reports a collision as
     * a flag rather than a plane, so it is rebuilt from the motion: a floor hit throws its chips
     * straight up, a wall hit throws them back along the wall's own outward normal.
     */
    private Vec3 collisionNormal() {
        if (onGround() || verticalCollision) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        Vec3 flat = new Vec3(-velocity.x, 0.0D, -velocity.z);
        return flat.lengthSqr() < 1.0E-6D ? null : flat.normalize();
    }

    private static Vector3 vector(Vec3 value) {
        return new Vector3(value.x, value.y, value.z);
    }

    private static Vec3 vec(Vector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }
}
