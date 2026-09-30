package io.github.littlebird.entity;

import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.MissileGuidance;
import io.github.littlebird.entity.weapon.MissileKind;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Fire-and-forget air-to-air missile. Once locked it steers towards the target with
 * {@link MissileGuidance}; a target that deploys flares during the seduction window can break the
 * lock and leave the missile to fly on ballistically. It reuses the 70mm rocket's flight,
 * tracking, hit detection and warhead plumbing through {@link LittleBirdRocketEntity}.
 */
public class LockOnMissileEntity extends LittleBirdRocketEntity {
    /** Launched faster than the 70mm rocket so it can run a jet down. */
    public static final double INITIAL_SPEED = 2.0D;
    /** How far the self-acquiring radar missile will look for a target in flight. */
    private static final double RADAR_ACQUIRE_RANGE_BLOCKS = 128.0D;

    private UUID targetUuid;
    private boolean seduced;
    private MissileKind kind = MissileKind.IR;

    public LockOnMissileEntity(EntityType<? extends LockOnMissileEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = false;
    }

    /** Points the seeker at a target entity; the missile is fire-and-forget from here. */
    public void lockOnto(Entity target) {
        lockOnto(target, MissileKind.IR);
    }

    /** Points the seeker at a target and picks the warhead and seeker behaviour. */
    public void lockOnto(Entity target, MissileKind kind) {
        this.targetUuid = target == null ? null : target.getUUID();
        this.kind = kind == null ? MissileKind.IR : kind;
        this.seduced = false;
    }

    public boolean isSeduced() {
        return seduced;
    }

    @Override
    protected Vec3 steer(Vec3 start) {
        Entity target = resolveTarget();
        if (target == null && kind == MissileKind.RADAR) {
            // A radar missile may be fired into the dark: it self-acquires in flight.
            target = acquireNearestAirframe();
            if (target != null) {
                this.targetUuid = target.getUUID();
            }
        }
        if (target != null) {
            if (kind.seducible() && target instanceof JetEntity jet
                    && jet.countermeasureSeduces(random.nextDouble())) {
                // Spoofed: the seeker follows the flare, so the missile flies on unguided.
                this.targetUuid = null;
                this.seduced = true;
            } else {
                LittleBirdBallistics.Vector3 steered = MissileGuidance.home(
                        vector(position()), vector(velocity),
                        vector(target.position()), vector(target.getDeltaMovement()),
                        kind.guidance());
                this.heading = vec(steered.normalize());
                return vec(steered);
            }
        }
        return super.steer(start);
    }

    /** Nearest live airframe in range, used by the self-acquiring radar missile. */
    private Entity acquireNearestAirframe() {
        if (!(level() instanceof ServerLevel server)) {
            return null;
        }
        Entity nearest = null;
        double best = Double.MAX_VALUE;
        for (Entity candidate : server.getEntities(this,
                getBoundingBox().inflate(RADAR_ACQUIRE_RANGE_BLOCKS),
                e -> e != this && e.isAlive() && !e.isSpectator()
                        && AirframeTargets.isAirframe(e))) {
            double distance = candidate.position().distanceToSqr(position());
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private Entity resolveTarget() {
        if (targetUuid == null || !(level() instanceof ServerLevel server)) {
            return null;
        }
        Entity target = server.getEntity(targetUuid);
        return target != null && target.isAlive() && !target.isRemoved() ? target : null;
    }

    @Override
    protected float rocketDamageTo(LittleBirdBallistics.Airframe airframe) {
        return LittleBirdBallistics.damageTo(
                airframe != LittleBirdBallistics.Airframe.NONE,
                kind.directDamage(), kind.airframeDamage());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Target")) {
            targetUuid = tag.getUUID("Target");
        }
        seduced = tag.getBoolean("Seduced");
        kind = MissileKind.byOrdinal(tag.getInt("Kind"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (targetUuid != null) {
            tag.putUUID("Target", targetUuid);
        }
        tag.putBoolean("Seduced", seduced);
        tag.putInt("Kind", kind.ordinal());
    }

    private static LittleBirdBallistics.Vector3 vector(Vec3 value) {
        return new LittleBirdBallistics.Vector3(value.x, value.y, value.z);
    }

    private static Vec3 vec(LittleBirdBallistics.Vector3 value) {
        return new Vec3(value.x(), value.y(), value.z());
    }
}
