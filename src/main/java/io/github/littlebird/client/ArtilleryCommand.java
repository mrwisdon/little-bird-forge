package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ArtilleryPlatform;
import io.github.littlebird.entity.ArtilleryStrike;
import io.github.littlebird.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opens the fire-mission board for the current artillery driver. */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ArtilleryCommand {
    private ArtilleryCommand() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (LittleBirdKeyMappings.ARTILLERY_STRIKE.consumeClick()) {
            // Another screen already owns the frame (chat, inventory, pause): let it win.
            if (minecraft.screen != null || minecraft.player == null) {
                continue;
            }
            Entity vehicle = minecraft.player.getVehicle();
            if (!(vehicle instanceof ArtilleryPlatform platform) || !isDriving(vehicle, minecraft.player)) {
                continue;
            }
            ArtilleryStrike strike = platform.strike();
            if (strike.ready()) {
                minecraft.setScreen(new ArtilleryCommandScreen(vehicle, strike));
            } else {
                // Same ping as the scope's "no": the driver hears why nothing opened.
                minecraft.player.playSound(ModSounds.RADAR_PING.get(), 0.7F, 0.5F);
            }
        }
    }

    /** The board and the key binding must agree on who controls the hull. */
    static boolean isDriving(Entity vehicle, Player player) {
        return player != null && player.isAlive() && vehicle.isAlive()
                && player.getVehicle() == vehicle && vehicle.getControllingPassenger() == player;
    }
}
