package io.github.littlebird.entity;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.weapon.SmokeGrenadeRules;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModItems;
import java.util.List;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;

/**
 * The thrown canister itself: it flies like any thrown item, then vents where it lands and leaves a
 * {@link SmokeCloudEntity} behind.
 *
 * <p>Two things end the flight: a hit, or the fuse. The fuse is the important one - a canister that
 * lands in a hole, or that a player drops off a cliff, still has to make its cloud where it comes to
 * rest rather than at the thrower's feet.
 */
public class SmokeGrenadeEntity extends ThrowableItemProjectile {
    /** Bounce damping is deliberately harsh: a canister of smoke should not roll down a hill. */
    private static final double BOUNCE_DAMPING = 0.45D;

    public SmokeGrenadeEntity(EntityType<? extends SmokeGrenadeEntity> type, Level level) {
        super(type, level);
    }

    public SmokeGrenadeEntity(EntityType<? extends SmokeGrenadeEntity> type, double x, double y,
                              double z, Level level) {
        super(type, x, y, z, level);
    }

    public SmokeGrenadeEntity(EntityType<? extends SmokeGrenadeEntity> type, LivingEntity shooter,
                              Level level) {
        super(type, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SMOKE_GRENADE.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved() || this.level().isClientSide) {
            return;
        }
        if (SmokeGrenadeRules.fuseExpired(this.tickCount)) {
            vent();
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        vent();
    }

    /** A canister does not collide with a cloud: it passes through its own smoke. */
    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof SmokeCloudEntity);
    }

    /** Opens the cloud where the canister came to rest, then removes the canister. */
    private void vent() {
        if (this.level() instanceof ServerLevel server) {
            openCloud(server);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.PLAYERS, 1.2F, 0.7F);
        }
        discard();
    }

    private void openCloud(ServerLevel server) {
        int cap = LittleBirdConfig.smokeGrenadeMaxClouds();
        if (cap <= 0) {
            return;
        }
        // The cap is what keeps a spawn point carpeted in canisters from becoming a permanent fog
        // bank: the oldest cloud of the neighbourhood goes before the new one opens.
        List<SmokeCloudEntity> nearby = server.getEntitiesOfClass(SmokeCloudEntity.class,
                getBoundingBox().inflate(96.0D));
        while (nearby.size() >= cap) {
            SmokeCloudEntity oldest = nearby.get(0);
            for (SmokeCloudEntity candidate : nearby) {
                if (candidate.ageTicks() > oldest.ageTicks()) {
                    oldest = candidate;
                }
            }
            oldest.discard();
            nearby.remove(oldest);
        }
        SmokeCloudEntity cloud = new SmokeCloudEntity(ModEntities.SMOKE_CLOUD.get(), server);
        cloud.setPos(getX(), getY() + 0.2D, getZ());
        cloud.open((float) LittleBirdConfig.smokeGrenadeRadius(),
                LittleBirdConfig.smokeGrenadeLifetimeTicks());
        server.addFreshEntity(cloud);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
