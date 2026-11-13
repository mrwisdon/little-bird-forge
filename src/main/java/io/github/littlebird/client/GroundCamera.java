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
        // Exterior still uses the third person key, but the turret interior is a magnifying gun
        // sight and reads its own: a tank crew gets real magnification (0.55 by default), while
        // widening the exterior ground view can never change how the optic looks. The IFV shares the
        // same sight key because it looks through the same kind of optic.
        double factor = GroundViewMath.fovMultiplier(groundVehicle, interiorView,
                LittleBirdConfig.thirdPersonFovMultiplier(),
                LittleBirdConfig.groundSightFovMultiplier());
        if (factor != 1.0D) {
            event.setFOV((float) (event.getFOV() * factor));
        }
    }

    /**
     * Slaves the turret interior camera's yaw to the turret's own synched yaw, then adds the impact
     * shock on top of whatever camera this is. The rider offset in {@code positionRider} uses the
     * same yaw, so the camera and the turret share one frame and the turret's shell cannot sweep
     * across the lens. Without the lock the camera keeps the raw mouse yaw while the turret slews
     * after it, which is exactly the sweep this fixes.
     *
     * <p>Both steps live in this one handler on purpose: a second handler for the same event would
     * run in an order the mod cannot control, and the absolute yaw assignment here would either wipe
     * the shock or be wiped by it. The order inside the method is therefore the order on screen.
     *
     * <p>The shock is added outside the interior-only guard, because a nearby shell must be felt from
     * every camera the player can hold: the tank optic, a vehicle's exterior view, and on foot. The
     * barrel's elevation is still deliberately not copied - only yaw is taken, so vertical aim keeps
     * the player's own pitch.
     */
    /**
     * The tank's seat two has his own cupola optic: it turns with his own head, all the way round,
     * so the camera must not be slaved to the main gun's turret for him. The driver's sight stays
     * locked to the turret, which is what keeps the 120mm lens and the barrel in one frame.
     */
    private static boolean drivesGunnerOptic(net.minecraft.world.entity.player.Player player) {
        return player.getVehicle() instanceof MainBattleTankEntity tank
                && tank.seatOf(player) == io.github.littlebird.entity.seat.GroundSeat.TANK_GUNNER;
    }

    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        float partialTick = (float) event.getPartialTick();

        // The tank's sight is a gun sight: it must not swing past the barrel it is bolted to. The
        // 120mm stops at -30/+10, so the eye stops there too instead of climbing to 85 degrees over
        // a barrel that cannot follow.
        if (minecraft.player.getVehicle() instanceof MainBattleTankEntity tank
                && tank.seatOf(minecraft.player)
                == io.github.littlebird.entity.seat.GroundSeat.TANK_DRIVER
                && tank.isInteriorView()) {
            event.setPitch(tank.renderTurretPitchDeg(partialTick));
        }

        if (minecraft.player.getVehicle() instanceof GroundView view && view.isInteriorView()
                && !drivesGunnerOptic(minecraft.player)) {
            event.setYaw(GroundViewMath.followYawDeg(true, event.getYaw(),
                    view.renderTurretYawDeg(partialTick)));
        }
        // Additive and unconditional: ImpactShake returns exactly zero when there is no trauma, so
        // the camera is bit-for-bit what it was until a shell actually lands.
        event.setYaw(event.getYaw() + ImpactShake.yawOffsetDeg(partialTick));
        event.setPitch(event.getPitch() + ImpactShake.pitchOffsetDeg(partialTick));
    }
}
