package io.github.littlebird.item;

import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.registry.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Deploys an AH-64 a few blocks in front of the player. */
public class ApacheDeployItem extends Item {
    private static final double SPAWN_DISTANCE = 4.0D;

    public ApacheDeployItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 spawn = GunshipPlacementProbe.spawnPosition(server, player, SPAWN_DISTANCE);
            ApacheEntity apache = new ApacheEntity(ModEntities.APACHE.get(), server);
            apache.setPos(spawn.x, spawn.y, spawn.z);
            CompoundTag carried = stack.getTag();
            if (carried != null && carried.contains("Health")) {
                apache.setHealth(carried.getFloat("Health"));
            }
            apache.setYRot(player.getYRot());
            server.addFreshEntity(apache);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            server.playSound(null, spawn.x, spawn.y, spawn.z,
                    SoundEvents.ARMOR_STAND_PLACE, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
