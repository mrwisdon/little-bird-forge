package io.github.littlebird.item;

import io.github.littlebird.entity.BradleyEntity;
import io.github.littlebird.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Deploys an M2A2 Bradley infantry fighting vehicle in front of the player. */
public class BradleyDeployItem extends Item {
    private static final double SPAWN_DISTANCE = 5.5D;

    public BradleyDeployItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 spawn = GunshipPlacementProbe.spawnPosition(server, player, SPAWN_DISTANCE);
            BradleyEntity bradley = new BradleyEntity(ModEntities.BRADLEY.get(), server);
            bradley.setPos(spawn.x, spawn.y, spawn.z);
            CompoundTag carried = stack.getTag();
            if (carried != null && carried.contains("Health")) {
                bradley.setHealth(carried.getFloat("Health"));
            }
            bradley.setYRot(player.getYRot());
            server.addFreshEntity(bradley);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            server.playSound(null, spawn.x, spawn.y, spawn.z,
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
