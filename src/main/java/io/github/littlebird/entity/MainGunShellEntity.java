package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import io.github.littlebird.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
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
 * not drawn as a model - the fireball is, so the muzzle flash reads as a shell leaving the barrel
 * rather than as a bullet streak. On impact it damages what it hit, throws dust and, when the
 * server allows it, breaks the blocks it lands on.
 */
public class MainGunShellEntity extends Entity {
    /** The impact boom is deeper than a rocket's. */
    private static final float IMPACT_VOLUME = 1.6F;
    private static final float IMPACT_PITCH = 0.7F;

    private Vec3 velocity = Vec3.ZERO;
    private Vec3 heading = new Vec3(0.0D, 0.0D, 1.0D);
    private UUID ownerUuid;
    private MainGunAmmoType shellType = MainGunAmmoType.AP;
    private float directDamage;
    private float armoredDamage;
    private double splashRadius = 3.0D;

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
    public void configure(MainGunAmmoType type, float direct, float armored, double splash) {
        this.shellType = type == null ? MainGunAmmoType.AP : type;
        this.directDamage = direct;
        this.armoredDamage = armored;
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
        EntityHitResult directHit = findEntityHit(start, end);
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockIsNearer = blockHit.getType() != HitResult.Type.MISS
                && (directHit == null
                        || start.distanceToSqr(blockHit.getLocation())
                                <= start.distanceToSqr(directHit.getLocation()));
        if (blockIsNearer) {
            setPos(blockHit.getLocation());
            detonate(null);
            return;
        }
        if (directHit != null) {
            setPos(directHit.getLocation());
            detonate(directHit.getEntity());
            return;
        }
        move(MoverType.SELF, velocity);
        if (horizontalCollision || verticalCollision || onGround()) {
            detonate(null);
            return;
        }
        spawnFireball();
        if (tickCount % 3 == 0 && level() instanceof ServerLevel server) {
            // The passing crack: pitched up so it reads as a shell rather than a launch.
            server.playSound(null, getX(), getY(), getZ(), ModSounds.ROCKET_LAUNCH.get(),
                    SoundSource.PLAYERS, 0.5F, 1.8F);
        }
    }

    /** Nearest live entity along this tick's flight segment, never the shooter or their hull. */
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        Entity owner = owner();
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        return ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && candidate != this && candidate != owner
                        && candidate != ownerVehicle
                        && !(candidate instanceof MainGunShellEntity));
    }

    /** The player who fired this round, resolved from the stored UUID. */
    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }

    /** The moving fireball: flame core plus a smoke sleeve. */
    private void spawnFireball() {
        if (level() instanceof ServerLevel server) {
            double scale = LittleBirdConfig.mainGunParticleScale();
            server.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(),
                    count(4, scale), 0.08D, 0.08D, 0.08D, 0.01D);
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY(), getZ(),
                    count(3, scale), 0.12D, 0.12D, 0.12D, 0.01D);
        }
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

    /** Direct hit, dust, terrain damage when the server allows it, and the boom. */
    private void detonate(Entity directHit) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            if (directHit != null && directHit != owner) {
                boolean armored = GroundWeapons.isArmored(directHit);
                float damage = LittleBirdBallistics.damageTo(armored, directDamage, armoredDamage);
                if (damage > 0.0F) {
                    // Credited to the gunner's player, the same way the rocket credits its pilot.
                    directHit.hurt(server.damageSources().explosion(this, owner), damage);
                }
            }
            double scale = LittleBirdConfig.mainGunParticleScale();
            server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
            server.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(),
                    count(6, scale), 0.6D, 0.6D, 0.6D, 0.02D);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(),
                    count(24, scale), 1.4D, 0.7D, 1.4D, 0.03D);
            // Entity damage always applies; terrain damage is opt-in, exactly like the rocket.
            Level.ExplosionInteraction interaction = LittleBirdConfig.mainGunBreaksBlocks()
                    ? Level.ExplosionInteraction.TNT
                    : Level.ExplosionInteraction.NONE;
            server.explode(this, getX(), getY(), getZ(), (float) splashRadius, false, interaction);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.EXPLOSION.get(),
                    SoundSource.PLAYERS, IMPACT_VOLUME, IMPACT_PITCH);
        }
        discard();
    }

    private static int count(int base, double scale) {
        return Math.max(1, (int) Math.round(base * scale));
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
