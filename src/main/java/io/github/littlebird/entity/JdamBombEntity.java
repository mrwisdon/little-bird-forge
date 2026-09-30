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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * GBU-31 JDAM: the B-2's heavy store. Gravity does the work; a pod designation merely bends the
 * fall onto the spot the operator picked, and only within a bomb's turn authority.
 *
 * <p>Server authoritative, and the flight lives in {@link BombBallistics} so it can be unit tested.
 * The bomb keeps the velocity the aircraft handed it at release (a bomb released at 300 knots
 * carries that speed into the fall), which is why the release point matters and why the HUD wants
 * the lead distance.
 *
 * <p>With no designation - or with the fuse set ballistic - this is a dumb bomb: the same code path
 * simply never steers. It cannot fuse on the aircraft that dropped it and it cannot fuse on another
 * bomb.
 */
public class JdamBombEntity extends Entity {
    private Vec3 velocity = Vec3.ZERO;
    private UUID ownerUuid;
    /** The crew asked for guidance; the round can only steer when it also has a designation. */
    private boolean guided;
    /** Which release this round belongs to, for the crew's kill feed. */
    private int releaseId;
    private boolean hasTarget;
    private double targetX;
    private double targetY;
    private double targetZ;
    private int ageTicks;
    /** Who let go of it, so it cannot detonate on its own aircraft or its own pilot. */
    private int releaseEntityId = -1;
    private int releaseVehicleId = -1;

    public JdamBombEntity(EntityType<? extends JdamBombEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
    }

    /**
     * Hands the bomb to the world: where it starts, how fast the aircraft was already going
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

    /** The spot the pod designated. Without a call the round stays ballistic. */
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

    /** True when this tick actually steers: the fuse is guided and there is somewhere to aim. */
    private boolean steering() {
        return guided && BombKind.JDAM.guided() && hasTarget;
    }

    private double[] targetOrNull() {
        return steering() ? new double[]{targetX, targetY, targetZ} : null;
    }

    /** Points the model down the launch velocity, so the fins trail the fall. */
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
                BombKind.JDAM, targetOrNull(), steering());
        velocity = new Vec3(state[3], state[4], state[5]);
        setDeltaMovement(velocity);
        // The step's position output is the same segment, but the move below has to do the
        // travelling: it is what resolves the collision and sets onGround.
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

    /** Nearest entity along this tick's fall, ignoring the crew that released it. */
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        return ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator() && candidate != this
                        && candidate.getId() != releaseEntityId
                        && candidate.getId() != releaseVehicleId
                        && !(candidate instanceof JdamBombEntity)
                        && !(candidate instanceof IncendiaryBombEntity));
    }

    /** The pilot who released it, resolved from the stored UUID. */
    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }

    /** Direct hit damage, then the 2000 lb warhead, then the report back to the crew. */
    private void detonate(Entity directHit) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            net.minecraft.server.level.ServerPlayer credit =
                    io.github.littlebird.entity.AirframeTargets.creditPlayer(owner);
            if (directHit != null) {
                float damage = LittleBirdBallistics.damageTo(DamageType.SHELL,
                        AirframeTargets.armorClass(directHit),
                        LittleBirdBallistics.WeaponDamage.flat(
                                (float) LittleBirdConfig.jdamDamage()));
                boolean applied = directHit.hurt(
                        server.damageSources().explosion(this, owner), damage);
                if (applied && credit != null) {
                    HitFeedback.report(credit, directHit);
                }
            }
            float blastRadius = (float) LittleBirdConfig.jdamBlastRadius();
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
                                io.github.littlebird.entity.B2Entity.WEAPON_JDAM, killed));
            }
            server.playSound(null, getX(), getY(), getZ(), ModSounds.EXPLOSION.get(),
                    SoundSource.PLAYERS, 1.4F, 0.9F);
        }
        discard();
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
