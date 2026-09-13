package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.LittleBirdCameraMath;
import io.github.littlebird.entity.flight.FlightControls;
import io.github.littlebird.entity.seat.ApacheSeat;
import io.github.littlebird.network.ApacheGunnerAimPacket;
import io.github.littlebird.network.ApacheReloadPacket;
import io.github.littlebird.network.ApacheSeatSwitchPacket;
import io.github.littlebird.network.LittleBirdWeaponInputPacket;
import io.github.littlebird.network.ModNetwork;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client input for the AH-64.
 *
 * The rear seat flies exactly like the Little Bird: the mouse already drives the player's yaw
 * and pitch, and this class only supplies the axes the mouse cannot express (collective and
 * roll) plus the seat switch key. The front seat does not fly at all - its mouse drives the
 * chain gun turret, so it reports where the gunner is looking and the server slews the turret.
 *
 * Sent only on change, so a stationary gunner costs nothing.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ApacheInput {
    private static final float AIM_EPSILON_DEG = 0.25F;

    private static ApacheEntity crewed;
    private static CameraType cameraBeforeCrew;
    private static int activeWeapon = ApacheEntity.WEAPON_ROCKETS;
    private static int sentWeapon = -1;
    private static boolean sentTrigger;
    private static boolean gunSelectHeld;
    private static boolean rocketSelectHeld;
    private static float sentYaw = Float.NaN;
    private static float sentPitch = Float.NaN;

    private ApacheInput() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof ApacheEntity apache)) {
            endSession();
            return;
        }
        if (crewed != apache) {
            if (crewed != null) {
                crewed.resetClientPrediction();
            }
            crewed = apache;
            cameraBeforeCrew = minecraft.options.getCameraType();
            applyCameraMode(minecraft, false);
            activeWeapon = ApacheEntity.WEAPON_ROCKETS;
            sentWeapon = -1;
            sentTrigger = false;
            gunSelectHeld = false;
            rocketSelectHeld = false;
            sentYaw = Float.NaN;
            sentPitch = Float.NaN;
        }

        boolean controlsActive = minecraft.screen == null && minecraft.isWindowActive();
        boolean pilot = apache.seatOf(minecraft.player) == ApacheSeat.PILOT;

        int controls = 0;
        if (controlsActive && pilot) {
            if (minecraft.options.keyUp.isDown()) {
                controls |= FlightControls.CLIMB;
            }
            if (minecraft.options.keyDown.isDown()) {
                controls |= FlightControls.DESCEND;
            }
            if (minecraft.options.keyRight.isDown()) {
                controls |= FlightControls.ROLL_RIGHT;
            }
            if (minecraft.options.keyLeft.isDown()) {
                controls |= FlightControls.ROLL_LEFT;
            }
        }
        apache.setClientControls(controls);
        apache.setClientEngineRequested(true);

        if (minecraft.screen == null) {
            while (LittleBirdKeyMappings.SEAT_SWITCH.consumeClick()) {
                ModNetwork.CHANNEL.sendToServer(new ApacheSeatSwitchPacket());
            }
            while (LittleBirdKeyMappings.COCKPIT.consumeClick()) {
                apache.setCockpitView(!apache.isCockpitView());
                applyCameraMode(minecraft, apache.isCockpitView());
            }
            if (pilot) {
                // Vanilla consumes the hotbar keys in its own key handling, so weapon
                // selection uses our own edge detection on the physical key state.
                if (edgePressed(minecraft.options.keyHotbarSlots[0].isDown(), true)) {
                    activeWeapon = ApacheEntity.WEAPON_ROCKETS;
                }
                if (edgePressed(minecraft.options.keyHotbarSlots[1].isDown(), false)) {
                    activeWeapon = ApacheEntity.WEAPON_MISSILES;
                }
                while (LittleBirdKeyMappings.RELOAD.consumeClick()) {
                    ModNetwork.CHANNEL.sendToServer(new ApacheReloadPacket());
                }
            }
        }

        // Aim button (right mouse) holds the over-the-shoulder camera in third person, exactly
        // as it does on the Little Bird. The pilot holds it; the gunner keeps the turret.
        apache.setShoulderView(LittleBirdCameraMath.applies(
                LittleBirdConfig.shoulderView(),
                pilot,
                !minecraft.options.getCameraType().isFirstPerson(),
                apache.isCockpitView(),
                controlsActive && minecraft.options.keyUse.isDown()));

        if (pilot) {
            boolean trigger = controlsActive && minecraft.options.keyAttack.isDown();
            if (activeWeapon != sentWeapon || trigger != sentTrigger) {
                ModNetwork.CHANNEL.sendToServer(new LittleBirdWeaponInputPacket(
                        activeWeapon, false, trigger));
                sentWeapon = activeWeapon;
                sentTrigger = trigger;
            }
        }

        if (!pilot) {
            float yaw = minecraft.player.getYRot();
            float pitch = minecraft.player.getXRot();
            boolean trigger = controlsActive && minecraft.options.keyAttack.isDown();
            boolean moved = Float.isNaN(sentYaw)
                    || Math.abs(yaw - sentYaw) >= AIM_EPSILON_DEG
                    || Math.abs(pitch - sentPitch) >= AIM_EPSILON_DEG;
            if (moved || trigger != sentTrigger) {
                ModNetwork.CHANNEL.sendToServer(new ApacheGunnerAimPacket(yaw, pitch, trigger));
                sentYaw = yaw;
                sentPitch = pitch;
                sentTrigger = trigger;
            }
        }
    }

    /**
     * Third person by default, like the Little Bird: the exterior view is what shows off the
     * airframe, and the seat view is opt in with the cockpit key.
     */
    private static void applyCameraMode(Minecraft minecraft, boolean cockpitView) {
        if (cockpitView) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            return;
        }
        CameraType restore = cameraBeforeCrew == null
                ? CameraType.THIRD_PERSON_BACK : cameraBeforeCrew;
        minecraft.options.setCameraType(
                restore.isFirstPerson() ? CameraType.THIRD_PERSON_BACK : restore);
    }

    /** Reports one press of a held key, independent of vanilla click consumption. */
    private static boolean edgePressed(boolean down, boolean gunKey) {
        boolean wasDown = gunKey ? gunSelectHeld : rocketSelectHeld;
        if (gunKey) {
            gunSelectHeld = down;
        } else {
            rocketSelectHeld = down;
        }
        return down && !wasDown;
    }

    private static void endSession() {
        if (crewed == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (cameraBeforeCrew != null) {
            minecraft.options.setCameraType(cameraBeforeCrew);
            cameraBeforeCrew = null;
        }
        crewed.setCockpitView(false);
        crewed.setShoulderView(false);
        crewed.resetClientPrediction();
        crewed = null;
        sentYaw = Float.NaN;
        sentPitch = Float.NaN;
        sentTrigger = false;
    }
}
