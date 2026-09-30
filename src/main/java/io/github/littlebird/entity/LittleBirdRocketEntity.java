package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.LittleBirdRocketBallistics;
import io.github.littlebird.network.ModNetwork;
import java.util.UUID;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * 70mm rocket. Server authoritative: it is spawned, stepped and exploded on the
 * server and ordinary entity tracking carries its position to clients.
 *
 * The trajectory itself lives in LittleBirdRocketBallistics so the client side
 * aim predictor runs exactly the same maths.
 */
public class LittleBirdRocketEntity extends Entity {
    public static final double INITIAL_SPEED = LittleBirdRocketBallistics.INITIAL_SPEED;
    public static final int LIFETIME_TICKS = LittleBirdRocketBallistics.LIFETIME_TICKS;
    public static final float EXPLOSION_RADIUS = 3.0F;

    protected Vec3 velocity = Vec3.ZERO;
    protected Vec3 heading = new Vec3(0.0D, 0.0D, 1.0D);
    private UUID ownerUuid;
    /**
     * Multiplier on both damage prices. The Little Bird's pods carry 14 rounds, the Apache's 38,
     * so the same rocket entity is priced per launcher instead of per rocket type.
     */
    private float damageScale = 1.0F;

    public LittleBirdRocketEntity(EntityType<? extends LittleBirdRocketEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
    }

    /** Scales the direct hit and airframe price; 1.0 keeps the Little Bird's full 70mm. */
    public void setDamageScale(float scale) {
        this.damageScale = Float.isFinite(scale) && scale > 0.0F ? scale : 1.0F;
    }

    /** Points and launches the rocket along the given unit direction. */
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
        if (tickCount > LIFETIME_TICKS) {
            discard();
            return;
        }
        Vec3 start = position();
        velocity = steer(start);
        setDeltaMovement(velocity);
        Vec3 end = start.add(velocity);

        // Entity sweep over the whole step, so a fast rocket cannot tunnel past a
        // target at six blocks per tick. The block hit is compared in and whichever
        // is nearer wins, so a thin wall still stops the rocket.
        EntityHitResult directHit = findEntityHit(start, end);
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockIsNearer = blockHit.getType() != HitResult.Type.MISS
                && (directHit == null
                        || start.distanceToSqr(blockHit.getLocation())
                                <= start.distanceToSqr(directHit.getLocation()));
        if (blockIsNearer) {
            setPos(blockHit.getLocation());
            explode(null);
            return;
        }
        if (directHit != null) {
            setPos(directHit.getLocation());
            explode(directHit.getEntity());
            return;
        }

        move(MoverType.SELF, velocity);
        if (horizontalCollision || verticalCollision || onGround()) {
            explode(null);
            return;
        }
        if (tickCount % 2 == 0) {
            spawnTrail();
        }
    }

    /**
     * The velocity this tick's flight uses. The unguided 70mm follows its own drag model; a guided
     * subclass overrides this to steer while keeping the same hit, trail and blast plumbing.
     */
    protected Vec3 steer(Vec3 start) {
        return vec(LittleBirdRocketBallistics.advance(vector(velocity), vector(heading)));
    }

    /** Nearest living entity along this tick's flight segment. */
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        Entity owner = owner();
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        return ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && candidate != this && candidate != owner
                        && candidate != ownerVehicle
                        && !(candidate instanceof LittleBirdRocketEntity));
    }

    /** The pilot who fired this rocket, resolved from the stored UUID. */
    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }
    private void spawnTrail() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** Direct hit damage plus the warhead, credited to the pilot who fired it. */
    protected void explode(Entity directHit) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            if (directHit != null) {
                // The direct hit is credited to the pilot rather than to this rocket, which is
                // also how an airframe tells a real hit from this warhead's own blast.
                LittleBirdBallistics.Airframe airframe = AirframeTargets.classify(directHit);
                // Rockets are an anti-personnel round: against a tracked or wheeled hull they only
            // scratch, and the launcher's damage scale does not change that.
            float damage = io.github.littlebird.entity.AirframeTargets.armorClass(directHit)
                    == io.github.littlebird.entity.weapon.ArmorClass.HEAVY_ARMOR
                            ? LittleBirdConfig.rocketHeavyDamage() : rocketDamageTo(airframe);
                boolean applied = directHit.hurt(
                        server.damageSources().explosion(owner, owner), damage);
                if (applied && owner instanceof ServerPlayer serverPlayer) {
                    HitFeedback.report(serverPlayer, directHit);
                }
            }
            // Entity damage always applies; terrain damage is opt-in through config.
            Level.ExplosionInteraction interaction = LittleBirdConfig.rocketsBreakBlocks()
                    ? Level.ExplosionInteraction.TNT
                    : Level.ExplosionInteraction.NONE;
            server.explode(this, getX(), getY(), getZ(), EXPLOSION_RADIUS, false, interaction);
            server.playSound(null, getX(), getY(), getZ(),
                    io.github.littlebird.registry.ModSounds.EXPLOSION.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.3F, 0.9F);
        }
        discard();
    }

    /**
     * Damage a direct hit deals. An Apache is billed the flat airframe price whichever launcher
     * fired the rocket - the Apache's own pods carry a per launcher scale, and that price must
     * not change what a hit on a hull is worth - while a Little Bird keeps the scaled price.
     */
    protected float rocketDamageTo(LittleBirdBallistics.Airframe airframe) {
        if (airframe == LittleBirdBallistics.Airframe.APACHE) {
            return LittleBirdConfig.rocketDamageApache();
        }
        return LittleBirdBallistics.damageTo(
                airframe != LittleBirdBallistics.Airframe.NONE,
                LittleBirdConfig.rocketDirectHitDamage() * damageScale,
                LittleBirdConfig.rocketDamageAircraft() * damageScale);
    }

    private static LittleBirdBallistics.Vector3 vector(Vec3 value) {
        return new LittleBirdBallistics.Vector3(value.x, value.y, value.z);
    }

    private static Vec3 vec(LittleBirdBallistics.Vector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("VX")) {
            velocity = new Vec3(tag.getDouble("VX"), tag.getDouble("VY"), tag.getDouble("VZ"));
        }
        if (tag.contains("HX")) {
            heading = new Vec3(tag.getDouble("HX"), tag.getDouble("HY"), tag.getDouble("HZ"));
        }
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        setDamageScale(tag.contains("DamageScale") ? tag.getFloat("DamageScale") : 1.0F);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("VX", velocity.x);
        tag.putDouble("VY", velocity.y);
        tag.putDouble("VZ", velocity.z);
        tag.putDouble("HX", heading.x);
        tag.putDouble("HY", heading.y);
        tag.putDouble("HZ", heading.z);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        tag.putFloat("DamageScale", damageScale);
        }
    }
}