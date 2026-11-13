package io.github.littlebird.item;

import io.github.littlebird.entity.GunshipRider;
import io.github.littlebird.entity.weapon.RepairTorch;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;

/**
 * The field repair torch: hold right click on a damaged hull within {@link RepairTorch#RANGE_BLOCKS}
 * and it welds {@link RepairTorch#HEALTH_PER_SECOND} points a second back into it.
 *
 * <p>Battlefield's tool is a hold, not a click, so the item starts a use on right click and does the
 * work in {@link #onUseTick}. The repair itself is server side, one tick at a time, and stops on its
 * own once the hull is whole; the client only sends the ordinary "I am using this item" state that
 * vanilla already synchronises.
 *
 * <p>Nothing about a specific vehicle is known here: the tool asks the crosshair for a
 * {@link GunshipRider} and welds it.
 */
public class RepairTorchItem extends Item {
    /** Ticks between two hiss transients while the torch is lit. */
    private static final int SOUND_INTERVAL_TICKS = 5;
    /** A held item can be used for this long; effectively "until the button is released". */
    private static final int USE_DURATION = 72000;

    public RepairTorchItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(findTarget(level, player) instanceof GunshipRider vehicle)
                || !RepairTorch.needsRepair(vehicle.getHealth(), vehicle.maxHealth())) {
            return InteractionResultHolder.pass(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
        if (!(living instanceof Player player) || level.isClientSide) {
            return;
        }
        Entity target = findTarget(level, player);
        if (!(target instanceof GunshipRider vehicle)) {
            return;
        }
        float max = vehicle.maxHealth();
        float before = vehicle.getHealth();
        if (!RepairTorch.needsRepair(before, max)) {
            return;
        }
        vehicle.setHealth(RepairTorch.repaired(before, max, RepairTorch.HEALTH_PER_TICK));

        if (level instanceof ServerLevel server) {
            Vec3 weld = new Vec3(target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D, target.getZ());
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    weld.x, weld.y, weld.z, 4, 0.35D, 0.35D, 0.35D, 0.02D);
            if (player.tickCount % SOUND_INTERVAL_TICKS == 0) {
                server.playSound(null, weld.x, weld.y, weld.z, SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS, 0.25F, 1.6F);
            }
        }
    }

    /** The hull under the crosshair, raycast from the eyes, blocked by terrain like any use. */
    private static Entity findTarget(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getLookAngle().scale(RepairTorch.RANGE_BLOCKS);
        Vec3 end = eye.add(reach);
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, limit,
                player.getBoundingBox().expandTowards(reach).inflate(1.0D),
                candidate -> candidate instanceof GunshipRider && candidate.isAlive());
        return hit == null ? null : hit.getEntity();
    }
}
