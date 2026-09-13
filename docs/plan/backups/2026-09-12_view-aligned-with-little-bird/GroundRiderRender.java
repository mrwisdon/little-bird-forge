package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
import io.github.littlebird.entity.MainBattleTankEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hides a crew member's own body while they are strapped into a ground hull.
 *
 * <p>The hull models have no crew compartment: the rider is drawn standing in the turret ring with
 * their head and shoulders poking out of the roof, and in the turret interior camera the body would
 * sit right where the fire control is drawn. The seats read on their own, so the bodies are simply
 * not drawn - exactly the rule the Apache already follows.
 *
 * <p>Rendering only: the hull's model, its turret and barrel, other units, the HUD and the hitbox
 * (which comes from the entity type, not from this renderer) are untouched.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class GroundRiderRender {
    private GroundRiderRender() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof MainBattleTankEntity
                || event.getEntity().getVehicle() instanceof InfantryFightingVehicleEntity) {
            event.setCanceled(true);
        }
    }
}
