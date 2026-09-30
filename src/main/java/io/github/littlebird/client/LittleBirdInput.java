package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.LittleBirdCameraMath;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.network.LittleBirdExitPacket;
import io.github.littlebird.network.LittleBirdWeaponInputPacket;
import io.github.littlebird.network.ModNetwork;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Turns the local pilot's keyboard and mouse into flight and weapon input.
 *
 * Mouse yaw and pitch need no code here: while riding, the vanilla mouse already
 * rotates the player, and the entity reads player yRot/xRot every tick. This
 * class supplies the axes the mouse cannot express (collective, roll), the
 * dismount key, weapon selection on the vanilla 1/2 hotbar keys, the salvo
 * toggle, and it keeps the camera in third person.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class LittleBirdInput {
    private static final boolean FORCE_THIRD_PERSON = true;

    private static LittleBirdEntity piloted;
    private static CameraType cameraBeforePilot;
    private static int activeWeapon = LittleBirdEntity.WEAPON_GUN;
    private static boolean salvoMode;
    private static int sentWeapon = -1;
    private static boolean sentSalvo;
    private static boolean sentTrigger;
    private static boolean gunSelectHeld;
    private static boolean rocketSelectHeld;
    private static boolean cockpitView;

    private LittleBirdInput() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof LittleBirdEntity helicopter)) {
            endSession(minecraft);
            return;
        }
        if (piloted != helicopter) {
            if (piloted != null) {
                piloted.resetClientPrediction();
            }
            piloted = helicopter;
            beginSession(minecraft);
        }

        boolean controlsActive = minecraft.screen == null && minecraft.isWindowActive();
        int controls = 0;
        if (controlsActive) {
            if (minecraft.options.keyUp.isDown()) {
                controls |= LittleBirdEntity.CONTROL_CLIMB;
            }
            if (minecraft.options.keyDown.isDown()) {
                controls |= LittleBirdEntity.CONTROL_DESCEND;
            }
            if (minecraft.options.keyRight.isDown()) {
                controls |= LittleBirdEntity.CONTROL_ROLL_RIGHT;
            }
            if (minecraft.options.keyLeft.isDown()) {
                controls |= LittleBirdEntity.CONTROL_ROLL_LEFT;
            }
        }
        // Passengers are ride-along only: they never feed controls, and the server would refuse
        // them anyway (every entry point checks the controlling passenger).
        boolean isPilot = minecraft.player == helicopter.getControllingPassenger();
        helicopter.setClientControls(isPilot ? controls : 0);
        helicopter.setClientEngineRequested(true);
        if (!isPilot) {
            // Third person only, and F5 is overruled rather than merely ignored: a passenger has
            // no cockpit camera and nothing to aim.
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }

        if (minecraft.screen == null) {
            // Vanilla also consumes the hotbar keys in Minecraft#handleKeybinds, which
            // may run before this handler, so the selection uses our own edge detection
            // on the physical key state instead of consumeClick().
            if (isPilot) {
                if (edgePressed(minecraft.options.keyHotbarSlots[0].isDown(), true)) {
                    activeWeapon = LittleBirdEntity.WEAPON_GUN;
                }
                if (edgePressed(minecraft.options.keyHotbarSlots[1].isDown(), false)) {
                    activeWeapon = LittleBirdEntity.WEAPON_ROCKETS;
                }
                while (LittleBirdKeyMappings.SALVO.consumeClick()) {
                    salvoMode = !salvoMode;
                }
                while (LittleBirdKeyMappings.RELOAD.consumeClick()) {
                    ModNetwork.CHANNEL.sendToServer(
                            new io.github.littlebird.network.ApacheReloadPacket());
                }
                while (LittleBirdKeyMappings.COCKPIT.consumeClick()) {
                    cockpitView = !cockpitView;
                    helicopter.setCockpitView(cockpitView);
                    applyCameraMode(minecraft);
                }
            }
            while (LittleBirdKeyMappings.EXIT.consumeClick()) {
                helicopter.setClientControls(0);
                ModNetwork.CHANNEL.sendToServer(new LittleBirdExitPacket());
            }
        }

        // Aim button (right mouse) holds the over-the-shoulder camera in third person.
        helicopter.setShoulderView(LittleBirdCameraMath.applies(
                io.github.littlebird.LittleBirdConfig.shoulderView(),
                isPilot,
                !minecraft.options.getCameraType().isFirstPerson(),
                helicopter.isCockpitView(),
                controlsActive && minecraft.options.keyUse.isDown()));

        boolean trigger = controlsActive && minecraft.options.keyAttack.isDown();
        if (isPilot
                && (activeWeapon != sentWeapon || salvoMode != sentSalvo
                        || trigger != sentTrigger)) {
            ModNetwork.CHANNEL.sendToServer(new LittleBirdWeaponInputPacket(
                    activeWeapon, salvoMode, trigger));
            sentWeapon = activeWeapon;
            sentSalvo = salvoMode;
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
        activeWeapon = LittleBirdEntity.WEAPON_GUN;
        salvoMode = false;
        sentWeapon = -1;
        sentSalvo = false;
        sentTrigger = false;
        gunSelectHeld = false;
        rocketSelectHeld = false;
        cockpitView = false;
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
        sentWeapon = -1;
        sentTrigger = false;
        gunSelectHeld = false;
        rocketSelectHeld = false;
        if (FORCE_THIRD_PERSON && cameraBeforePilot != null) {
            minecraft.options.setCameraType(cameraBeforePilot);
            cameraBeforePilot = null;
        }
    }

    /**
     * Minecraft draws its own crosshair in first person, which in the cockpit view
     * doubled up with ours and read as one oversized cross. The projected impact
     * reticles are the aiming reference instead.
     */
    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
        if (isPiloting() && event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isPiloting()) {
            event.setCanceled(true);
        }
    }

    /** Hides the riding player on every client, including armour and held items. */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().getVehicle() instanceof io.github.littlebird.entity.GunshipRider) {
            event.setCanceled(true);
        }
    }

    /** Stops attack/use from breaking blocks or opening GUIs while piloting. */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (isPiloting()) {
            event.setCanceled(true);
        }
    }

    /** True while the local player is crewing any gunship in the pack. */
    private static boolean isPiloting() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && minecraft.player.getVehicle() instanceof io.github.littlebird.entity.GunshipRider;
    }
}
