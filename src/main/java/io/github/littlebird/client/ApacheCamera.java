package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.ApacheEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Camera presentation for the Apache. The distance is widened where the request is made (see
 * CameraDistanceMixin); here the camera rolls with the aircraft, which matters more than it does
 * for the Little Bird because this one has no bank limit and can sit inverted.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ApacheCamera {
    private ApacheCamera() {
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof ApacheEntity apache)) {
            return;
        }
        event.setRoll(event.getRoll()
                + ApacheCameraMath.cameraRollDeg(apache.getRenderRollDeg(),
                        LittleBirdConfig.cameraRollFactor()));
    }
}
