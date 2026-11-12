package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ArtilleryStrike;
import io.github.littlebird.entity.GunshipRider;
import io.github.littlebird.entity.Phl191Entity;
import io.github.littlebird.entity.weapon.Phl191Ballistics;
import io.github.littlebird.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tactical radar picture for the PHL-191: contacts inside the strike radius and the firing solution.
 *
 * <p>M toggles a circular map centred on the hull. Contacts are red dots (vehicles larger than
 * infantry); the predicted impact point is a gold cross; and the crew gets the one number that
 * actually matters for a rocket launcher - the elevation needed to put a round on the nearest
 * target the hull is pointing at, next to the elevation it is currently laid at.
 *
 * <p>Pure data: it reads the level and the launcher state, the HUD does all the drawing.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class Phl191Recon {
    /** Radius of the picture, in blocks. */
    public static final double RADIUS = ArtilleryStrike.PHL191_RANGE;
    /** Contacts closer than this bearing to the hull's nose count as "the target". */
    private static final double TARGET_CONE_DEG = 15.0D;
    /** Ticks between two refreshes. */
    private static final int REFRESH_TICKS = 4;

    /** One contact, relative to the hull. */
    public record Dot(double x, double z, boolean vehicle) {
    }

    /** The contact the hull is pointed at, and the elevation that would reach it. */
    public record Target(double x, double z, double distance, double requiredElevationDeg) {
    }

    private static boolean active;
    private static List<Dot> dots = List.of();
    private static Target target;
    private static boolean hasImpact;
    private static double impactX;
    private static double impactZ;
    private static double elevationDeg;
    private static float vehicleYaw;
    private static int counter;

    private Phl191Recon() {
    }

    public static boolean isActive() {
        return active;
    }

    public static List<Dot> dots() {
        return dots;
    }

    public static Target target() {
        return target;
    }

    public static boolean hasImpact() {
        return hasImpact;
    }

    public static double impactX() {
        return impactX;
    }

    public static double impactZ() {
        return impactZ;
    }

    public static double elevationDeg() {
        return elevationDeg;
    }

    public static float vehicleYaw() {
        return vehicleYaw;
    }

    /** Refresh the command board even when the regular radar is off. */
    public static void refreshNow() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null
                && minecraft.player.getVehicle() instanceof Phl191Entity launcher
                && ArtilleryCommand.isDriving(launcher, minecraft.player)) {
            capture(minecraft, launcher);
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null
                || !(minecraft.player.getVehicle() instanceof Phl191Entity launcher)
                || !launcher.isDriver(launcher.seatOf(minecraft.player))) {
            active = false;
            dots = List.of();
            target = null;
            hasImpact = false;
            return;
        }
        while (LittleBirdKeyMappings.PHL191_RECON.consumeClick()) {
            active = !active;
            minecraft.player.playSound(ModSounds.RADAR_PING.get(), 0.9F, active ? 1.0F : 0.7F);
        }
        if (!active || counter++ % REFRESH_TICKS != 0) {
            return;
        }
        capture(minecraft, launcher);
    }

    private static void capture(Minecraft minecraft, Phl191Entity launcher) {
        Vec3 centre = launcher.position();
        vehicleYaw = launcher.getYRot();
        elevationDeg = launcher.getElevationDeg();
        AABB box = new AABB(centre.x - RADIUS, centre.y - 48.0D, centre.z - RADIUS,
                centre.x + RADIUS, centre.y + 48.0D, centre.z + RADIUS);
        List<Dot> found = new ArrayList<>();
        Entity best = null;
        double bestDistance = Double.MAX_VALUE;
        double bestBearing = TARGET_CONE_DEG;
        double forwardX = -Math.sin(Math.toRadians(vehicleYaw));
        double forwardZ = Math.cos(Math.toRadians(vehicleYaw));
        for (Entity entity : minecraft.level.getEntitiesOfClass(Entity.class, box)) {
            if (entity == launcher || entity == minecraft.player || !entity.isAlive()
                    || entity.isSpectator()) {
                continue;
            }
            boolean vehicle = entity instanceof GunshipRider;
            if (!vehicle && !(entity instanceof LivingEntity)) {
                continue;
            }
            double dx = entity.getX() - centre.x;
            double dz = entity.getZ() - centre.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (horizontal > RADIUS || horizontal < 0.5D) {
                continue;
            }
            found.add(new Dot(dx, dz, vehicle));
            // Nearest contact inside the bearing cone: the picture's "aimed at" target.
            double bearing = Math.toDegrees(Math.acos(Math.max(-1.0D,
                    Math.min(1.0D, (dx * forwardX + dz * forwardZ) / horizontal))));
            if ((entity instanceof LivingEntity || vehicle) && horizontal < bestDistance
                    && Math.abs(bearing) <= TARGET_CONE_DEG && bearing <= bestBearing) {
                best = entity;
                bestDistance = horizontal;
                bestBearing = bearing;
            }
        }
        dots = List.copyOf(found);
        if (best != null) {
            double dx = best.getX() - centre.x;
            double dz = best.getZ() - centre.z;
            double distance = Math.sqrt(dx * dx + dz * dz);
            target = new Target(dx, dz, distance,
                    Phl191Ballistics.requiredElevation(distance));
        } else {
            target = null;
        }
        Phl191Aim.Solution solution = Phl191Aim.solution();
        hasImpact = solution.valid();
        if (hasImpact) {
            impactX = solution.point().x - centre.x;
            impactZ = solution.point().z - centre.z;
        }
    }
}
