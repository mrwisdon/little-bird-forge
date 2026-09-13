package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ApacheEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hides the crew's own bodies while they are strapped into an Apache.
 *
 * The aircraft model has no cockpit interior, so a rider is drawn standing inside the fuselage
 * with their head and shoulders poking through the canopy - from the exterior camera the crew
 * member covered the middle of the screen, reticle and all. The tandem canopy reads as two
 * seats on its own, so the bodies are simply not drawn.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ApacheRiderRender {
    private ApacheRiderRender() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof ApacheEntity) {
            event.setCanceled(true);
        }
    }
}
