package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.B2Entity;
import io.github.littlebird.network.B2BombReleasePacket;
import io.github.littlebird.network.B2WeaponSelectPacket;
import io.github.littlebird.network.ModNetwork;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * B-2 specific controls: bomb release, store selection, the pod sight and the designation.
 *
 * <p>While the pod is up the mouse is taken off the airframe: the deltas drive the gimbal and the
 * pilot's view is pinned to the aircraft so a bomb run holds its heading. The cockpit toggle (C)
 * stays with JetInput, which already treats every jet the same way.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class B2Input {
    private static CameraType cameraBeforePod;
    private static boolean attackHeld;
    private static boolean rightHeld;
    private static double lastYaw;
    private static double lastPitch;
    private static boolean tracking;

    private B2Input() {
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        // Ages wherever the crew is, so a feed entry never sticks when they leave the aeroplane.
        io.github.littlebird.client.B2KillFeed.tickClient();
        if (minecraft.player == null) {
            endSession(minecraft);
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            endSession(minecraft);
            return;
        }
        boolean pilot = b2.getControllingPassenger() == minecraft.player;
        if (minecraft.screen != null) {
            tracking = false;
            return;
        }
        autoReleaseTick(minecraft, b2);
        B2BombCam.tick();

        while (LittleBirdKeyMappings.B2_POD.consumeClick()) {
            B2PodView.setActive(!B2PodView.isActive());
            b2.setClientInputSuppressed(B2PodView.isActive());
            applyCameraMode(minecraft);
        }
        if (B2PodView.isActive()) {
            podTick(minecraft, b2);
        } else {
            tracking = false;
            if (b2.isClientInputSuppressed()) {
                b2.setClientInputSuppressed(false);
            }
        }
        // Release and store selection work from both seats in both views: the pod is where a bomb
        // run actually happens, so gating these on "pod down" left the release key dead there.
        if (pilot) {
            while (LittleBirdKeyMappings.SALVO.consumeClick()) {
                onReleasePressed(minecraft, b2);
            }
            while (LittleBirdKeyMappings.RELOAD.consumeClick()) {
                onAutoTogglePressed(minecraft, b2);
            }
            while (LittleBirdKeyMappings.WEAPON_SWITCH.consumeClick()) {
                int next = (b2.getBombWeapon() + 1) % B2Entity.WEAPON_COUNT;
                ModNetwork.CHANNEL.sendToServer(new B2WeaponSelectPacket(next));
                minecraft.player.displayClientMessage(
                        Component.translatable(next == B2Entity.WEAPON_JDAM
                                ? "message.battlefield.b2_weapon_jdam"
                                : "message.battlefield.b2_weapon_incendiary"), true);
            }
        }
    }

    private static void podTick(Minecraft minecraft, B2Entity b2) {
        B2PodView.tick();
        // The pod owns the mouse: JetEntity stops banking the look travel (so the airframe's own
        // leveller holds the bomb run level) and leaves the pilot's view free for us to read.
        b2.setClientInputSuppressed(true);
        if (!tracking) {
            lastYaw = minecraft.player.getYRot();
            lastPitch = minecraft.player.getXRot();
            tracking = true;
        }
        double deltaYaw = Mth.wrapDegrees((float) (minecraft.player.getYRot() - lastYaw));
        double deltaPitch = minecraft.player.getXRot() - lastPitch;
        lastYaw = minecraft.player.getYRot();
        lastPitch = minecraft.player.getXRot();
        if (!io.github.littlebird.entity.weapon.B2BombRun.holdsLevel(b2.getRenderRollDeg(), b2.getXRot())) {
            // No level readout on the glass, but the pod overlay prints the state for the crew.
            B2PodView.setLevelWarning(true);
        } else {
            B2PodView.setLevelWarning(false);
        }
        B2PodView.addMouseDelta(deltaYaw, deltaPitch);

        boolean attack = minecraft.options.keyAttack.isDown();
        if (attack && !attackHeld) {
            if (!B2PodView.designate()) {
                minecraft.player.displayClientMessage(
                        Component.translatable("message.battlefield.b2_no_target"), true);
            }
        }
        attackHeld = attack;
        boolean use = minecraft.options.keyUse.isDown();
        if (use && !rightHeld) {
            B2PodView.clearDesignation();
            B2PodView.clearAutoRelease();
            announce(minecraft, "message.battlefield.b2_auto_cancelled");
        }
        rightHeld = use;
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (!B2PodView.isActive()) {
            return;
        }
        B2PodView.addZoom(event.getScrollDelta());
        event.setCanceled(true);
    }

    /**
     * One key does the whole job. With no designation the reticle becomes the target, then the
     * release either goes now (the prediction says it lands on the target) or arms the automatic
     * trigger so the crew only has to keep the sight on the target.
     */
    /**
     * Z: the stick leaves the bay now. Auto-designating first keeps the common case to one press,
     * but the key never second-guesses the crew about the solution - the sight already shows how
     * good the drop is, and arming lives on its own key (R).
     */
    private static void onReleasePressed(Minecraft minecraft, B2Entity b2) {
        if (b2.bombCount(b2.getBombWeapon()) <= 0) {
            announce(minecraft, "message.battlefield.b2_no_bombs");
            return;
        }
        if (B2PodView.designation() == null && B2PodView.isActive() && B2PodView.designate()) {
            announce(minecraft, "message.battlefield.b2_auto_designated");
        }
        boolean salvo = minecraft.player != null && minecraft.player.isShiftKeyDown();
        B2PodView.clearAutoRelease();
        ModNetwork.CHANNEL.sendToServer(new B2BombReleasePacket(salvo));
        announce(minecraft, "message.battlefield.b2_bomb_away");
    }

    /**
     * R: arm or cancel the automatic release. Two keys, two jobs - Z drops on command, R drops when
     * the sight reaches the line - so neither key has to guess what the crew meant.
     */
    private static void onAutoTogglePressed(Minecraft minecraft, B2Entity b2) {
        if (B2PodView.isAutoArmed()) {
            B2PodView.clearAutoRelease();
            announce(minecraft, "message.battlefield.b2_auto_cancelled");
            return;
        }
        if (b2.bombCount(b2.getBombWeapon()) <= 0) {
            announce(minecraft, "message.battlefield.b2_no_bombs");
            return;
        }
        if (B2PodView.designation() == null && B2PodView.isActive() && B2PodView.designate()) {
            announce(minecraft, "message.battlefield.b2_auto_designated");
        }
        if (B2PodView.designation() == null) {
            announce(minecraft, "message.battlefield.b2_auto_needs_target");
            return;
        }
        if (B2PodView.impactReady()) {
            // Already inside the window: arming would only fire on the next tick, so drop it now.
            ModNetwork.CHANNEL.sendToServer(new B2BombReleasePacket(false));
            announce(minecraft, "message.battlefield.b2_bomb_away");
            return;
        }
        if (!B2PodView.canArmNow()) {
            announce(minecraft, "message.battlefield.b2_not_closing");
            return;
        }
        B2PodView.armAutoRelease();
        announce(minecraft, "message.battlefield.b2_auto_armed");
    }

    /** Fires the stick on its own once the prediction has closed onto the designation. */
    private static void autoReleaseTick(Minecraft minecraft, B2Entity b2) {
        B2PodView.sampleMiss();
        if (!B2PodView.isAutoArmed() || !B2PodView.impactReady()) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new B2BombReleasePacket(false));
        B2PodView.clearAutoRelease();
        announce(minecraft, "message.battlefield.b2_auto_released");
    }

    private static void announce(Minecraft minecraft, String key) {
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    net.minecraft.network.chat.Component.translatable(key), true);
        }
    }

    private static void applyCameraMode(Minecraft minecraft) {
        if (B2PodView.isActive()) {
            cameraBeforePod = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            return;
        }
        CameraType restore = cameraBeforePod == null ? CameraType.THIRD_PERSON_BACK : cameraBeforePod;
        minecraft.options.setCameraType(restore);
        cameraBeforePod = null;
    }

    private static void endSession(Minecraft minecraft) {
        if (B2PodView.isActive()) {
            B2PodView.setActive(false);
            applyCameraMode(minecraft);
        }
        attackHeld = false;
        rightHeld = false;
        tracking = false;
    }
}
