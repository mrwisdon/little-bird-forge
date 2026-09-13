package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.ApacheMissileBallistics;
import io.github.littlebird.entity.weapon.ApacheMissileTrail;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import net.minecraft.core.particles.ParticleOptions;
import io.github.littlebird.network.ModNetwork;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Wire guided missile: the operator keeps the sight on the target and the missile is flown onto
 * the spot the sight line ends at.
 *
 * Server authoritative. The flight itself lives in ApacheMissileBallistics so it can be unit
 * tested; this class feeds it the operator's current sight line and the depth of the aim point.
 * The aim point is re-raycast every tick at that same depth, so the missile is always flying at
 * where the crosshair is now, and it detonates when it arrives there: a TOW is aimed at a spot,
 * not fired down a line and left to find something. Cutting the sight away therefore drags the
 * missile off target, which is the weapon working, not a bug.
 */
public class ApacheMissileEntity extends Entity {
    private Vec3 velocity = Vec3.ZERO;
    private UUID ownerUuid;
    private int ageTicks;
    /**
     * How far along the sight line the aim point sits. Fixed at launch: the first block the
     * sight line hit, or the sight range over open ground and sky. Keeping the depth fixed is
     * what lets the aim point slide sideways with the operator's sight while the missile is on
     * its way, instead of burying itself in whatever it happens to be passing.
     */
    private double sightDistance;

    public ApacheMissileEntity(EntityType<? extends ApacheMissileEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
    }

    /**
     * How deep the operator's aim point is, measured once at launch: the first block the sight
     * line meets, or the sight range when it meets nothing. Raycasting is the one part of the
     * wire guidance that needs a level, so it lives here and the geometry stays in the pure
     * rules class.
     */
    public static double aimDepthBlocks(net.minecraft.world.level.Level level, Vec3 muzzle,
                                        Vec3 sightLine, Entity shooter) {
        Vec3 sight = sightLine.normalize();
        Vec3 end = muzzle.add(sight.scale(ApacheMissileBallistics.SIGHT_RANGE_BLOCKS));
        BlockHitResult hit = level.clip(new ClipContext(
                muzzle, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (hit.getType() != HitResult.Type.MISS) {
            double distance = muzzle.distanceTo(hit.getLocation());
            if (distance > 0.0D) {
                return Math.min(distance, ApacheMissileBallistics.SIGHT_RANGE_BLOCKS);
            }
        }
        return ApacheMissileBallistics.SIGHT_RANGE_BLOCKS;
    }

    /** Points and launches the missile down the given unit direction. */
    public void launch(Vec3 sightLine, Entity owner) {
        launch(sightLine, owner, ApacheMissileBallistics.SIGHT_RANGE_BLOCKS);
    }

    /** Points and launches the missile, fixing how deep the operator's aim point is. */
    public void launch(Vec3 sightLine, Entity owner, double aimDepthBlocks) {
        this.ownerUuid = owner == null ? null : owner.getUUID();
        Vec3 unit = sightLine.normalize();
        this.velocity = vec(ApacheMissileBallistics.launchVelocity(vector(unit)));
        this.sightDistance = Double.isFinite(aimDepthBlocks) && aimDepthBlocks > 0.0D
                ? aimDepthBlocks : ApacheMissileBallistics.SIGHT_RANGE_BLOCKS;
        setYRot((float) Math.toDegrees(Math.atan2(-unit.x, unit.z)));
        setXRot((float) Math.toDegrees(-Math.asin(unit.y)));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        ageTicks++;
        Vec3 sight = sightLine();
        Vec3 aimPoint = aimPoint(sight);
        velocity = vec(ApacheMissileBallistics.guide(
                vector(velocity), vector(sight), ageTicks));
        setDeltaMovement(velocity);
        Vec3 start = position();
        Vec3 end = start.add(velocity);

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
        // Arrived on the spot the operator picked: this is the TOW's fusing, and it is what
        // makes the sight a real aiming device rather than decoration.
        if (ApacheMissileBallistics.reachedAimPoint(vector(position()), vector(aimPoint),
                ApacheMissileBallistics.REACH_RADIUS_BLOCKS)) {
            explode(null);
            return;
        }
        if (horizontalCollision || verticalCollision || onGround()) {
            explode(null);
            return;
        }
        if (ApacheMissileBallistics.expired(ageTicks)) {
            explode(null);
            return;
        }
        if (tickCount % ApacheMissileTrail.TRAIL_INTERVAL_TICKS == 0) {
            spawnTrail();
        }
    }

    /** The operator's sight line, or straight ahead with nobody left to guide it. */
    private Vec3 sightLine() {
        Entity owner = owner();
        Entity vehicle = owner == null ? null : owner.getVehicle();
        if (vehicle instanceof ApacheEntity apache) {
            return Vec3.directionFromRotation(apache.getXRot(), apache.getYRot());
        }
        return velocity.length() < 1.0E-9D
                ? new Vec3(0.0D, 0.0D, 1.0D) : velocity.normalize();
    }

    /**
     * Where the sight line ends this tick, raycast from the operator's eye at the depth fixed at
     * launch. The origin has to be the eye rather than the missile: a raycast from the missile
     * itself would shorten as the missile sank towards the ground, walking the aim point in
     * towards the missile until it fused on its own tail. From the eye the aim point only ever
     * moves because the operator moved the sight.
     */
    private Vec3 aimPoint(Vec3 sight) {
        Vec3 eye = seekPoint();
        Vec3 end = eye.add(sight.scale(sightDistance));
        BlockHitResult hit = level().clip(new ClipContext(
                eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() != HitResult.Type.MISS
                && eye.distanceToSqr(hit.getLocation()) <= sightDistance * sightDistance) {
            return hit.getLocation();
        }
        return end;
    }

    /** The operator's eye while they are aboard; the missile itself once they are not. */
    private Vec3 seekPoint() {
        Entity owner = owner();
        Entity vehicle = owner == null ? null : owner.getVehicle();
        return vehicle instanceof ApacheEntity ? owner.getEyePosition() : position();
    }

    /** Nearest entity along this tick's flight segment, ignoring the crew that fired it. */
    private EntityHitResult findEntityHit(Vec3 start, Vec3 end) {
        Entity owner = owner();
        Entity ownerVehicle = owner == null ? null : owner.getVehicle();
        return ProjectileUtil.getEntityHitResult(level(), this, start, end,
                getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D),
                candidate -> candidate.isAlive() && !candidate.isSpectator()
                        && candidate != this && candidate != owner
                        && candidate != ownerVehicle
                        && !(candidate instanceof ApacheMissileEntity)
                        && !(candidate instanceof LittleBirdRocketEntity));
    }

    /** The pilot who fired this missile, resolved from the stored UUID. */
    public Entity owner() {
        if (ownerUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        return server.getPlayerByUUID(ownerUuid);
    }

    /**
     * The TOW's signature: a bright nozzle flame and two parallel smoke tails streaming back
     * along this tick's flight. Sent per player instead of through the bulk broadcast, because
     * that one stops at 32 blocks - the trail used to vanish while the missile was still being
     * tracked, and a guided missile you cannot follow is a missile you cannot guide.
     */
    private void spawnTrail() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        double distance = velocity.length();
        Vector3 forward = distance < 1.0E-9D
                ? new Vector3(0.0D, 0.0D, 1.0D) : vector(velocity);
        Vector3 end = vector(position());
        // Cover every tick since the last emission, not just this one.
        Vector3 start = distance < 1.0E-9D ? end : end.subtract(forward.normalize()
                .scale(distance * ApacheMissileTrail.TRAIL_INTERVAL_TICKS));
        Vector3 middle = start.add(end).scale(0.5D);
        Vector3 half = end.subtract(start).scale(0.5D);
        Vector3 right = ApacheMissileTrail.rightOf(forward);
        double range = ApacheMissileTrail.particleRange(
                LittleBirdConfig.apacheMissileParticleRange());

        for (Vector3 anchor : ApacheMissileTrail.tailAnchors(middle, right)) {
            broadcastParticles(server, ParticleTypes.CAMPFIRE_COSY_SMOKE, anchor, half, 3, range);
        }
        broadcastParticles(server, ParticleTypes.SMOKE, ApacheMissileTrail.nozzle(end, forward),
                half.scale(0.25D), 1, range);
        broadcastParticles(server, ParticleTypes.FLAME, ApacheMissileTrail.nozzle(end, forward),
                new Vector3(0.02D, 0.02D, 0.02D), 2, range);
    }

    /**
     * One particle packet, to every player inside the configured range. The bulk
     * ServerLevel#sendParticles is only delivered within 32 blocks, which is shorter than this
     * missile's flight, so the range is checked here and the per player call used instead.
     */
    private void broadcastParticles(ServerLevel server, ParticleOptions options, Vector3 at,
                                    Vector3 spread, int count, double range) {
        double rangeSqr = range * range;
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(at.x(), at.y(), at.z()) > rangeSqr) {
                continue;
            }
            server.sendParticles(player, options, true, at.x(), at.y(), at.z(), count,
                    spread.x(), spread.y(), spread.z(), 0.0D);
        }
    }

    /** Direct hit damage plus the warhead, credited to the pilot who fired it. */
    private void explode(Entity directHit) {
        if (level() instanceof ServerLevel server) {
            Entity owner = owner();
            if (directHit != null) {
                LittleBirdBallistics.Airframe airframe = AirframeTargets.classify(directHit);
                float damage = damageFor(directHit, airframe);
                boolean applied = directHit.hurt(
                        server.damageSources().explosion(owner, owner), damage);
                if (applied && owner instanceof ServerPlayer serverPlayer) {
                    ModNetwork.sendHitFeedback(serverPlayer, !directHit.isAlive(),
                            airframe != LittleBirdBallistics.Airframe.NONE,
                            LittleBirdEntity.healthFractionOf(directHit));
                }
            }
            server.explode(this, getX(), getY(), getZ(),
                    LittleBirdConfig.apacheMissileSplashRadius(), false,
                    Level.ExplosionInteraction.NONE);
            server.playSound(null, getX(), getY(), getZ(),
                    io.github.littlebird.registry.ModSounds.EXPLOSION.get(),
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2F, 1.0F);
        }
        discard();
    }

    /**
     * Damage this hit deals. A direct hit on a rotorcraft destroys it outright - the missile
     * takes the rotor off, so whatever is left on the health bar is irrelevant - while
     * everything else takes the ordinary warhead.
     */
    private float damageFor(Entity target, LittleBirdBallistics.Airframe airframe) {
        if (airframe != LittleBirdBallistics.Airframe.NONE
                && LittleBirdConfig.apacheMissileDirectHitKills()) {
            return ApacheMissileBallistics.directHitDamage(healthOf(target));
        }
        return LittleBirdBallistics.damageTo(
                airframe != LittleBirdBallistics.Airframe.NONE,
                LittleBirdConfig.apacheMissileDamage(),
                LittleBirdConfig.apacheMissileAirframeDamage());
    }

    /** Remaining airframe health, so the killing blow clears it on any aircraft. */
    private static float healthOf(Entity target) {
        if (target instanceof ApacheEntity apache) {
            return apache.getHealth();
        }
        if (target instanceof LittleBirdEntity bird) {
            return bird.getHealth();
        }
        return 0.0F;
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
    protected void readAdditionalSaveData(CompoundTag tag) {
        velocity = new Vec3(tag.getDouble("VX"), tag.getDouble("VY"), tag.getDouble("VZ"));
        ageTicks = tag.getInt("Age");
        sightDistance = tag.contains("SightDistance")
                ? tag.getDouble("SightDistance") : ApacheMissileBallistics.SIGHT_RANGE_BLOCKS;
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
        tag.putDouble("SightDistance", sightDistance);
        if (ownerUuid != null) {
            tag.putUUID("Owner", ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
