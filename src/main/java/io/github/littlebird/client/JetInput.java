package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.JetEntity;
import io.github.littlebird.network.ApacheReloadPacket;
import io.github.littlebird.network.LittleBirdExitPacket;
import io.github.littlebird.network.JetFlarePacket;
import io.github.littlebird.network.LittleBirdWeaponInputPacket;
import io.github.littlebird.network.ModNetwork;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Turns the local pilot's keyboard and mouse into jet flight input.
 *
 * <p>Mouse roll and pitch need no code here: the entity reads the pilot's yaw delta as the roll
 * command and the pilot's xRot as the pitch command every tick, then anchors the view back to the
 * airframe. This class supplies the axes the mouse cannot express: W/S on the throttle, A/D on the
 * rudder, sprint for the afterburner, the VTOL toggle, the cockpit camera and the dismount key.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class JetInput {
    private static final boolean FORCE_THIRD_PERSON = true;

    private static JetEntity piloted;
    private static CameraType cameraBeforePilot;
    private static boolean cockpitView;
    private static boolean vtol;
    private static int activeWeapon = JetEntity.WEAPON_CANNON;
    private static int sentWeapon = -1;
    private static boolean sentTrigger;
    private static boolean gunSelectHeld;
    private static boolean rocketSelectHeld;
    private static boolean missileSelectHeld;
    private static boolean agmSelectHeld;
    private static boolean radarSelectHeld;

    private JetInput() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // Global, not gated on riding: the pilot may want to flip this on the ground or in a
        // menu, and the choice is written to the config so it survives a restart.
        while (LittleBirdKeyMappings.INVERT_PITCH.consumeClick()) {
            boolean inverted = LittleBirdConfig.toggleJetInvertPitch();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(inverted
                        ? "message.battlefield.pitch_inverted"
                        : "message.battlefield.pitch_normal"), true);
            }
        }
        while (LittleBirdKeyMappings.TOGGLE_MOUSE_YAW.consumeClick()) {
            boolean mouseYaw = LittleBirdConfig.toggleJetMouseYaw();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(mouseYaw
                        ? "message.battlefield.mouse_yaw"
                        : "message.battlefield.mouse_roll"), true);
            }
        }
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof JetEntity jet)) {
            endSession(minecraft);
            return;
        }
        if (piloted != jet) {
            if (piloted != null) {
                piloted.resetClientPrediction();
            }
            piloted = jet;
            beginSession(minecraft);
        }

        boolean controlsActive = minecraft.screen == null && minecraft.isWindowActive();
        boolean isPilot = minecraft.player == jet.getControllingPassenger();
        int controls = 0;
        if (controlsActive && isPilot) {
            if (minecraft.options.keyUp.isDown()) {
                controls |= JetEntity.CONTROL_THROTTLE_UP;
            }
            if (minecraft.options.keyDown.isDown()) {
                controls |= JetEntity.CONTROL_THROTTLE_DOWN;
            }
            if (minecraft.options.keyRight.isDown()) {
                controls |= JetEntity.CONTROL_RUDDER_RIGHT;
            }
            if (minecraft.options.keyLeft.isDown()) {
                controls |= JetEntity.CONTROL_RUDDER_LEFT;
            }
        }
        jet.setClientControls(isPilot ? controls : 0);
        jet.setClientAfterburner(controlsActive && isPilot
                && minecraft.options.keySprint.isDown());

        if (minecraft.screen == null) {
            if (isPilot) {
                // Vanilla also consumes the hotbar keys, so selection uses our own edge
                // detection on the physical key state instead of consumeClick().
                // The B-2 has a bomb bay, not the fighter's five stores: its own keys (G/Z)
                // drive the bay, so the hotbar and the trigger are left alone.
                boolean b2SkipsFighterWeapons = jet instanceof io.github.littlebird.entity.B2Entity;
                if (!b2SkipsFighterWeapons
                        && edgePressed(minecraft.options.keyHotbarSlots[0].isDown(), true)) {
                    activeWeapon = JetEntity.WEAPON_CANNON;
                }
                if (!b2SkipsFighterWeapons
                        && edgePressed(minecraft.options.keyHotbarSlots[1].isDown(), false)) {
                    activeWeapon = JetEntity.WEAPON_ROCKETS;
                }
                boolean missileDown = minecraft.options.keyHotbarSlots[2].isDown();
                if (!b2SkipsFighterWeapons && missileDown && !missileSelectHeld) {
                    activeWeapon = JetEntity.WEAPON_IR_MISSILE;
                }
                missileSelectHeld = missileDown;
                boolean agmDown = minecraft.options.keyHotbarSlots[3].isDown();
                if (!b2SkipsFighterWeapons && agmDown && !agmSelectHeld) {
                    activeWeapon = JetEntity.WEAPON_AGM;
                }
                agmSelectHeld = agmDown;
                boolean radarDown = minecraft.options.keyHotbarSlots[4].isDown();
                if (!b2SkipsFighterWeapons && radarDown && !radarSelectHeld) {
                    activeWeapon = JetEntity.WEAPON_RADAR_MISSILE;
                }
                radarSelectHeld = radarDown;
                if (!b2SkipsFighterWeapons) {
                    // The B-2 has no rocket bay to top up: R is its auto-release key and is read
                    // by B2Input, so the fighter rearm must leave the click alone.
                    while (LittleBirdKeyMappings.RELOAD.consumeClick()) {
                        ModNetwork.CHANNEL.sendToServer(new ApacheReloadPacket());
                    }
                }
                while (LittleBirdKeyMappings.FLARES.consumeClick()) {
                    ModNetwork.CHANNEL.sendToServer(new JetFlarePacket());
                }
                while (!b2SkipsFighterWeapons && LittleBirdKeyMappings.VTOL.consumeClick()) {
                    vtol = !vtol;
                    jet.setClientVtol(vtol);
                }
                while (LittleBirdKeyMappings.COCKPIT.consumeClick()) {
                    cockpitView = !cockpitView;
                    jet.setCockpitView(cockpitView);
                    applyCameraMode(minecraft);
                }
            }
            while (LittleBirdKeyMappings.EXIT.consumeClick()) {
                jet.setClientControls(0);
                ModNetwork.CHANNEL.sendToServer(new LittleBirdExitPacket());
            }
        }
        jet.setShoulderView(false);

        boolean trigger = controlsActive && isPilot && minecraft.options.keyAttack.isDown();
        if (isPilot && !(jet instanceof io.github.littlebird.entity.B2Entity)
                && (activeWeapon != sentWeapon || trigger != sentTrigger)) {
            ModNetwork.CHANNEL.sendToServer(new LittleBirdWeaponInputPacket(
                    activeWeapon, false, trigger));
            sentWeapon = activeWeapon;
            sentTrigger = trigger;
        }
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

    private static void beginSession(Minecraft minecraft) {
        cockpitView = false;
        vtol = false;
        activeWeapon = JetEntity.WEAPON_CANNON;
        sentWeapon = -1;
        sentTrigger = false;
        gunSelectHeld = false;
        rocketSelectHeld = false;
        missileSelectHeld = false;
        agmSelectHeld = false;
        radarSelectHeld = false;
        if (piloted != null) {
            piloted.setClientVtol(false);
        }
        cameraBeforePilot = minecraft.options.getCameraType();
        applyCameraMode(minecraft);
    }

    /** Third person by default; C switches to the cockpit camera. */
    private static void applyCameraMode(Minecraft minecraft) {
        if (!FORCE_THIRD_PERSON) {
            return;
        }
        if (cockpitView) {
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            return;
        }
        CameraType restore = cameraBeforePilot == null
                ? CameraType.THIRD_PERSON_BACK : cameraBeforePilot;
        minecraft.options.setCameraType(
                restore.isFirstPerson() ? CameraType.THIRD_PERSON_BACK : restore);
    }

    private static void endSession(Minecraft minecraft) {
        if (piloted == null) {
            return;
        }
        piloted.resetClientPrediction();
        piloted = null;
        cockpitView = false;
        vtol = false;
        activeWeapon = JetEntity.WEAPON_CANNON;
        sentWeapon = -1;
        sentTrigger = false;
        gunSelectHeld = false;
        rocketSelectHeld = false;
        missileSelectHeld = false;
        agmSelectHeld = false;
        radarSelectHeld = false;
        if (FORCE_THIRD_PERSON && cameraBeforePilot != null) {
            minecraft.options.setCameraType(cameraBeforePilot);
            cameraBeforePilot = null;
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (isPiloting() && event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (isPiloting()) {
            event.setCanceled(true);
        }
    }

    private static boolean isPiloting() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.getVehicle() instanceof JetEntity;
    }
}
