package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.LittleBirdEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Rolls the camera with the aircraft so banking reads like Battlefield. */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class LittleBirdCamera {
    /**
     * Sign of the camera roll. Needs one in-game look to confirm; if the horizon
     * tilts the wrong way, flip this to -1.0F.
     */
    private static final float CAMERA_ROLL_SIGN = 1.0F;

    private LittleBirdCamera() {
    }

    /** Widens the exterior camera and keeps the cockpit view close to vanilla. */
    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof LittleBirdEntity helicopter)) {
            return;
        }
        double factor = helicopter.isCockpitView()
                ? io.github.littlebird.LittleBirdConfig.cockpitFovMultiplier()
                : io.github.littlebird.LittleBirdConfig.thirdPersonFovMultiplier();
        event.setFOV(event.getFOV() * factor);
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof LittleBirdEntity helicopter)) {
            return;
        }
        event.setRoll(event.getRoll()
                + helicopter.getRenderRollDeg() * CAMERA_ROLL_SIGN
                * io.github.littlebird.LittleBirdConfig.cameraRollFactor());
    }
}
