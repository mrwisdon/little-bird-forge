package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.LittleBirdGeometry;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.LittleBirdRocketBallistics;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Works out where the rockets will land and projects that point onto the screen, so the
 * HUD can put a marker on the predicted impact. The machine gun cross no longer follows
 * the impact: it is drawn fixed at the centre of the screen (see centreCrosshair).
 *
 * Nothing is drawn in the world: only the projected markers are handed to HudAir.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LittleBirdAimRenderer {
    public record Reticle(double x, double y, boolean visible) {
        public static final Reticle HIDDEN = new Reticle(0.0D, 0.0D, false);
    }

    /**
     * Battlefield-style gun crosshair: pinned to the centre of the screen, with the arm
     * gap widening with the barrel's current dispersion.
     */
    public record GunReticle(double x, double y, boolean visible, double gapPixels) {
        public static final GunReticle HIDDEN = new GunReticle(0.0D, 0.0D, false, 0.0D);
    }

    private static Vec3 rocketImpact;
    private static GunReticle gunReticle = GunReticle.HIDDEN;
    private static Reticle rocketReticle = Reticle.HIDDEN;
    private static long predictedTick = Long.MIN_VALUE;
    private static double smoothedRocketX = Double.NaN;
    private static double smoothedRocketY = Double.NaN;

    private LittleBirdAimRenderer() {
    }

    public static GunReticle gunReticle() {
        return gunReticle;
    }

    public static Reticle rocketReticle() {
        return rocketReticle;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof LittleBirdEntity helicopter)
                || !helicopter.isPilot(minecraft.player)) {
            // Passengers get no reticles: they have no weapons to point them with.
            clear();
            return;
        }
        if (minecraft.level.getGameTime() != predictedTick) {
            predictedTick = minecraft.level.getGameTime();
            predict(minecraft, helicopter);
        }
        project(minecraft, event.getCamera(), helicopter);
    }

    private static void clear() {
        rocketImpact = null;
        gunReticle = GunReticle.HIDDEN;
        rocketReticle = Reticle.HIDDEN;
        smoothedRocketX = Double.NaN;
        smoothedRocketY = Double.NaN;
    }

    private static void predict(Minecraft minecraft, LittleBirdEntity helicopter) {
        rocketImpact = rocketImpact(minecraft, helicopter,
                Vec3.directionFromRotation(helicopter.getXRot(), helicopter.getYRot()));
    }

    /** Predicted impact of the 70mm rockets, using the server trajectory code. */
    private static Vec3 rocketImpact(
            Minecraft minecraft, LittleBirdEntity helicopter, Vec3 aim) {
        Vec3 position = helicopter.position()
                .add(0.0D, LittleBirdGeometry.podHeightBlocks(), 0.0D)
                .add(aim.scale(LittleBirdGeometry.podForwardBlocks()));
        LittleBirdBallistics.Vector3 velocity =
                LittleBirdRocketBallistics.launchVelocity(vector(aim));
        for (int tick = 1; tick <= LittleBirdRocketBallistics.LIFETIME_TICKS; tick++) {
            velocity = LittleBirdRocketBallistics.advance(velocity, vector(aim));
            Vec3 next = position.add(velocity.x(), velocity.y(), velocity.z());
            BlockHitResult hit = minecraft.level.clip(new ClipContext(
                    position, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, helicopter));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getLocation();
            }
            position = next;
        }
        return position;
    }

    private static void project(
            Minecraft minecraft, Camera camera, LittleBirdEntity helicopter) {
        Vec3 eye = camera.getPosition();
        AimProjection.Vector eyeVector = new AimProjection.Vector(eye.x, eye.y, eye.z);
        float roll = helicopter.getRenderRollDeg()
                * (float) LittleBirdConfig.cameraRollFactor();
        double fov = minecraft.options.fov().get() * (helicopter.isCockpitView()
                ? LittleBirdConfig.cockpitFovMultiplier()
                : LittleBirdConfig.thirdPersonFovMultiplier());
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        gunReticle = centreCrosshair(helicopter, fov, width, height);
        rocketReticle = project(rocketImpact, eyeVector, camera, roll, fov, width, height);
    }

    /**
     * The machine gun cross is drawn fixed at the centre of the screen.
     *
     * Tracking the projected impact point only worked when the camera sat well behind the
     * muzzle line: in the cockpit view the camera is ahead of and above the muzzles, so
     * close impacts project far below the frame and the cross kept sliding off screen, and
     * in the third person view it was pinned to whatever block the bullets happened to
     * reach. A centre cross is always readable and answers the question that matters while
     * firing - is the nose pointing where I want it - in both views. The arms still open
     * with barrel heat, so the dispersion is not lost: they show the cone's radius at the
     * convergence distance.
     */
    private static GunReticle centreCrosshair(
            LittleBirdEntity helicopter, double fov, int width, int height) {
        double spread = LittleBirdBallistics.spreadDegrees(helicopter.getGunHeat());
        double gap = LittleBirdCrosshairMath.crosshairGapPixels(spread,
                LittleBirdBallistics.CONVERGENCE_DISTANCE, fov, height);
        return new GunReticle(width / 2.0D, height / 2.0D, true, gap);
    }

    private static Reticle project(
            Vec3 point, AimProjection.Vector eye, Camera camera,
            float roll, double fov, int width, int height) {
        if (point == null) {
            return Reticle.HIDDEN;
        }
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(point.x, point.y, point.z), eye,
                camera.getYRot(), camera.getXRot(), roll, fov, width, height);
        boolean behind = LittleBirdCrosshairMath.behindCamera(screen.visible());
        LittleBirdCrosshairMath.ScreenPoint placed = LittleBirdCrosshairMath.clampToFrame(
                behind ? width / 2.0D : screen.x(),
                behind ? height / 2.0D : screen.y(), width, height);
        double targetX = placed.x();
        double targetY = placed.y();
        if (Double.isNaN(smoothedRocketX)) {
            smoothedRocketX = targetX;
            smoothedRocketY = targetY;
        }
        smoothedRocketX = LittleBirdCrosshairMath.smooth(smoothedRocketX, targetX,
                LittleBirdCrosshairMath.SMOOTHING_FACTOR);
        smoothedRocketY = LittleBirdCrosshairMath.smooth(smoothedRocketY, targetY,
                LittleBirdCrosshairMath.SMOOTHING_FACTOR);
        return new Reticle(smoothedRocketX, smoothedRocketY, true);
    }

    private static LittleBirdBallistics.Vector3 vector(Vec3 value) {
        return new LittleBirdBallistics.Vector3(value.x, value.y, value.z);
    }
}