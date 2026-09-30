package io.github.littlebird.mixin;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.ApacheCameraMath;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.JetEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        if (minecraft.player == null) {
            return requested;
        }
        if (minecraft.player.getVehicle() instanceof ApacheEntity apache
                && !apache.isCockpitView()) {
            return ApacheCameraMath.requestedDistance(requested,
                    LittleBirdConfig.apacheThirdPersonDistance());
        }
        if (minecraft.player.getVehicle() instanceof io.github.littlebird.entity.B2Entity b2
                && !b2.isCockpitView()) {
            // Checked before the plain JetEntity branch: the B-2 is a jet subclass but needs the
            // camera a wing span further back than the fighters do.
            return ApacheCameraMath.requestedDistance(requested,
                    LittleBirdConfig.b2ThirdPersonDistance());
        }
        if (minecraft.player.getVehicle() instanceof JetEntity jet && !jet.isCockpitView()) {
            return ApacheCameraMath.requestedDistance(requested,
                    LittleBirdConfig.jetThirdPersonDistance());
        }
        return requested;
    }

    /**
     * Runs before vanilla reads the camera entity's view rotation, and slaves a jet pilot's view
     * to the airframe that frame so the exterior camera rides the nose instead of the mouse. See
     * {@link JetEntity#anchorPilotView}.
     */
    @Inject(method = "setup(Lnet/minecraft/world/level/BlockGetter;"
            + "Lnet/minecraft/world/entity/Entity;ZZF)V", at = @At("HEAD"))
    private void battlefield_slaveJetCamera(BlockGetter level, Entity entity, boolean detached,
            boolean mirror, float partialTick, CallbackInfo callback) {
        Player pilot = null;
        JetEntity jet = null;
        if (entity instanceof Player player && player.getVehicle() instanceof JetEntity ridden) {
            pilot = player;
            jet = ridden;
        } else if (entity instanceof JetEntity cameraJet
                && cameraJet.getControllingPassenger() instanceof Player rider) {
            pilot = rider;
            jet = cameraJet;
        }
        if (jet != null && pilot != null) {
            jet.anchorPilotView(pilot);
        }
    }
}
