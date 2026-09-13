package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.ApacheGeometry;
import io.github.littlebird.entity.weapon.ApachePods;
import io.github.littlebird.entity.weapon.ChainGunTurret;
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
 * Projects two things onto the screen: the chain gun's own aim, so the front seat gets a reticle
 * showing where the barrel points rather than where the nose points, and the 70mm rockets'
 * predicted impact, so the pilot gets the same rocket sight the Little Bird has. The turret
 * angle is server authoritative, so this mirrors what the aircraft is really doing, lag and all.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ApacheAimRenderer {
    /** Distance the projected sight point sits at: far enough to read as a direction. */
    private static final double SIGHT_DISTANCE = 96.0D;

    public record Reticle(double x, double y, boolean visible, boolean pegged) {
        public static final Reticle HIDDEN = new Reticle(0.0D, 0.0D, false, false);
    }

    private static Reticle reticle = Reticle.HIDDEN;
    private static double smoothedX = Double.NaN;
    private static double smoothedY = Double.NaN;
    private static Reticle rocketReticle = Reticle.HIDDEN;
    private static double rocketX = Double.NaN;
    private static double rocketY = Double.NaN;
    private static Vec3 rocketImpact;
    private static long predictedTick = Long.MIN_VALUE;

    private ApacheAimRenderer() {
    }

    public static Reticle reticle() {
        return reticle;
    }

    /** Predicted 70mm impact, for the pilot's rocket sight. */
    public static Reticle rocketReticle() {
        return rocketReticle;
    }

    private static void clear() {
        reticle = Reticle.HIDDEN;
        smoothedX = Double.NaN;
        smoothedY = Double.NaN;
        rocketReticle = Reticle.HIDDEN;
        rocketX = Double.NaN;
        rocketY = Double.NaN;
        rocketImpact = null;
        predictedTick = Long.MIN_VALUE;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof ApacheEntity apache)) {
            clear();
            return;
        }
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        if (minecraft.level.getGameTime() != predictedTick) {
            predictedTick = minecraft.level.getGameTime();
            rocketImpact = rocketImpact(minecraft, apache);
        }
        float roll = apache.getRenderRollDeg() * (float) LittleBirdConfig.cameraRollFactor();
        double fov = minecraft.options.fov().get() * (apache.isCockpitView()
                ? LittleBirdConfig.cockpitFovMultiplier()
                : LittleBirdConfig.thirdPersonFovMultiplier());
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        LittleBirdBallistics.Vector3 direction = ChainGunTurret.aimDirection(
                apache.getTurretAimYawDeg(), apache.getTurretAimPitchDeg());
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(
                        eye.x + direction.x() * SIGHT_DISTANCE,
                        eye.y + direction.y() * SIGHT_DISTANCE,
                        eye.z + direction.z() * SIGHT_DISTANCE),
                new AimProjection.Vector(eye.x, eye.y, eye.z),
                camera.getYRot(), camera.getXRot(), roll, fov, width, height);

        double targetX = screen.visible() ? screen.x() : width / 2.0D;
        double targetY = screen.visible() ? screen.y() : height / 2.0D;
        LittleBirdCrosshairMath.ScreenPoint placed =
                LittleBirdCrosshairMath.clampToFrame(targetX, targetY, width, height);
        if (Double.isNaN(smoothedX)) {
            smoothedX = placed.x();
            smoothedY = placed.y();
        }
        smoothedX = LittleBirdCrosshairMath.smooth(
                smoothedX, placed.x(), LittleBirdCrosshairMath.SMOOTHING_FACTOR);
        smoothedY = LittleBirdCrosshairMath.smooth(
                smoothedY, placed.y(), LittleBirdCrosshairMath.SMOOTHING_FACTOR);
        reticle = new Reticle(smoothedX, smoothedY, true,
                ChainGunTurret.atElevationStop(apache.getTurretAimPitchDeg()));
        rocketReticle = projectRocket(rocketImpact, eye, camera, roll, fov, width, height);
    }

    /**
     * Predicted impact of the 70mm rockets. The launch point and the flight code are the ones
     * the server uses - down to which pod fires next - or the sight would lie by a pod's width.
     */
    private static Vec3 rocketImpact(Minecraft minecraft, ApacheEntity apache) {
        Vec3 aim = Vec3.directionFromRotation(apache.getXRot(), apache.getYRot());
        Vec3 right = Vec3.directionFromRotation(0.0F, apache.getYRot() + 90.0F);
        double side = ApachePods.launchLateralBlocks(
                apache.getReadyRockets(), ApacheGeometry.podLateralBlocks());
        Vec3 position = apache.position()
                .add(0.0D, ApacheGeometry.podHeightBlocks(), 0.0D)
                .add(right.scale(side))
                .add(aim.scale(ApacheGeometry.podForwardBlocks()));
        LittleBirdBallistics.Vector3 direction = vector(aim);
        LittleBirdBallistics.Vector3 velocity =
                LittleBirdRocketBallistics.launchVelocity(direction);
        for (int tick = 1; tick <= LittleBirdRocketBallistics.LIFETIME_TICKS; tick++) {
            velocity = LittleBirdRocketBallistics.advance(velocity, direction);
            Vec3 next = position.add(velocity.x(), velocity.y(), velocity.z());
            BlockHitResult hit = minecraft.level.clip(new ClipContext(
                    position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, apache));
            if (hit.getType() != HitResult.Type.MISS) {
                return hit.getLocation();
            }
            position = next;
        }
        return position;
    }

    /** Projects the predicted impact, clamped to the frame and smoothed like the turret. */
    private static Reticle projectRocket(Vec3 point, Vec3 eye, Camera camera,
                                         float roll, double fov, int width, int height) {
        if (point == null) {
            return Reticle.HIDDEN;
        }
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(point.x, point.y, point.z),
                new AimProjection.Vector(eye.x, eye.y, eye.z),
                camera.getYRot(), camera.getXRot(), roll, fov, width, height);
        boolean behind = LittleBirdCrosshairMath.behindCamera(screen.visible());
        LittleBirdCrosshairMath.ScreenPoint placed = LittleBirdCrosshairMath.clampToFrame(
                behind ? width / 2.0D : screen.x(),
                behind ? height / 2.0D : screen.y(), width, height);
        if (Double.isNaN(rocketX)) {
            rocketX = placed.x();
            rocketY = placed.y();
        }
        rocketX = LittleBirdCrosshairMath.smooth(
                rocketX, placed.x(), LittleBirdCrosshairMath.SMOOTHING_FACTOR);
        rocketY = LittleBirdCrosshairMath.smooth(
                rocketY, placed.y(), LittleBirdCrosshairMath.SMOOTHING_FACTOR);
        return new Reticle(rocketX, rocketY, true, false);
    }

    private static LittleBirdBallistics.Vector3 vector(Vec3 value) {
        return new LittleBirdBallistics.Vector3(value.x, value.y, value.z);
    }
}
