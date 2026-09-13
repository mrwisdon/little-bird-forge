package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.weapon.LittleBirdGunCycle;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Starts and stops the positional rotor and Gatling loops for every Little Bird
 * the client can hear, driven purely by synced entity state.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class LittleBirdSoundManager {
    private static final double EARSHOT = 72.0D;
    private static final Map<Integer, LittleBirdEngineSoundInstance> ENGINES = new HashMap<>();
    private static final Map<Integer, LittleBirdGunSoundInstance> GUNS = new HashMap<>();

    private LittleBirdSoundManager() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            ENGINES.clear();
            GUNS.clear();
            return;
        }
        Set<Integer> heard = new HashSet<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof LittleBirdEntity helicopter)) {
                continue;
            }
            if (minecraft.player.distanceToSqr(helicopter) > EARSHOT * EARSHOT) {
                continue;
            }
            heard.add(helicopter.getId());
            if (!ENGINES.containsKey(helicopter.getId())
                    && helicopter.getRenderRotorRpm() > 0.02F) {
                LittleBirdEngineSoundInstance instance =
                        new LittleBirdEngineSoundInstance(helicopter);
                ENGINES.put(helicopter.getId(), instance);
                minecraft.getSoundManager().play(instance);
            }
            int state = helicopter.getGunState();
            boolean firing = state == LittleBirdGunCycle.WARMING
                    || state == LittleBirdGunCycle.FIRING;
            if (firing && !GUNS.containsKey(helicopter.getId())) {
                LittleBirdGunSoundInstance instance =
                        new LittleBirdGunSoundInstance(helicopter);
                GUNS.put(helicopter.getId(), instance);
                minecraft.getSoundManager().play(instance);
            }
        }
        ENGINES.keySet().retainAll(heard);
        GUNS.keySet().retainAll(heard);
        ENGINES.entrySet().removeIf(entry -> !isPlaying(minecraft, entry.getValue()));
        GUNS.entrySet().removeIf(entry -> !isPlaying(minecraft, entry.getValue()));
    }

    private static boolean isPlaying(Minecraft minecraft, SoundInstance instance) {
        return minecraft.getSoundManager().isActive(instance);
    }
}