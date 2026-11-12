package io.github.littlebird.entity;

import io.github.littlebird.entity.weapon.CraterShape;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraft.world.damagesource.DamageSource;
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
    /** The ground vehicle terminal homing is locked on this tick, or -1. */
    private int lockedEntityId = -1;

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

    /**
     * The ground vehicle this round is homing on, or null when none is close enough. Terminal
     * homing is automatic: the JDAM does not need the crew's fuse or a pod designation to attack
     * the last hull it falls past, and it never homes on the aircraft that released it.
     */
    private Entity terminalLock() {
        double radius = LittleBirdConfig.jdamTerminalLockRadius();
        if (radius <= 0.0D) {
            return null;
        }
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        var near = level().getEntities(this, getBoundingBox().inflate(radius),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && candidate.getId() != releaseEntityId
                        && candidate.getId() != releaseVehicleId
                        && AirframeTargets.isGroundVehicle(candidate));
        for (Entity candidate : near) {
            double distance = candidate.distanceToSqr(this);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    /**
     * Where this tick steers. Terminal homing outranks the pod designation, because it is the
     * round's last chance to correct; the designation keeps working when no hull is in reach.
     */
    private double[] aimPoint() {
        Entity lock = terminalLock();
        if (lock != null) {
            lockedEntityId = lock.getId();
            return BombBallistics.leadAimPoint(
                    new double[]{getX(), getY(), getZ()},
                    new double[]{velocity.x, velocity.y, velocity.z},
                    new double[]{lock.getX(), lock.getY() + lock.getBbHeight() * 0.5D, lock.getZ()},
                    new double[]{lock.getDeltaMovement().x, lock.getDeltaMovement().y,
                            lock.getDeltaMovement().z});
        }
        lockedEntityId = -1;
        return guided && hasTarget ? new double[]{targetX, targetY, targetZ} : null;
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
        double[] aim = aimPoint();
        double authority = lockedEntityId >= 0
                ? BombBallistics.TERMINAL_GUIDANCE_AUTHORITY
                : BombBallistics.GUIDANCE_AUTHORITY;
        double[] state = BombBallistics.step(
                new double[]{start.x, start.y, start.z},
                new double[]{velocity.x, velocity.y, velocity.z},
                BombKind.JDAM, aim, aim != null, authority);
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

    /**
     * True when a damage source is one of the B-2 warheads' own blasts - the JDAM's or the
     * incendiary's. A ground vehicle ignores both: their anti-vehicle damage is settled by the
     * priced direct hit each bomb applies itself, and the incendiary is not allowed to mark a hull
     * at all. Letting the vanilla explosion pile 43-71 raw damage on top would make those figures
     * meaningless. Blasts from anything else - TNT, shells, rockets - are untouched.
     */
    public static boolean isWarheadBlast(DamageSource source) {
        if (source == null) {
            return false;
        }
        return source.getDirectEntity() instanceof JdamBombEntity
                || source.getDirectEntity() instanceof IncendiaryBombEntity;
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
                        io.github.littlebird.entity.weapon.VehicleDamageTable.row(
                                io.github.littlebird.entity.weapon.VehicleDamageTable.Weapon.JDAM)
                                .prices());
                // A null direct entity is deliberate: a hull ignores the warhead's own blast (see
                // isWarheadBlast), so the priced hit must not look like that blast.
                boolean applied = directHit.hurt(
                        io.github.littlebird.entity.weapon.WarheadSplash.source(server, owner), damage);
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
            // Entity damage above, terrain damage below: two knobs, two balances.
            carveCrater(server);
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

    /**
     * Tears the crater. The vanilla explosion above is entity damage only (interaction NONE), so
     * the hole is carved here instead: that keeps the terrain radius a separate knob from the kill
     * radius, and a huge hole can never widen the blast that already hurt the crew of the aircraft
     * that dropped the bomb. Unbreakable and very tough material is left standing, and the whole
     * pass is capped so a carpet of sixteen bombs cannot stall the server.
     */
    private void carveCrater(ServerLevel server) {
        if (!LittleBirdConfig.jdamBreaksBlocks()) {
            return;
        }
        double radius = LittleBirdConfig.jdamCraterRadius();
        if (radius <= 0.0D) {
            return;
        }
        int reach = Mth.ceil(radius);
        BlockPos centre = blockPosition();
        int broken = 0;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (broken >= CraterShape.MAX_BLOCKS) {
                        return;
                    }
                    if (!CraterShape.isCracked(dx, dy, dz, radius)) {
                        continue;
                    }
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (!server.isLoaded(pos)) {
                        continue;
                    }
                    BlockState state = server.getBlockState(pos);
                    if (state.isAir()
                            || !CraterShape.isBreakable(state.getBlock().getExplosionResistance(),
                                    CraterShape.MAX_RESISTANCE)) {
                        continue;
                    }
                    server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    broken++;
                }
            }
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
