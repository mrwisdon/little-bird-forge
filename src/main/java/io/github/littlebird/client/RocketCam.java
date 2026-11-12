package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.Phl191Entity;
import io.github.littlebird.entity.Phl191RocketEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rocket chase camera.
 *
 * <p>Firing a salvo hands the lens to the newest round and it stays there: the camera flies with
 * the rocket, and once the round lands it freezes on the impact instead of snapping back to the
 * cab. Only the cockpit key (C) gives the view back, and the next rocket fired takes it again.
 *
 * <p>Rounds are only picked up while they are fresh (a few ticks old), so leaving the view with C
 * cannot be undone by the same rocket still in flight - it takes a new launch.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class RocketCam {
    /** Distance behind the round. */
    private static final double DISTANCE = 7.0D;
    /** Height above the round. */
    private static final double LIFT = 2.4D;
    /** Frames the follow camera is smoothed with. */
    private static final float SMOOTHING = 0.35F;
    /** How close a rocket has to be to the driver to be picked up. */
    private static final double CAPTURE_RANGE = 48.0D;
    /** A rocket is only picked up this soon after it spawns. */
    private static final int FRESH_TICKS = 8;
    /**
     * One salvo is one camera pick: eight tubes leave six ticks apart, so without this the lens
     * hopped from round to round at short range. A new burst starts well past this window because
     * the launcher has to reload between salvos.
     */
    private static final int BURST_TICKS = 60;

    private static int targetId = -1;
    /** True once the followed round is gone: the camera holds its last frame. */
    private static boolean holding;
    /** Rocket dismissed with C; it must not be grabbed again. */
    private static int dismissedId = -1;
    /** Game time of the last pick-up, used to keep one salvo on one camera. */
    private static long lastCaptureTick = -1000L;
    private static boolean primed;
    private static double x;
    private static double y;
    private static double z;
    private static float yaw;
    private static float pitch;

    private RocketCam() {
    }

    public static boolean isActive() {
        return targetId >= 0;
    }

    /** One client tick: follow the round, hold the last frame, or pick up a fresh one. */
    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            stop();
            return;
        }
        if (isActive()) {
            // C hands the lens back; the view stays off until the next salvo.
            while (LittleBirdKeyMappings.COCKPIT.consumeClick()) {
                dismissedId = targetId;
                stop();
                return;
            }
            if (!(minecraft.player.getVehicle() instanceof io.github.littlebird.entity.ArtilleryPlatform)) {
                stop();
                return;
            }
            Entity rocket = minecraft.level.getEntity(targetId);
            if (rocket instanceof io.github.littlebird.entity.TrackedRound
                    && rocket != null && rocket.isAlive()) {
                follow(rocket);
                holding = false;
            } else {
                // The round has landed: keep the last frame, do not go home.
                holding = true;
            }
            if (holding) {
                captureFresh(minecraft);
            }
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof io.github.littlebird.entity.ArtilleryPlatform)
                || !(minecraft.player.getVehicle()
                        instanceof io.github.littlebird.entity.ground.GroundVehicleControls platform)
                || !platform.isDriver(platform.seatOf(minecraft.player))) {
            return;
        }
        captureFresh(minecraft);
    }

    /** Locks onto a freshly launched round of ours, unless it is the one dismissed with C. */
    private static void captureFresh(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null
                || minecraft.level.getGameTime() % 4L != 0L) {
            return;
        }
        long now = minecraft.level.getGameTime();
        if (now - lastCaptureTick < BURST_TICKS) {
            // Still the same salvo: keep the lens where it is instead of hopping to a newer round.
            return;
        }
        AABB box = minecraft.player.getBoundingBox().inflate(CAPTURE_RANGE);
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity rocket : minecraft.level.getEntitiesOfClass(Entity.class, box)) {
            if (!(rocket instanceof io.github.littlebird.entity.TrackedRound)) {
                continue;
            }
            if (!rocket.isAlive() || rocket.tickCount > FRESH_TICKS
                    || rocket.getId() == dismissedId) {
                continue;
            }
            double distance = rocket.distanceToSqr(minecraft.player);
            if (distance < bestDistance) {
                best = rocket;
                bestDistance = distance;
            }
        }
        if (best != null) {
            targetId = best.getId();
            lastCaptureTick = now;
            holding = false;
            primed = false;
            yaw = best.getYRot();
            pitch = best.getXRot();
            follow(best);
        }
    }

    /** Smoothly trails the round from behind and above. */
    private static void follow(Entity rocket) {
        Vec3 direction = Vec3.directionFromRotation(rocket.getXRot(), rocket.getYRot());
        double targetX = rocket.getX() - direction.x * DISTANCE;
        double targetY = rocket.getY() - direction.y * DISTANCE + LIFT;
        double targetZ = rocket.getZ() - direction.z * DISTANCE;
        if (!primed) {
            x = targetX;
            y = targetY;
            z = targetZ;
            primed = true;
        } else {
            x = Mth.lerp(SMOOTHING, x, targetX);
            y = Mth.lerp(SMOOTHING, y, targetY);
            z = Mth.lerp(SMOOTHING, z, targetZ);
        }
        yaw = Mth.rotLerp(SMOOTHING, yaw, rocket.getYRot());
        pitch = Mth.lerp(SMOOTHING, pitch, rocket.getXRot());
    }

    public static void stop() {
        targetId = -1;
        holding = false;
        primed = false;
    }

    public static double[] cameraPosition() {
        return new double[] {x, y, z};
    }

    public static float cameraYawDeg() {
        return yaw;
    }

    public static float cameraPitchDeg() {
        return pitch;
    }

    public static double fov() {
        return 72.0D;
    }

    /** The follow camera is the lens while it runs, over any vehicle view. */
    @SubscribeEvent
    public static void computeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!isActive()) {
            return;
        }
        event.setYaw(yaw);
        event.setPitch(pitch);
        event.setRoll(0.0F);
    }

    @SubscribeEvent
    public static void computeFov(ViewportEvent.ComputeFov event) {
        if (!isActive()) {
            return;
        }
        event.setFOV((float) fov());
    }
}
