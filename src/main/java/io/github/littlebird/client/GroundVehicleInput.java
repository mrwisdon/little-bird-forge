package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ground.GroundDrive;
import io.github.littlebird.entity.ground.GroundVehicleControls;
import io.github.littlebird.network.GroundWeaponInputPacket;
import io.github.littlebird.network.LittleBirdExitPacket;
import io.github.littlebird.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client input for both ground vehicles.
 *
 * <p>The work is split the same way the vehicles split it: the driver reads W/S/A/D for throttle
 * and steering (and the client predicts the hull from them), while the trigger and the reload key
 * go to the server, which knows which seat fired and therefore which weapon to use. Sent only on
 * a change, so a stationary crew costs nothing.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class GroundVehicleInput {
    private static GroundVehicleControls crewed;
    private static boolean sentTrigger;
    private static boolean reloadHeld;
    private static boolean sentElevationUp;
    private static boolean sentElevationDown;
    private static boolean sentPlz05ElevationUp;
    private static boolean sentPlz05ElevationDown;
    /** The camera the crew had before boarding, put back when they leave - as the aircraft do. */
    private static net.minecraft.client.CameraType cameraBeforeCrew;

    private GroundVehicleInput() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof GroundVehicleControls controls)) {
            endSession(minecraft);
            return;
        }
        if (crewed != controls) {
            if (crewed != null) {
                crewed.resetClientPrediction();
            }
            crewed = controls;
            sentTrigger = false;
            reloadHeld = false;
            // Third person by default, exactly like the Little Bird, remembering what the player
            // was using so leaving the hull puts their own camera back.
            cameraBeforeCrew = minecraft.options.getCameraType();
            applyCameraMode(minecraft, false);
        }

        boolean controlsActive = minecraft.screen == null && minecraft.isWindowActive();
        // The tank has two crew seats and no passenger list to shuffle while parked: the seat
        // switch key flips which seat each rider holds. Only the tank needs it, and only its crew.
        if (controlsActive && minecraft.player.getVehicle()
                instanceof io.github.littlebird.entity.MainBattleTankEntity tank
                && (tank.driver() == minecraft.player || tank.gunner() == minecraft.player)) {
            while (LittleBirdKeyMappings.SEAT_SWITCH.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(
                        new io.github.littlebird.network.TankSeatSwitchPacket());
            }
        }
        int seat = controls.seatOf(minecraft.player);
        int axes = 0;
        if (controlsActive && controls.isDriver(seat)) {
            if (minecraft.options.keyUp.isDown()) {
                axes |= GroundDrive.THROTTLE_FORWARD;
            }
            if (minecraft.options.keyDown.isDown()) {
                axes |= GroundDrive.THROTTLE_BACK;
            }
            if (minecraft.options.keyLeft.isDown()) {
                axes |= GroundDrive.STEER_LEFT;
            }
            if (minecraft.options.keyRight.isDown()) {
                axes |= GroundDrive.STEER_RIGHT;
            }
        }
        controls.setClientControls(axes);

        // PHL-191 lays its launcher with Z / X instead of the look pitch. The keys are held, so
        // the server is only told when the pair changes state.
        if (controls instanceof io.github.littlebird.entity.Plz05Entity) {
            boolean up = controlsActive && LittleBirdKeyMappings.PHL191_UP.isDown();
            boolean down = controlsActive && !minecraft.player.isShiftKeyDown()
                    && LittleBirdKeyMappings.PHL191_DOWN.isDown();
            if (up != sentPlz05ElevationUp || down != sentPlz05ElevationDown) {
                ModNetwork.CHANNEL.sendToServer(new io.github.littlebird.network.Plz05ElevationPacket(
                        up, down));
                sentPlz05ElevationUp = up;
                sentPlz05ElevationDown = down;
            }
        }
        if (controls instanceof io.github.littlebird.entity.Phl191Entity) {
            boolean up = controlsActive && LittleBirdKeyMappings.PHL191_UP.isDown();
            boolean down = controlsActive && !minecraft.player.isShiftKeyDown()
                    && LittleBirdKeyMappings.PHL191_DOWN.isDown();
            if (up != sentElevationUp || down != sentElevationDown) {
                ModNetwork.CHANNEL.sendToServer(new io.github.littlebird.network.Phl191ElevationPacket(
                        up, down));
                sentElevationUp = up;
                sentElevationDown = down;
            }
        }

        if (minecraft.screen == null) {
            while (LittleBirdKeyMappings.EXIT.consumeClick()) {
                // X doubles as "lower the launcher" in the PHL-191, so its dismount is Shift+X and
                // a bare X is swallowed here instead of throwing the crew out every press.
                if ((controls instanceof io.github.littlebird.entity.Phl191Entity
                        || controls instanceof io.github.littlebird.entity.Plz05Entity)
                        && !minecraft.player.isShiftKeyDown()) {
                    continue;
                }
                ModNetwork.CHANNEL.sendToServer(new LittleBirdExitPacket());
            }
            // The cockpit key doubles as the turret interior toggle for the ground hulls: the
            // interior camera is first person, the exterior one goes back to third person.
            while (LittleBirdKeyMappings.COCKPIT.consumeClick()
                    && controls instanceof GroundView view) {
                view.setInteriorView(!view.isInteriorView());
                applyCameraMode(minecraft, view.isInteriorView());
            }
        }

        boolean reloadDown = minecraft.screen == null && LittleBirdKeyMappings.RELOAD.isDown();
        boolean reloadEdge = reloadDown && !reloadHeld;
        reloadHeld = reloadDown;

        int shellDelta = 0;
        if (minecraft.screen == null && controls.isOperator(seat)
                && LittleBirdKeyMappings.SHELL_SWITCH.consumeClick()) {
            shellDelta = 1;
        }
        int weaponDelta = 0;
        if (minecraft.screen == null && controls.isOperator(seat)
                && LittleBirdKeyMappings.WEAPON_SWITCH.consumeClick()) {
            weaponDelta = 1;
        }

        boolean trigger = controlsActive && controls.isOperator(seat)
                && minecraft.options.keyAttack.isDown();
        if (trigger != sentTrigger || reloadEdge || shellDelta != 0 || weaponDelta != 0) {
            ModNetwork.CHANNEL.sendToServer(
                    new GroundWeaponInputPacket(trigger, reloadEdge, shellDelta, weaponDelta));
            sentTrigger = trigger;
        }
    }

    private static void endSession(Minecraft minecraft) {
        if (crewed != null) {
            if (crewed instanceof GroundView view) {
                view.setInteriorView(false);
            }
            crewed.resetClientPrediction();
            crewed = null;
            applyCameraMode(minecraft, false);
            cameraBeforeCrew = null;
        }
        sentTrigger = false;
        reloadHeld = false;
        sentElevationUp = false;
        sentElevationDown = false;
    }

    /**
     * The aircraft rule: the turret interior is first person, and leaving it restores whatever
     * camera the player had before boarding (a first person preference falls back to third person,
     * because the hulls are meant to be seen from outside).
     */
    private static void applyCameraMode(Minecraft minecraft, boolean interiorView) {
        if (interiorView) {
            minecraft.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            return;
        }
        net.minecraft.client.CameraType restore = cameraBeforeCrew == null
                ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : cameraBeforeCrew;
        minecraft.options.setCameraType(restore.isFirstPerson()
                ? net.minecraft.client.CameraType.THIRD_PERSON_BACK : restore);
    }
}
