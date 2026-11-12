package io.github.littlebird.client.hud;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.GunshipRider;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Air radar data: nearby contacts around the crewed aircraft, sampled every four ticks and kept
 * hull-relative, ready for {@link HudRadar}.
 *
 * <p>Contacts are living entities and other aircraft. The picture is world-aligned like the
 * artillery scope; the chevron in the middle shows which way the nose points.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class AirRadar {
    /** Radar range in blocks. */
    public static final double RADIUS = 200.0D;
    private static final int REFRESH_TICKS = 4;
    private static final double VERTICAL = 48.0D;

    private static List<HudRadar.Contact> contacts = List.of();
    private static int counter;

    private AirRadar() {
    }

    public static List<HudRadar.Contact> contacts() {
        return contacts;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof GunshipRider aircraft)) {
            contacts = List.of();
            return;
        }
        if (counter++ % REFRESH_TICKS != 0) {
            return;
        }
        Entity hull = minecraft.player.getVehicle();
        AABB box = new AABB(hull.getX() - RADIUS, hull.getY() - VERTICAL, hull.getZ() - RADIUS,
                hull.getX() + RADIUS, hull.getY() + VERTICAL, hull.getZ() + RADIUS);
        List<HudRadar.Contact> found = new ArrayList<>();
        for (Entity entity : minecraft.level.getEntitiesOfClass(Entity.class, box)) {
            if (entity == hull || entity == minecraft.player || !entity.isAlive()
                    || entity.isSpectator()) {
                continue;
            }
            boolean vehicle = entity instanceof GunshipRider;
            if (!vehicle && !(entity instanceof LivingEntity)) {
                continue;
            }
            found.add(new HudRadar.Contact(entity.getX() - hull.getX(),
                    entity.getZ() - hull.getZ(), vehicle));
        }
        contacts = found;
    }
}
