package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.JetEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Widens the jet camera and rolls it with the airframe so banking reads like Battlefield. */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class JetCamera {
    /** Sign of the camera roll; flip if the horizon tilts the wrong way in game. */
    private static final float CAMERA_ROLL_SIGN = 1.0F;

    private JetCamera() {
    }

    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof JetEntity jet)
                || jet instanceof io.github.littlebird.entity.B2Entity) {
            return;
        }
        double factor = jet.isCockpitView()
                ? LittleBirdConfig.cockpitFovMultiplier()
                : LittleBirdConfig.thirdPersonFovMultiplier();
        event.setFOV(event.getFOV() * factor);
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof JetEntity jet)
                || jet instanceof io.github.littlebird.entity.B2Entity) {
            return;
        }
        event.setRoll(event.getRoll()
                + jet.interpolatedRenderRollDeg((float) event.getPartialTick())
                * CAMERA_ROLL_SIGN * LittleBirdConfig.cameraRollFactor());
    }
}
