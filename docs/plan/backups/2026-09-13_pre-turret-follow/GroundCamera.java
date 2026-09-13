package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
import io.github.littlebird.entity.MainBattleTankEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Camera presentation for the ground vehicles.
 *
 * <p>Deliberately a separate handler from {@link LittleBirdCamera}: the aircraft own their own FOV
 * keys, and the ground hulls get their own, so widening the tank's exterior view can never change
 * how a helicopter looks. The heavy lifting of deciding which multiplier applies lives in
 * {@link GroundViewMath}, which is unit tested.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class GroundCamera {
    private GroundCamera() {
    }

    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        Entity vehicle = minecraft.player.getVehicle();
        boolean groundVehicle = vehicle instanceof MainBattleTankEntity
                || vehicle instanceof InfantryFightingVehicleEntity;
        boolean interiorView = vehicle instanceof GroundView view && view.isInteriorView();
        // Exactly the two keys the Little Bird reads, so "the same view settings" means the same
        // numbers for every crewed vehicle: exterior uses the third person key, the turret interior
        // uses the cockpit key.
        double factor = GroundViewMath.fovMultiplier(groundVehicle, interiorView,
                LittleBirdConfig.thirdPersonFovMultiplier(),
                LittleBirdConfig.cockpitFovMultiplier());
        if (factor != 1.0D) {
            event.setFOV((float) (event.getFOV() * factor));
        }
    }
}
