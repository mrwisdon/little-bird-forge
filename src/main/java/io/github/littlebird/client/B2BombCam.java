package io.github.littlebird.client;

import io.github.littlebird.entity.B2Entity;
import io.github.littlebird.entity.weapon.B2BombCamRules;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

/**
 * Chase camera for a released stick.
 *
 * <p>When the server names the bomb, the lens hops to a point above it and looks down, so the crew
 * watches the fall and the explosion instead of guessing where it went. Once the bomb is gone the
 * blast is held for one second, then the view hands itself back to whatever the pilot was using -
 * the pod, the rigid cockpit or the wide chase camera. Nothing about the camera mode is changed,
 * so there is no state to put back.
 */
public final class B2BombCam {
    private static int targetId = -1;
    private static int ticksFollowing;
    private static int ticksSinceLost;
    private static boolean seen;
    private static double lastX;
    private static double lastY;
    private static double lastZ;
    private static float lastYaw;

    private B2BombCam() {
    }

    /** Follows this bomb unless a stick is already being watched. */
    public static void follow(int entityId) {
        if (isActive() && !seenLost()) {
            return;
        }
        targetId = entityId;
        ticksFollowing = 0;
        ticksSinceLost = 0;
        seen = false;
        lastX = 0.0D;
        lastY = 0.0D;
        lastZ = 0.0D;
        Minecraft minecraft = Minecraft.getInstance();
        lastYaw = minecraft.player == null ? 0.0F
                : minecraft.player.getVehicle() instanceof B2Entity b2 ? b2.getYRot()
                        : minecraft.player.getYRot();
    }

    private static boolean seenLost() {
        return seen && ticksSinceLost > 0;
    }

    public static boolean isActive() {
        return targetId >= 0;
    }

    /** One client tick of book-keeping: remember where it is, notice when it is gone. */
    public static void tick() {
        if (!isActive()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ticksFollowing++;
        Entity bomb = minecraft.level == null ? null : minecraft.level.getEntity(targetId);
        if (bomb != null && bomb.isAlive()) {
            lastX = bomb.getX();
            lastY = bomb.getY();
            lastZ = bomb.getZ();
            if (minecraft.player != null && minecraft.player.getVehicle() instanceof B2Entity b2) {
                lastYaw = b2.getYRot();
            }
            seen = true;
            ticksSinceLost = 0;
        } else if (seen) {
            // Gone: this is the explosion. Hold the shot for a second, then go home.
            ticksSinceLost++;
        }
        if (seen && B2BombCamRules.shouldReturnHome(ticksSinceLost)) {
            stop();
            return;
        }
        if (B2BombCamRules.timedOut(ticksFollowing)) {
            stop();
        }
    }

    /** Hands the camera back immediately. */
    public static void stop() {
        targetId = -1;
        ticksFollowing = 0;
        ticksSinceLost = 0;
        seen = false;
    }

    public static double[] cameraPosition() {
        return new double[]{lastX, B2BombCamRules.cameraHeight(lastY), lastZ};
    }

    public static float cameraYawDeg() {
        return lastYaw;
    }

    public static float cameraPitchDeg() {
        return (float) B2BombCamRules.cameraPitchDeg();
    }

    public static float cameraRollDeg() {
        return 0.0F;
    }

    public static double fov() {
        return B2BombCamRules.cameraFovDeg();
    }
}
