package io.github.littlebird.mixin;

import io.github.littlebird.client.B2BombCam;
import io.github.littlebird.client.B2PodView;
import io.github.littlebird.entity.B2Entity;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Moves the camera for the B-2's two sight systems: the bomb chase camera and the
 * electro-optical pod.
 *
 * <p>Camera#setup is where vanilla decides the lens sits on the rider's eyes; the pod hangs under
 * the nose instead, so the position is rewritten after vanilla has had its say. Rotation comes
 * from the viewport event, and vanilla's own terrain sweep has already run, so the pod cannot be
 * dragged through a hill it is not looking at.
 */
@Mixin(Camera.class)
public abstract class B2PodCameraMixin {
    @Shadow
    protected abstract void setPosition(double pX, double pY, double pZ);

    @Inject(method = "setup", at = @At("TAIL"))
    private void battlefield_podCamera(BlockGetter level, Entity entity, boolean detached,
                                       boolean mirrored, float partialTick, CallbackInfo ci) {
        if (entity instanceof Player && B2BombCam.isActive()) {
            double[] bomb = B2BombCam.cameraPosition();
            this.setPosition(bomb[0], bomb[1], bomb[2]);
            return;
        }
        if (!B2PodView.isActive() || !(entity instanceof Player player)) {
            return;
        }
        if (!(player.getVehicle() instanceof B2Entity b2)) {
            return;
        }
        double[] pos = B2PodView.cameraPosition(b2);
        this.setPosition(pos[0], pos[1], pos[2]);
    }
}
