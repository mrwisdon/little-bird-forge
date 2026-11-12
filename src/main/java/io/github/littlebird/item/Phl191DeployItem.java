package io.github.littlebird.item;

import io.github.littlebird.entity.Phl191Entity;
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

/** Deploys a PHL-191 box launcher on the ground in front of the player. */
public class Phl191DeployItem extends Item {
    private static final double SPAWN_DISTANCE = 8.0D;

    public Phl191DeployItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 spawn = GunshipPlacementProbe.spawnPosition(server, player, SPAWN_DISTANCE);
            Phl191Entity launcher = new Phl191Entity(ModEntities.PHL191.get(), server);
            launcher.setPos(spawn.x, spawn.y, spawn.z);
            CompoundTag carried = stack.getTag();
            if (carried != null && carried.contains("Health")) {
                launcher.setHealth(carried.getFloat("Health"));
            }
            launcher.setYRot(player.getYRot());
            server.addFreshEntity(launcher);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            server.playSound(null, spawn.x, spawn.y, spawn.z,
                    SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 1.0F, 0.5F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
