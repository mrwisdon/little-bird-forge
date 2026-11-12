package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.BombBallistics;
import io.github.littlebird.entity.weapon.BombKind;
import io.github.littlebird.entity.weapon.DamageType;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.registry.ModSounds;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
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
 * Incendiary canister: a light, unguided store that trades blast for a burning impact area.
 *
 * <p>It flies the same ballistics as the JDAM with no guidance at all - there is no seeker in it -
 * and on impact it leaves fire on the ground it hit and sets the remaining fire time of everything
 * in the blast, for {@code incendiary_fire_ticks}. Fire is only placed where it can actually burn:
 * on air above a solid surface, never in water and never under rain, because a canister that lights
 * a fire in a lake reads as a bug rather than as a weapon.
 */
public class IncendiaryBombEntity extends Entity {
    private Vec3 velocity = Vec3.ZERO;
    private UUID ownerUuid;
    private boolean guided;
    /** Which release this round belongs to, for the crew's kill feed. */
    private int releaseId;
    private boolean hasTarget;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int ageTicks;
    private int releaseEntityId = -1;
    private int releaseVehicleId = -1;

    public IncendiaryBombEntity(EntityType<? extends IncendiaryBombEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
    }

    /**
     * Hands the canister to the world: where it starts, how fast the aircraft was already going
     * (blocks per tick) and whether the crew left the fuse guided.
     */
    public void launchFrom(Entity shooter, double x, double y, double z,
                           double vx, double vy, double vz, boolean guided, int releaseId) {
        this.ownerUuid = shooter == null ? null : shooter.getUUID();
        this.releaseEntityId = shooter == null ? -1 : shooter.getId();
        Entity vehicle = shooter == null ? null : shooter.getVehicle();
        this.releaseVehicleId = vehicle == null ? -1 : vehicle.getId();
        this.guided = guided;
        this.releaseId = releaseId;
        setPos(x, y, z);
        this.velocity = new Vec3(vx, vy, vz);
        setDeltaMovement(this.velocity);
        faceVelocity();
    }

    /** Accepted for the shared release path, but a canister has no seeker to give it to. */
    public void setTarget(double x, double y, double z) {
        this.hasTarget = true;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public void clearTarget() {
        this.hasTarget = false;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    /** Always false: {@link BombKind#INCENDIARY} carries no guidance, designation or not. */
    private boolean steering() {
        return guided && BombKind.INCENDIARY.guided() && hasTarget;
    }

    private double[] targetOrNull() {
        return steering() ? new double[]{targetX, targetY, targetZ} : null;
    }

    private void faceVelocity() {
        if (velocity.lengthSqr() < 1.0E-9D) {
            return;
        }
        Vec3 unit = velocity.normalize();
        setYRot((float) Math.toDegrees(Math.atan2(-unit.x, unit.z)));
        setXRot((float) Math.toDegrees(-Math.asin(Mth.clamp(unit.y, -1.0D, 1.0D))));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ageTicks++;
        Vec3 start = position();
        double[] state = BombBallistics.step(
                new double[]{start.x, start.y, start.z},
                new double[]{velocity.x, velocity.y, velocity.z},
                BombKind.INCENDIARY, targetOrNull(), steering());
        velocity = new Vec3(state[3], state[4], state[5]);
        setDeltaMovement(velocity);
        Vec3 end = start.add(velocity);

        EntityHitResult directHit = findEntityHit(start, end);
        BlockHitResult blockHit = level().clip(new ClipContext(
                start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        boolean blockFirst = blockHit.getType() != HitResult.Type.MISS
                && (directHit == null
                        || start.distanceToSqr(blockHit.getLocation())
                                <= start.distanceToSqr(directHit.getLocation()));
        if (blockFirst) {
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
        if (horizontalCollision || verticalCollision || onGround()
                || BombBallistics.shouldDetonate(getY(), onGround())
                || ageTicks >= BombBallistics.EXPIRY_TICKS) {
            detonate(null);
        }
    }

    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        return ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator() && candidate != this
                        && candidate.getId() != releaseEntityId
                        && candidate.getId() != releaseVehicleId
                        && !(candidate instanceof IncendiaryBombEntity)
                        && !(candidate instanceof JdamBombEntity));
    }

    /** The pilot who released it, resolved from the stored UUID. */
    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }

    /** Small warhead, then the fire: the burn is what this store is for. */
    private void detonate(Entity directHit) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            net.minecraft.server.level.ServerPlayer credit =
                    io.github.littlebird.entity.AirframeTargets.creditPlayer(owner);
            if (directHit != null) {
                float damage = LittleBirdBallistics.damageTo(DamageType.SHELL,
                        AirframeTargets.armorClass(directHit),
                        LittleBirdBallistics.WeaponDamage.incendiary(
                                (float) LittleBirdConfig.incendiaryDamage()));
                boolean applied = directHit.hurt(
                        io.github.littlebird.entity.weapon.WarheadSplash.source(server, owner), damage);
                if (applied && credit != null) {
                    HitFeedback.report(credit, directHit);
                }
            }
            float blastRadius = (float) LittleBirdConfig.incendiaryBlastRadius();
            // Sample the neighbourhood before the blast so the dead can be named afterwards.
            java.util.List<Entity> around = blastRadius > 0.0F
                    ? server.getEntities(this, getBoundingBox().inflate(blastRadius))
                    : java.util.List.of();
            server.explode(this, getX(), getY(), getZ(), blastRadius, false,
                    Level.ExplosionInteraction.NONE);
            java.util.List<String> killed =
                    io.github.littlebird.entity.AirframeTargets.kills(owner, directHit, around);
            if (credit != null && !killed.isEmpty()) {
                io.github.littlebird.network.ModNetwork.CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> credit),
                        new io.github.littlebird.network.B2BombKillPacket(releaseId,
                                io.github.littlebird.entity.B2Entity.WEAPON_INCENDIARY, killed));
            }
            ignite(server);
            server.playSound(null, getX(), getY(), getZ(), ModSounds.EXPLOSION.get(),
                    SoundSource.PLAYERS, 1.1F, 1.15F);
        }
        discard();
    }

    /**
     * Lights the impact area: fire on the terrain surface under the canister, the remaining burn
     * time of everything still standing in the fire radius, and an {@link IncendiaryBurnZones} zone
     * that keeps the ground alight and charges a per-second burn. The burning ground follows
     * {@code incendiary_fire_radius} rather than the warhead, so the two can be tuned apart.
     */
    private void ignite(ServerLevel server) {
        int fireTicks = LittleBirdConfig.incendiaryFireTicks();
        if (fireTicks <= 0) {
            return;
        }
        double radius = Math.max(LittleBirdConfig.incendiaryBlastRadius(),
                LittleBirdConfig.incendiaryFireRadius());
        IncendiaryBurnZones.light(server, getX(), getY(), getZ(), radius, fireTicks);
        AABB area = new AABB(getX() - radius, getY() - radius, getZ() - radius,
                getX() + radius, getY() + radius, getZ() + radius);
        for (Entity entity : server.getEntities(this, area)) {
            if (entity.fireImmune()) {
                continue;
            }
            entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), fireTicks));
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        velocity = new Vec3(tag.getDouble("VX"), tag.getDouble("VY"), tag.getDouble("VZ"));
        ageTicks = tag.getInt("Age");
        guided = tag.getBoolean("Guided");
        hasTarget = tag.getBoolean("HasTarget");
        targetX = tag.getDouble("TargetX");
        targetY = tag.getDouble("TargetY");
        targetZ = tag.getDouble("TargetZ");
        releaseEntityId = tag.getInt("ReleaseEntity");
        releaseVehicleId = tag.getInt("ReleaseVehicle");
        if (tag.hasUUID("Owner")) {
            ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("VX", velocity.x);
        tag.putDouble("VY", velocity.y);
        tag.putDouble("VZ", velocity.z);
        tag.putInt("Age", ageTicks);
        tag.putBoolean("Guided", guided);
        tag.putBoolean("HasTarget", hasTarget);
        tag.putDouble("TargetX", targetX);
        tag.putDouble("TargetY", targetY);
        tag.putDouble("TargetZ", targetZ);
        tag.putInt("ReleaseEntity", releaseEntityId);
        tag.putInt("ReleaseVehicle", releaseVehicleId);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
