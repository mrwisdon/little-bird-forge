package io.github.littlebird.item;

import io.github.littlebird.entity.SmokeGrenadeEntity;
import io.github.littlebird.registry.ModEntities;
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

/**
 * A hand thrown smoke canister. Right click lobs it where the player is looking; there is no charge
 * and no aiming mode, because the whole point of the item is to put a screen down now.
 */
public class SmokeGrenadeItem extends Item {
    /** Blocks per tick the canister leaves the hand at: a hard lob, not a throw. */
    private static final float THROW_SPEED = 0.9F;
    /** Degrees of spread: enough that two throws do not land on the same block. */
    private static final float INACCURACY = 2.0F;
    /** Ticks before the next canister can be thrown, so a stack cannot be emptied in one tick. */
    private static final int THROW_COOLDOWN_TICKS = 10;

    public SmokeGrenadeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.7F, 1.1F);
        if (level instanceof ServerLevel server) {
            SmokeGrenadeEntity canister = new SmokeGrenadeEntity(
                    ModEntities.SMOKE_GRENADE.get(), player, server);
            Vec3 look = player.getLookAngle();
            canister.shoot(look.x, look.y + 0.08D, look.z, THROW_SPEED, INACCURACY);
            server.addFreshEntity(canister);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        player.getCooldowns().addCooldown(this, THROW_COOLDOWN_TICKS);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
