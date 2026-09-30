package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.B2Entity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Camera presentation for the B-2: the pod's gimbal view, the rigid ("hard") cockpit view and the
 * wide third person lens.
 *
 * <p>JetCamera skips the B-2 so this class owns the B-2 lens outright; otherwise the cockpit FOV
 * multiplier would be applied twice.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class B2Camera {
    private B2Camera() {
    }

    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov event) {
        if (B2BombCam.isActive()) {
            event.setFOV(B2BombCam.fov());
            return;
        }
        B2Entity b2 = piloted();
        if (b2 == null) {
            return;
        }
        if (B2PodView.isActive()) {
            event.setFOV(B2PodView.zoomFov());
            return;
        }
        double factor = b2.isCockpitView()
                ? LittleBirdConfig.cockpitFovMultiplier()
                : LittleBirdConfig.b2ThirdPersonFovMultiplier();
        event.setFOV(B2CockpitMath.cockpitFov(event.getFOV(), factor));
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (B2BombCam.isActive()) {
            // Watching the stick go down: the lens lives above the bomb and looks at it.
            event.setYaw(B2BombCam.cameraYawDeg());
            event.setPitch(B2BombCam.cameraPitchDeg());
            event.setRoll(B2BombCam.cameraRollDeg());
            return;
        }
        B2Entity b2 = piloted();
        if (b2 == null) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        if (B2PodView.isActive()) {
            // The pod is gyro stabilised: it does not roll with the airframe.
            event.setYaw(B2PodView.cameraYawDeg(b2));
            event.setPitch(B2PodView.cameraPitchDeg(b2));
            event.setRoll(0.0F);
            return;
        }
        float roll = b2.interpolatedRenderRollDeg(partialTick);
        if (b2.isCockpitView() && LittleBirdConfig.cockpitRigidView()) {
            // Rigid: the frame is welded to the airframe, so the aircraft's attitude is the
            // camera's attitude. No lerp, no head bob, no smoothing.
            event.setYaw(B2CockpitMath.rigidYawDeg(b2.getYRot()));
            event.setPitch(B2CockpitMath.rigidPitchDeg(b2.getXRot()));
            event.setRoll(B2CockpitMath.rigidRollDeg(roll));
            return;
        }
        event.setRoll(event.getRoll() + roll);
    }

    static B2Entity piloted() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }
        return minecraft.player.getVehicle() instanceof B2Entity b2 ? b2 : null;
    }
}
