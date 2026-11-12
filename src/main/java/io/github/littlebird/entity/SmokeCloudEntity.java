package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.SmokeGrenadeRules;
import io.github.littlebird.registry.ModParticles;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * The cloud a smoke canister leaves behind.
 *
 * <p>The cloud is not drawn by this entity at all. Each client regenerates the visual out of
 * short lived puffs: every tick the entity asks the level for a handful of {@link ModParticles}
 * smoke sprites inside its own volume, and those puffs boil, swell and die a second and a half
 * later, to be replaced by the next handful. The density comes from the turnover, the same way a
 * fire's smoke reads as a column even though no single particle survives more than a second.
 *
 * <p>That split is what keeps the frame budget sane. Nothing about the cloud crosses the wire but
 * its full radius and its lifetime, both written once when the canopy opens; the age, the swell
 * and the fade are derived on each client through {@link SmokeGrenadeRules}. The server pays for
 * one entity per cloud and no particles, and each client pays for the puffs it can see, at the
 * rate its own config allows.
 */
public class SmokeCloudEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_FULL_RADIUS =
            SynchedEntityData.defineId(SmokeCloudEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFETIME =
            SynchedEntityData.defineId(SmokeCloudEntity.class, EntityDataSerializers.INT);
    /** Clouds are watched from the air as well, so they stay on screen further than a hull does. */
    private static final double RENDER_DISTANCE_BLOCKS = 160.0D;
    /** Fraction of the full radius the puffs are seeded inside, so the edge stays ragged. */
    private static final double SEED_RADIUS_FRACTION = 0.92D;

    private int ageTicks;

    public SmokeCloudEntity(EntityType<? extends SmokeCloudEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = false;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_FULL_RADIUS, 5.5F);
        this.entityData.define(DATA_LIFETIME, 300);
    }

    /** Opens the canopy: the radius it will swell to and the life it has to run. */
    public void open(float fullRadius, int lifetimeTicks) {
        this.entityData.set(DATA_FULL_RADIUS, Math.max(0.0F, fullRadius));
        this.entityData.set(DATA_LIFETIME, Math.max(SmokeGrenadeRules.MIN_LIFETIME_TICKS,
                lifetimeTicks));
        this.ageTicks = 0;
    }

    public float fullRadius() {
        return this.entityData.get(DATA_FULL_RADIUS);
    }

    public int lifetimeTicks() {
        return this.entityData.get(DATA_LIFETIME);
    }

    public int ageTicks() {
        return this.ageTicks;
    }

    /** Radius of the cloud right now, in blocks, as the client draws it. */
    public float radius() {
        return SmokeGrenadeRules.radiusAt(this.ageTicks, fullRadius());
    }

    /** 0..1 opacity of the cloud right now. */
    public float opacity() {
        return SmokeGrenadeRules.opacityAt(this.ageTicks, lifetimeTicks());
    }

    @Override
    public void tick() {
        super.tick();
        this.ageTicks++;
        if (this.level().isClientSide) {
            emitPuffs();
            return;
        }
        if (SmokeGrenadeRules.spent(this.ageTicks, lifetimeTicks())) {
            discard();
        }
    }

    /**
     * Spawns this tick's share of the cloud, on the client only.
     *
     * <p>The puffs are seeded in a squat cylinder rather than a ball: a screening cloud has to
     * stand tall enough to swallow a hull and still hug the ground, and a sphere spends most of
     * its puffs in the air where they hide nothing.
     */
    private void emitPuffs() {
        int perTick = LittleBirdConfig.smokeGrenadeParticlesPerTick();
        if (perTick <= 0) {
            return;
        }
        int puffLife = LittleBirdConfig.smokeGrenadeParticleLifetimeTicks();
        if (this.ageTicks > lifetimeTicks() - puffLife) {
            // Stop feeding the cloud before it dies, so the last puffs fade out with it instead of
            // hanging in the air after the screen has already lifted.
            return;
        }
        float radius = radius();
        if (radius <= 0.05F) {
            return;
        }
        Vec3 centre = position();
        RandomSource random = this.level().getRandom();
        for (int i = 0; i < perTick; i++) {
            double yaw = random.nextDouble() * Math.PI * 2.0D;
            double horizontal = radius * SEED_RADIUS_FRACTION * Math.sqrt(random.nextDouble());
            double x = centre.x + Math.cos(yaw) * horizontal;
            double z = centre.z + Math.sin(yaw) * horizontal;
            double y = centre.y + radius * (random.nextDouble() * 1.15D - 0.10D);
            // A slow outward drift is what keeps the surface of the cloud moving instead of
            // sitting still like a painted shell.
            double outward = 0.008D + random.nextDouble() * 0.012D;
            this.level().addParticle(ModParticles.SMOKE_PUFF.get(), true, x, y, z,
                    Math.cos(yaw) * outward,
                    0.006D + random.nextDouble() * 0.010D,
                    Math.sin(yaw) * outward);
        }
    }

    /** The cloud is scenery: nothing collides with it and no pick ray should stop on it. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSqr) {
        return distanceSqr < RENDER_DISTANCE_BLOCKS * RENDER_DISTANCE_BLOCKS;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.ageTicks = tag.getInt("Age");
        if (tag.contains("FullRadius")) {
            this.entityData.set(DATA_FULL_RADIUS, tag.getFloat("FullRadius"));
        }
        if (tag.contains("Lifetime")) {
            this.entityData.set(DATA_LIFETIME, tag.getInt("Lifetime"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.ageTicks);
        tag.putFloat("FullRadius", fullRadius());
        tag.putInt("Lifetime", lifetimeTicks());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
