package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.ground.MainGunImpactFx;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import io.github.littlebird.entity.weapon.WarheadSplash;
import io.github.littlebird.registry.ModSounds;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * One 120mm main gun shell: a fast, flat dart with a burning tracer, replacing the old instant
 * hitscan for the tank's main gun (the coaxial gun and every other weapon still use the hitscan).
 *
 * <p>Server authoritative, like the 70mm rocket: it is spawned, stepped and detonated on the
 * server, and ordinary entity tracking carries its position out to the clients. The round itself is
 * drawn client side as a dart with its own rigid tracer streak (see the client renderer); the
 * server owns the round's thin wake, the hard impact package at the point it lands and the damage
 * it deals. The muzzle flash is deliberately not centred on this entity, so a round leaving the
 * barrel reads as a shell rather than as a bullet streak. On impact it damages what it hit and, when
 * the server allows it, breaks the blocks it lands on.
 */
public class MainGunShellEntity extends Entity {
    private static final float HE_FRAGMENT_DAMAGE_FRACTION = 0.55F;
    private static final double FRAGMENT_FACE_OFFSET = 0.02D;

    private Vec3 velocity = Vec3.ZERO;
    private Vec3 heading = new Vec3(0.0D, 0.0D, 1.0D);
    private UUID ownerUuid;
    private MainGunAmmoType shellType = MainGunAmmoType.AP;
    private float directDamage;
    private float armoredDamage;
    private float heavyDamage;
    private double splashRadius = 3.0D;
    /** Which table row this round bills: AP / HE / canister. */
    private io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon weapon =
            io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.TANK_MAIN_GUN_AP;

    public MainGunShellEntity(EntityType<? extends MainGunShellEntity> type, Level level) {
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

    /** Prices this round: the shell type only chooses which config pair was read. */
    public void configure(MainGunAmmoType type, float direct, float armored, float heavy,
                          double splash) {
        this.shellType = type == null ? MainGunAmmoType.AP : type;
        this.weapon = switch (this.shellType) {
            case HE -> io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.TANK_MAIN_GUN_HE;
            case CANISTER -> io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon
                    .TANK_MAIN_GUN_CANISTER;
            default -> io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.TANK_MAIN_GUN_AP;
        };
        this.directDamage = direct;
        this.armoredDamage = armored;
        this.heavyDamage = heavy;
        this.splashRadius = splash;
    }

    public MainGunAmmoType shellType() {
        return shellType;
    }

    /** Points and launches the shell along the given direction. */
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
        if (tickCount > MainGunShellBallistics.LIFETIME_TICKS) {
            discard();
            return;
        }
        Vec3 start = position();
        velocity = vec(MainGunShellBallistics.advance(vector(velocity)));
        setDeltaMovement(velocity);
        Vec3 end = start.add(velocity);

        // Sweep the whole tick so a ten block step cannot tunnel through a target or a thin wall.
        Entity shooter = owner();
        Entity shooterVehicle = shooter == null ? null : shooter.getVehicle();
        HitResult hit = trace(level(), this, shooter, shooterVehicle, getBoundingBox(), start, end);
        if (hit instanceof BlockHitResult blockHit && hit.getType() != HitResult.Type.MISS) {
            setPos(blockHit.getLocation());
            detonate(null, Vec3.atLowerCornerOf(blockHit.getDirection().getNormal()));
            return;
        }
        if (hit instanceof EntityHitResult directHit) {
            setPos(directHit.getLocation());
            detonate(directHit.getEntity(), null);
            return;
        }
        move(MoverType.SELF, velocity);
        if (horizontalCollision || verticalCollision || onGround()) {
            detonate(null, collisionNormal());
            return;
        }
        spawnTracerFume();
        if (tickCount % 3 == 0 && level() instanceof ServerLevel server) {
            // The passing crack: pitched up so it reads as a shell rather than a launch.
            server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCKET_LAUNCH.get(),
                    SoundSource.PLAYERS, 0.5F, 1.8F);
        }
    }

    /**
     * Nearest collision along one shell segment, with blocks winning ties. The bounds are the
     * shell's AABB at the start of the segment. A prediction can pass no projectile entity.
     */
    public static HitResult trace(Level level, @Nullable Entity projectile,
                                  @Nullable Entity owner, @Nullable Entity ownerVehicle,
                                  AABB projectileBounds, Vec3 start, Vec3 end) {
        EntityHitResult directHit = ProjectileUtil.getEntityHitResult(level, projectile, start, end,
                projectileBounds.expandTowards(end.subtract(start)).inflate(1.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && candidate != projectile && candidate != owner
                        && candidate != ownerVehicle
                        && !(candidate instanceof MainGunShellEntity));
        BlockHitResult blockHit = level.clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
        boolean blockIsNearer = blockHit.getType() != HitResult.Type.MISS
                && (directHit == null
                        || start.distanceToSqr(blockHit.getLocation())
                                <= start.distanceToSqr(directHit.getLocation()));
        return blockIsNearer || directHit == null ? blockHit : directHit;
    }

    /** The player who fired this round, resolved from the stored UUID. */
    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }

    /**
     * The round's own wake: a thin, tight thread of fume instead of a fireball.
     *
     * <p>A flame blob travelling with the shell reads as a slow rocket, which is exactly what a
     * 120mm dart must not look like. The wake is emitted every other tick with almost no spread and
     * no drift, so it stays behind the round as a straight scratch in the air, and the bright
     * streak that carries the eye is drawn client side by the shell renderer - that way the read
     * does not depend on the server tick rate or on a packet arriving.
     */
    private void spawnTracerFume() {
        if (!(level() instanceof ServerLevel server) || tickCount % 2 != 0) {
            return;
        }
        double scale = LittleBirdConfig.mainGunParticleScale();
        server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(),
                count(2, scale), 0.02D, 0.02D, 0.02D, 0.0D);
    }

    /**
     * The muzzle flash: a bright core with flame and a smoke cloud at the barrel mouth, so the
     * player sees a big calibre round leave the gun instead of a bullet streak.
     */
    public static void muzzleFlash(ServerLevel server, Vec3 muzzle, double scale) {
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, muzzle.x, muzzle.y, muzzle.z, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z,
                count(18, scale), 0.35D, 0.35D, 0.35D, 0.02D);
        server.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z,
                count(10, scale), 0.4D, 0.4D, 0.4D, 0.01D);
    }

    /**
     * Resolves the direct hit and any HE fragments before the impact effects and terrain blast.
     * The effects also carry the camera shock to nearby clients.
     *
     * @param directHit the entity that was hit, or null for a block or a swept collision
     * @param normal    surface normal at the impact, or null for a hit with no face (an entity, or
     *                  a collision the sweep reported without a face)
     */
    private void detonate(Entity directHit, Vec3 normal) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            if (directHit != null && directHit != owner) {
                float damage = io.github.littlebird.entity.weapon.VehicleDamageTable.damage(
                        weapon, directHit);
                if (damage > 0.0F) {
                    // Credited to the gunner's player, the same way the rocket credits its pilot.
                    boolean applied = directHit.hurt(WarheadSplash.source(server, owner), damage);
                    if (applied && owner instanceof ServerPlayer serverPlayer) {
                        // The shell used to skip this, so a hit moved the target's bar and told
                        // the gunner nothing at all.
                        HitFeedback.report(serverPlayer, directHit);
                    }
                }
            }
            if (shellType == MainGunAmmoType.HE) {
                applyHeFragments(server, directHit, owner, normal);
            }
            // Shrapnel along the face that was hit, a dust column, the shock ring lying in the
            // impact plane, the crater and the layered report: one package, so a 120mm hit cannot
            // read as a firecracker. Entity damage always applies; terrain damage stays opt-in,
            // exactly like the rocket.
            MainGunImpactFx.detonate(server, this, position(), normal,
                    LittleBirdConfig.mainGunParticleScale(), splashRadius,
                    LittleBirdConfig.mainGunBreaksBlocks());
        }
        discard();
    }

    private void applyHeFragments(ServerLevel server, Entity directHit, Entity owner, Vec3 normal) {
        Vec3 impact = position();
        // Sample cover before the blast changes the terrain. Move the ray endpoint off the
        // impact face so the struck block only counts as cover from the opposite side.
        Vec3 fragmentOrigin = normal == null ? impact : impact.add(normal.scale(FRAGMENT_FACE_OFFSET));
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        MainBattleTankEntity firingTank = ownerVehicle instanceof MainBattleTankEntity tank ? tank : null;
        WarheadSplash.apply(server, this, directHit,
                new WarheadSplash.PlayerCredit(owner instanceof ServerPlayer player ? player : null),
                weapon, impact, LittleBirdConfig.tankMainGunHeFragmentRadius(),
                HE_FRAGMENT_DAMAGE_FRACTION, target -> target instanceof LivingEntity
                        && (firingTank == null || !firingTank.hasPassenger(target))
                        ? Explosion.getSeenPercent(fragmentOrigin, target) : 0.0D);
    }

    /**
     * The face a swept collision ran into, for the impact package. Minecraft reports the collision
     * as a flag, not as a plane, so it is rebuilt from the motion: a floor hit throws its ejecta
     * straight up, a wall hit throws it back along the wall's own outward normal.
     */
    private Vec3 collisionNormal() {
        if (onGround() || verticalCollision) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        Vec3 flat = new Vec3(-velocity.x, 0.0D, -velocity.z);
        return flat.lengthSqr() < 1.0E-6D ? null : flat.normalize();
    }

    private static int count(int base, double scale) {
        return Math.max(1, (int) Math.round(base * scale));
    }

    /**
     * Keeps the round drawn far past vanilla's own cull.
     *
     * <p>{@code Entity.shouldRenderAtSqrDistance} scales its limit by the entity's own bounding box -
     * 64 times the box's largest side - and this round is deliberately a 0.3 block dart, so the
     * vanilla rule stops drawing it about nineteen blocks out. That is exactly the distance at which
     * a tank crew wants to watch its tracer cross a valley, so the limit comes from the client config
     * instead and the world's render distance becomes the honest upper bound: nothing in an unloaded
     * chunk can be drawn at all, however far this allows.
     *
     * <p>Only the render dispatcher asks this, so reading a client config value here is safe on a
     * dedicated server, which never calls it.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double limit = LittleBirdConfig.mainGunShellVisibleDistanceBlocks();
        if (!Double.isFinite(limit) || limit <= 0.0D) {
            return super.shouldRenderAtSqrDistance(distance);
        }
        return distance < limit * limit;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private static Vector3 vector(Vec3 value) {
        return new Vector3(value.x, value.y, value.z);
    }

    private static Vec3 vec(Vector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }
}
