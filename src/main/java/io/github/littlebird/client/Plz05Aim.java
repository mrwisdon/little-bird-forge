package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.Plz05Entity;
import io.github.littlebird.entity.weapon.Plz05Ballistics;
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
 * Where the salvo will land.
 *
 * <p>Steps the very same {@link Plz05Ballistics} the server flies the rocket with, one tick at a
 * time, against the client's own world, so the ground marker cannot drift from the real impact.
 * The result is handed to the HUD as a screen point plus a range and a time of flight; nothing is
 * drawn into the world here.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Plz05Aim {
    /** One prediction: impact point, how far it is, and how long it takes to get there. */
    public record Solution(Vec3 point, double rangeBlocks, double flightTicks, boolean hitBlock) {
        public static final Solution NONE = new Solution(Vec3.ZERO, 0.0D, 0.0D, false);

        public boolean valid() {
            return flightTicks > 0.0D;
        }
    }

    /** Screen placement of the impact marker. */
    public record Reticle(double x, double y, boolean visible) {
        public static final Reticle HIDDEN = new Reticle(0.0D, 0.0D, false);
    }

    private static Solution solution = Solution.NONE;
    private static Reticle reticle = Reticle.HIDDEN;
    private static long predictedTick = Long.MIN_VALUE;

    private Plz05Aim() {
    }

    public static Solution solution() {
        return solution;
    }

    public static Reticle reticle() {
        return reticle;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof Plz05Entity launcher)
                || !launcher.isDriver(launcher.seatOf(minecraft.player))) {
            clear();
            return;
        }
        long time = minecraft.level.getGameTime();
        if (time != predictedTick) {
            predictedTick = time;
            solution = predict(minecraft, launcher);
        }
        reticle = project(minecraft, event.getCamera(), solution.point());
    }

    private static void clear() {
        solution = Solution.NONE;
        reticle = Reticle.HIDDEN;
        predictedTick = Long.MIN_VALUE;
    }

    /** Flies the round forward on this client until it meets a block or runs out of life. */
    private static Solution predict(Minecraft minecraft, Plz05Entity launcher) {
        double elevation = launcher.getElevationDeg();
        Plz05Ballistics.V3 direction =
                Plz05Ballistics.direction(launcher.getYRot(), elevation);
        Vec3 origin = launcher.muzzleOrigin(elevation);
        Vec3 position = origin;
        Plz05Ballistics.V3 velocity = Plz05Ballistics.launchVelocity(direction);
        for (int tick = 1; tick <= Plz05Ballistics.LIFETIME_TICKS; tick++) {
            velocity = Plz05Ballistics.advance(velocity, direction, tick);
            Vec3 next = position.add(velocity.x(), velocity.y(), velocity.z());
            BlockHitResult hit = minecraft.level.clip(new ClipContext(
                    position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, launcher));
            if (hit.getType() != HitResult.Type.MISS) {
                Vec3 point = hit.getLocation();
                return new Solution(point, origin.distanceTo(point), tick, true);
            }
            position = next;
        }
        return new Solution(position, origin.distanceTo(position),
                Plz05Ballistics.LIFETIME_TICKS, false);
    }

    private static Reticle project(Minecraft minecraft, Camera camera, Vec3 point) {
        if (point == null) {
            return Reticle.HIDDEN;
        }
        Vec3 eye = camera.getPosition();
        double fov = minecraft.options.fov().get();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(point.x, point.y, point.z),
                new AimProjection.Vector(eye.x, eye.y, eye.z),
                camera.getYRot(), camera.getXRot(), 0.0F, fov, width, height);
        if (!screen.visible()) {
            return Reticle.HIDDEN;
        }
        return new Reticle(screen.x(), screen.y(), true);
    }
}
