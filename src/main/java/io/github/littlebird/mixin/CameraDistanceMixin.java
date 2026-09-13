package io.github.littlebird.mixin;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.ApacheCameraMath;
import io.github.littlebird.entity.ApacheEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla third person asks the camera for four blocks of room, which is inside a nine block
 * gunship: the aircraft filled the screen and the lens sat in its own tail. Camera#setup is the
 * only place that number exists and it is private, so this is the one place a mod can change it.
 *
 * Only the request is raised, so vanilla's own sweep still clips the camera against terrain.
 */
@Mixin(Camera.class)
public abstract class CameraDistanceMixin {
    @ModifyVariable(method = "getMaxZoom(D)D", at = @At("HEAD"), argsOnly = true)
    private double battlefield_gunshipCameraDistance(double requested) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof ApacheEntity apache)
                || apache.isCockpitView()) {
            return requested;
        }
        return ApacheCameraMath.requestedDistance(requested,
                LittleBirdConfig.apacheThirdPersonDistance());
    }
}
