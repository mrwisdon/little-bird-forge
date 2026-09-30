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
                || vehicle instanceof InfantryFightingVehicleEntity
                || vehicle instanceof io.github.littlebird.entity.BradleyEntity;
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

    /**
     * Slaves the turret interior camera's yaw to the turret's own synched yaw. The rider offset in
     * {@code positionRider} uses the same yaw, so the camera and the turret share one frame and the
     * turret's shell cannot sweep across the lens. Without this the camera keeps the raw mouse yaw
     * while the turret slews after it, which is exactly the sweep this fixes.
     *
     * <p>Exterior and aircraft cameras are untouched: the guard requires the interior view. The
     * barrel's elevation is deliberately not copied - only yaw is taken, so vertical aim keeps the
     * player's own pitch as before.
     */
    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof GroundView view)
                || !view.isInteriorView()) {
            return;
        }
        event.setYaw(GroundViewMath.followYawDeg(true, event.getYaw(),
                view.renderTurretYawDeg((float) event.getPartialTick())));
    }
}
