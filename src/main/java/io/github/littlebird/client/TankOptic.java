package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.MainBattleTankEntity;
import io.github.littlebird.entity.MainGunShellEntity;
import io.github.littlebird.entity.ground.MainGun;
import io.github.littlebird.entity.seat.GroundSeat;
import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import io.github.littlebird.entity.weapon.WeaponSelection;
import io.github.littlebird.registry.ModEntities;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/** Samples the gun and rangefinder once per client tick and projects their markers every frame. */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class TankOptic {
    private static final double LASER_LIMIT = 500.0D;
    private static final double LASER_STEP = 16.0D;
    private static final double BORE_REFERENCE_RANGE = 64.0D;
    private static final AimProjection.Screen HIDDEN = new AimProjection.Screen(0.0D, 0.0D, false);
    private static final Matrix4f viewProjection = new Matrix4f();
    private static MainBattleTankEntity activeTank;
    private static MainGunAmmoType sampledAmmo;
    private static long sampledTick;
    private static Vec3 eye = Vec3.ZERO;
    private static float verticalFocalScale;
    private static Reading reading;

    public enum ImpactKind {
        TERRAIN, ENTITY, UNLOADED, NO_IMPACT, CANISTER
    }

    public record Flight(Vec3 point, double rangeBlocks, double ticks, ImpactKind kind) {
        public boolean hasImpact() {
            return kind == ImpactKind.TERRAIN || kind == ImpactKind.ENTITY;
        }
    }

    public record Reading(Vec3 borePoint, double laserRangeBlocks, Flight flight) {
    }

    record SegmentHit(Vec3 point, ImpactKind kind) {
    }

    @FunctionalInterface
    interface SegmentTrace {
        /** A null result means that the segment was clear. */
        SegmentHit trace(Vec3 start, Vec3 end);
    }

    private TankOptic() {
    }

    public static boolean active(MainBattleTankEntity tank, Player player) {
        return tank.isInteriorView() && GroundSeat.isTankDriver(tank.seatOf(player))
                && tank.getSelectedWeapon() == WeaponSelection.MAIN_GUN;
    }

    public static Reading reading(MainBattleTankEntity tank) {
        return activeTank == tank ? reading : null;
    }

    public static double focalLengthPixels(int height) {
        return verticalFocalScale * height * 0.5D;
    }

    @SubscribeEvent
    public static void capture(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen != null
                || !(minecraft.player.getVehicle() instanceof MainBattleTankEntity tank)
                || !active(tank, minecraft.player)) {
            activeTank = null;
            reading = null;
            return;
        }

        Camera camera = event.getCamera();
        eye = camera.getPosition();
        verticalFocalScale = Math.abs(event.getProjectionMatrix().m11());
        viewProjection.set(event.getProjectionMatrix()).mul(event.getPoseStack().last().pose());

        MainGunAmmoType ammo = MainGunAmmoType.byOrdinal(tank.getShellTypeOrdinal());
        long tick = minecraft.level.getGameTime();
        if (tank != activeTank || tick != sampledTick || ammo != sampledAmmo) {
            reading = sample(minecraft.level, minecraft.player, tank, ammo, camera);
            activeTank = tank;
            sampledTick = tick;
            sampledAmmo = ammo;
        }
    }

    public static AimProjection.Screen project(Vec3 point, int width, int height) {
        return project(viewProjection, point.subtract(eye), width, height);
    }

    static AimProjection.Screen project(Matrix4fc matrix, Vec3 relative, int width, int height) {
        Vector4f clip = matrix.transform(new Vector4f((float) relative.x, (float) relative.y,
                (float) relative.z, 1.0F));
        if (!(clip.w > 0.0F) || clip.z < -clip.w || clip.z > clip.w
                || !Float.isFinite(clip.x) || !Float.isFinite(clip.y) || !Float.isFinite(clip.w)) {
            return HIDDEN;
        }
        return new AimProjection.Screen((0.5D + 0.5D * clip.x / clip.w) * width,
                (0.5D - 0.5D * clip.y / clip.w) * height, true);
    }

    private static Reading sample(ClientLevel level, Player player, MainBattleTankEntity tank,
                                  MainGunAmmoType ammo, Camera camera) {
        double yaw = tank.getTurretYawDeg();
        double pitch = tank.getTurretPitchDeg();
        Vec3 muzzle = tank.mainGunMuzzle(yaw, pitch);
        Vector3 aim = MainGun.aimDirection(yaw, pitch);
        Vec3 aimVector = new Vec3(aim.x(), aim.y(), aim.z());
        Vec3 laserDirection = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());
        double range = laserRange(level, player, tank, camera.getPosition(), laserDirection);
        double boreDistance = Double.isFinite(range)
                ? Math.max(1.0D, range - muzzle.subtract(camera.getPosition()).dot(laserDirection))
                : BORE_REFERENCE_RANGE;
        Vec3 borePoint = muzzle.add(aimVector.scale(boreDistance));

        Flight flight = new Flight(null, 0.0D, 0.0D, ImpactKind.CANISTER);
        if (ammo != MainGunAmmoType.CANISTER) {
            EntityDimensions shell = ModEntities.MAIN_GUN_SHELL.get().getDimensions();
            flight = predict(muzzle, aim, ammo, (start, end) ->
                    traceRound(level, player, tank, shell.makeBoundingBox(start), start, end));
        }
        return new Reading(borePoint, range, flight);
    }

    private static double laserRange(ClientLevel level, Player player, MainBattleTankEntity tank,
                                     Vec3 origin, Vec3 direction) {
        Vec3 start = origin;
        for (double distance = LASER_STEP; ; distance += LASER_STEP) {
            Vec3 end = origin.add(direction.scale(Math.min(distance, LASER_LIMIT)));
            if (!loaded(level, start, end)) {
                return Double.NaN;
            }
            HitResult hit = MainGunShellEntity.trace(level, null, player, tank,
                    new AABB(start, start), start, end);
            if (hit.getType() != HitResult.Type.MISS) {
                return origin.distanceTo(hit.getLocation());
            }
            if (distance >= LASER_LIMIT) {
                return Double.NaN;
            }
            start = end;
        }
    }

    private static SegmentHit traceRound(ClientLevel level, Player player, MainBattleTankEntity tank,
                                         AABB bounds, Vec3 start, Vec3 end) {
        if (!loaded(level, start, end)) {
            return new SegmentHit(null, ImpactKind.UNLOADED);
        }
        HitResult hit = MainGunShellEntity.trace(level, null, player, tank, bounds, start, end);
        if (hit.getType() != HitResult.Type.MISS) {
            return new SegmentHit(hit.getLocation(), hit.getType() == HitResult.Type.ENTITY
                    ? ImpactKind.ENTITY : ImpactKind.TERRAIN);
        }
        // The server also clips the shell's full box when its centre ray has no hit.
        Vec3 motion = end.subtract(start);
        Vec3 clipped = Entity.collideBoundingBox(null, motion, bounds, level, List.of());
        return motion.equals(clipped) ? null
                : new SegmentHit(start.add(clipped), ImpactKind.TERRAIN);
    }

    private static boolean loaded(ClientLevel level, Vec3 start, Vec3 end) {
        return level.hasChunksAt(Mth.floor(Math.min(start.x, end.x)),
                Mth.floor(Math.min(start.z, end.z)), Mth.floor(Math.max(start.x, end.x)),
                Mth.floor(Math.max(start.z, end.z)));
    }

    static Flight predict(Vec3 muzzle, Vector3 aim, MainGunAmmoType ammo, SegmentTrace trace) {
        Vector3 velocity = MainGunShellBallistics.launchVelocity(aim, ammo);
        Vec3 start = muzzle;
        for (int tick = 0; tick < MainGunShellBallistics.LIFETIME_TICKS; tick++) {
            velocity = MainGunShellBallistics.advance(velocity);
            Vec3 end = start.add(velocity.x(), velocity.y(), velocity.z());
            SegmentHit hit = trace.trace(start, end);
            if (hit != null) {
                Vec3 point = hit.point();
                if (point == null) {
                    return new Flight(null, 0.0D, 0.0D, hit.kind());
                }
                double fraction = Mth.clamp(start.distanceTo(point) / start.distanceTo(end), 0.0D, 1.0D);
                return new Flight(point, muzzle.distanceTo(point), tick + fraction, hit.kind());
            }
            start = end;
        }
        return new Flight(null, 0.0D, 0.0D, ImpactKind.NO_IMPACT);
    }
}
