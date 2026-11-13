package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ground.GroundVehicleControls;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Starts and stops the diesel loops of every ground vehicle the client can hear.
 *
 * <p>The Little Bird has had a positional rotor loop since the first milestone; the ground hulls
 * never got one, so a tank drove past in silence. This manager is that missing half: it scans the
 * rendered entities for anything that answers {@link GroundVehicleControls}, keeps a loop per
 * occupied hull inside earshot, and stops them again when the driver climbs out.
 *
 * <p>Everything it reads is already on the client: position and motion come from the entity, the
 * "is the driver aboard" question from the shared seat plan. No packet and no server change.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class GroundEngineSoundManager {
    /** Blocks beyond which a ground engine is not worth a sound channel. */
    private static final double EARSHOT = 64.0D;
    /** Hard cap on simultaneous loops; the nearest hulls win when a base is packed. */
    private static final int MAX_LOOPS = 8;

    private static final Map<Integer, GroundEngineSoundInstance> ENGINES = new HashMap<>();

    private GroundEngineSoundManager() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stopAll();
            return;
        }

        List<Entity> heard = new ArrayList<>();
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!isDriven(entity)) {
                continue;
            }
            if (minecraft.player.distanceToSqr(entity) > EARSHOT * EARSHOT) {
                continue;
            }
            heard.add(entity);
        }
        heard.sort(Comparator.comparingDouble(minecraft.player::distanceToSqr));
        if (heard.size() > MAX_LOOPS) {
            heard = heard.subList(0, MAX_LOOPS);
        }

        Set<Integer> kept = new HashSet<>();
        for (Entity entity : heard) {
            kept.add(entity.getId());
            if (ENGINES.containsKey(entity.getId())) {
                continue;
            }
            GroundEngineSoundInstance instance = new GroundEngineSoundInstance(entity);
            ENGINES.put(entity.getId(), instance);
            minecraft.getSoundManager().play(instance);
        }

        ENGINES.keySet().retainAll(kept);
        ENGINES.entrySet().removeIf(entry -> !isPlaying(minecraft, entry.getValue()));
    }

    /**
     * True when somebody is driving: seat 0 is the driver in every ground hull's plan, and an
     * empty hull must not idle.
     */
    static boolean isDriven(Entity entity) {
        if (!(entity instanceof GroundVehicleControls controls) || !entity.isAlive()) {
            return false;
        }
        if (entity.getPassengers().isEmpty()) {
            return false;
        }
        return entity.getPassengers().get(0) instanceof Player && controls.isDriver(0);
    }

    private static void stopAll() {
        if (ENGINES.isEmpty()) {
            return;
        }
        for (GroundEngineSoundInstance instance : ENGINES.values()) {
            instance.halt();
        }
        ENGINES.clear();
    }

    private static boolean isPlaying(Minecraft minecraft, SoundInstance instance) {
        return minecraft.getSoundManager().isActive(instance);
    }
}
