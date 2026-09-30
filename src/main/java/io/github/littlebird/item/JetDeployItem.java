package io.github.littlebird.item;

import io.github.littlebird.entity.JetEntity;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Deploys one of the jets on the ground a few blocks in front of the player. */
public class JetDeployItem extends Item {
    private static final double SPAWN_DISTANCE = 6.0D;

    private final Supplier<EntityType<? extends JetEntity>> entityType;

    public JetDeployItem(Properties properties,
                         Supplier<EntityType<? extends JetEntity>> entityType) {
        super(properties);
        this.entityType = entityType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            JetEntity jet = entityType.get().create(server);
            if (jet == null) {
                return InteractionResultHolder.fail(stack);
            }
            Vec3 spawn = GunshipPlacementProbe.spawnPosition(server, player, SPAWN_DISTANCE);
            jet.setPos(spawn.x, spawn.y, spawn.z);
            CompoundTag carried = stack.getTag();
            if (carried != null && carried.contains("Health")) {
                jet.setHealth(carried.getFloat("Health"));
            }
            jet.setYRot(player.getYRot());
            server.addFreshEntity(jet);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            server.playSound(null, spawn.x, spawn.y, spawn.z,
                    SoundEvents.ARMOR_STAND_PLACE, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
