package io.github.littlebird.entity.ground;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.network.MainGunImpactPacket;
import io.github.littlebird.network.ModNetwork;
import io.github.littlebird.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * The server half of a 120mm impact: one call turns a hit point into a hard, readable hit.
 *
 * <p>A 120mm round used to land as a single puff anyone could mistake for a firecracker. This class
 * paints the pieces that make the hit read as a heavy strike - shrapnel thrown along the surface
 * normal, a dust column, a shock ring lying in the impact plane, a flash - and then carries the
 * feel to the players themselves: the blast, a layered crack, and one short camera shock per nearby
 * client. Damage is deliberately not here: the shell entity still owns the hit resolution, so the
 * same impact can never be damaged twice.
 *
 * <p>Every entry point tolerates junk (null, non-finite, negative scale) because a projectile can
 * be removed mid-tick and hand this class a half-built position.
 */
public final class MainGunImpactFx {
    /** Players inside this radius get the camera shock packet; the packet carries the falloff. */
    public static final double SHAKE_RADIUS = 64.0D;

    /** The deep boom the round lands with. */
    private static final float BLAST_VOLUME = 1.6F;
    private static final float BLAST_PITCH = 0.7F;

    /** The hard, low crack laid on top, so the boom has an attack instead of only a tail. */
    private static final float CRACK_VOLUME = 1.3F;
    private static final float CRACK_PITCH = 0.6F;

    /** Particle bases, all multiplied by the configured particle scale like the muzzle flash. */
    private static final int FRAGMENT_BASE = 16;
    private static final int SPARK_BASE = 10;
    private static final int COLUMN_STEPS = 4;
    private static final int COLUMN_BASE = 5;
    private static final int RING_LAYERS = 2;
    private static final int RING_SAMPLES = 10;
    private static final int FLASH_BASE = 14;

    /** A splash radius above this is treated as a config mistake rather than an area kill. */
    private static final double MAX_SPLASH_RADIUS = 32.0D;

    /** Top of the camera shock config range; the client clamps the accumulated trauma at 1. */
    private static final double MAX_SHAKE_INTENSITY = 2.0D;

    /** Ceiling on the visual broadcast radius, so a bad config value cannot walk the whole map. */
    private static final double MAX_VISIBLE_DISTANCE = 2048.0D;

    private MainGunImpactFx() {
    }

    /**
     * Spawns the whole hard-impact package at {@code point}.
     *
     * @param server        level the shell died in
     * @param shell         the shell itself, or null when it is already gone; passed on as the
     *                      explosion source so knockback and damage attribution stay with the round
     * @param point         impact position
     * @param normal        surface normal at the impact, or null for an air burst
     * @param particleScale multiplier on every particle count, from the server config
     * @param splashRadius  explosion radius in blocks
     * @param breakBlocks   whether terrain damage is allowed; damage to entities always applies
     */
    public static void detonate(ServerLevel server, Entity shell, Vec3 point, Vec3 normal,
            double particleScale, double splashRadius, boolean breakBlocks) {
        if (server == null || point == null || !finite(point)) {
            return;
        }
        double scale = scaleOf(particleScale);
        double spatial = clamp(scale, 0.5D, 2.0D);
        Vec3 axis = axisOf(normal);
        BlockState debris = surfaceState(server, point, axis);

        flash(server, point, scale, spatial);
        ejecta(server, point, axis, scale, debris);
        dustColumn(server, point, scale, spatial);
        shockRing(server, point, axis, scale, spatial);
        boom(server, shell, point, splashRadius, breakBlocks);
        sounds(server, point);
        broadcast(server, point);
    }

    /** The bright core every hard hit needs; short lived, but it carries the frame. */
    private static void flash(ServerLevel server, Vec3 point, double scale, double spatial) {
        server.sendParticles(ParticleTypes.EXPLOSION_EMITTER, point.x, point.y, point.z, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                count(FLASH_BASE, scale), 0.5D * spatial, 0.5D * spatial, 0.5D * spatial, 0.06D);
    }

    /**
     * Shrapnel and sparks thrown along the surface normal.
     *
     * <p>The spread box is stretched along the normal axis and squeezed across it, which is the one
     * way to aim vanilla particles: their velocity is random, so the direction has to come from
     * where they are allowed to appear. Without a normal the box stays a sphere, which is the honest
     * look for a round that went off in the air.
     */
    private static void ejecta(ServerLevel server, Vec3 point, Vec3 axis, double scale,
            BlockState debris) {
        double across = 0.7D * scale;
        double along = 1.5D * scale;
        double sx = axis == null ? across : Math.abs(axis.x) * along + (1.0D - Math.abs(axis.x)) * across;
        double sy = axis == null ? across : Math.abs(axis.y) * along + (1.0D - Math.abs(axis.y)) * across;
        double sz = axis == null ? across : Math.abs(axis.z) * along + (1.0D - Math.abs(axis.z)) * across;
        // Lift the emission centre a little off the surface so the cone does not start inside it.
        Vec3 centre = axis == null ? point : point.add(axis.scale(Math.min(0.9D, 0.35D * scale)));
        server.sendParticles(ParticleTypes.CRIT, centre.x, centre.y, centre.z,
                count(SPARK_BASE, scale), sx, sy, sz, 0.28D);
        if (debris != null) {
            // Real block chips read as dirt and stone coming off the crater, not as glowing dots.
            ParticleOptions chips = new BlockParticleOption(ParticleTypes.BLOCK, debris);
            server.sendParticles(chips, centre.x, centre.y, centre.z,
                    count(FRAGMENT_BASE, scale), sx, sy, sz, 0.35D);
        }
        server.sendParticles(ParticleTypes.SMOKE, centre.x, centre.y, centre.z,
                count(4, scale), sx, sy, sz, 0.04D);
    }

    /**
     * The dust column. Campfire smoke already drifts upward on its own, so a short stack of those
     * plus large smoke builds a pillar without any per-tick bookkeeping.
     */
    private static void dustColumn(ServerLevel server, Vec3 point, double scale, double spatial) {
        for (int step = 0; step < COLUMN_STEPS; step++) {
            double lift = 0.5D + step * 0.65D * spatial;
            server.sendParticles(ParticleTypes.LARGE_SMOKE, point.x, point.y + lift, point.z,
                    count(COLUMN_BASE, scale), 0.35D * spatial, 0.15D, 0.35D * spatial, 0.02D);
            server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, point.x, point.y + lift, point.z,
                    1, 0.3D * spatial, 0.1D, 0.3D * spatial, 0.01D);
        }
    }

    /**
     * The shock ring: particles laid on circles in the plane of the impact surface, so the hit has
     * a visible wavefront instead of a blob. Two layers of different radius keep it reading as a
     * spreading ring rather than a drawn circle.
     */
    private static void shockRing(ServerLevel server, Vec3 point, Vec3 axis, double scale,
            double spatial) {
        Vec3 normal = axis == null ? new Vec3(0.0D, 1.0D, 0.0D) : axis;
        Vec3 helper = Math.abs(normal.y) < 0.9D ? new Vec3(0.0D, 1.0D, 0.0D) : new Vec3(1.0D, 0.0D, 0.0D);
        Vec3 u = normal.cross(helper);
        if (u.lengthSqr() < 1.0E-6D) {
            return;
        }
        u = u.normalize();
        Vec3 v = normal.cross(u).normalize();
        int samples = Math.min(24, count(RING_SAMPLES, scale));
        for (int layer = 0; layer < RING_LAYERS; layer++) {
            double radius = (1.2D + layer * 1.1D) * spatial;
            for (int sample = 0; sample < samples; sample++) {
                double angle = Math.PI * 2.0D * sample / samples;
                Vec3 ring = point.add(u.scale(Math.cos(angle) * radius)).add(v.scale(Math.sin(angle) * radius));
                server.sendParticles(ParticleTypes.POOF, ring.x, ring.y, ring.z, 1,
                        0.05D, 0.05D, 0.05D, 0.01D);
            }
            Vec3 punch = point.add(u.scale(radius));
            server.sendParticles(ParticleTypes.EXPLOSION, punch.x, punch.y, punch.z, 1,
                    0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    /** The crater and the knockback. Terrain damage stays opt-in, exactly like the rocket. */
    private static void boom(ServerLevel server, Entity shell, Vec3 point, double splashRadius,
            boolean breakBlocks) {
        float radius = (float) clamp(splashRadius, 0.0D, MAX_SPLASH_RADIUS);
        Level.ExplosionInteraction interaction = breakBlocks
                ? Level.ExplosionInteraction.TNT
                : Level.ExplosionInteraction.NONE;
        server.explode(shell, point.x, point.y, point.z, radius, false, interaction);
    }

    /**
     * Two layers of the same registered clips: the explosion supplies the body, a low-pitched gun
     * shot supplies the snap in front of it. No new audio is needed for a heavier report.
     */
    private static void sounds(ServerLevel server, Vec3 point) {
        server.playSound(null, point.x, point.y, point.z, ModSounds.EXPLOSION.get(),
                SoundSource.PLAYERS, BLAST_VOLUME, BLAST_PITCH);
        server.playSound(null, point.x, point.y, point.z, ModSounds.GUN_SHOT.get(),
                SoundSource.PLAYERS, CRACK_VOLUME, CRACK_PITCH);
    }

    /**
     * Hands out one packet per nearby player, carrying both strengths of the hit.
     *
     * <p>The camera shock keeps its own short radius - a shake that reached the whole map would be
     * noise - while the visual strength follows the much wider visual radius, because vanilla only
     * delivers particles a few dozen blocks out. The list is walked once by hand instead of using
     * {@code PacketDistributor.NEAR} so every player gets their own pair of falloffs; NEAR sends one
     * identical packet to the whole radius.
     */
    private static void broadcast(ServerLevel server, Vec3 point) {
        double shakeBase = clamp(LittleBirdConfig.mainGunImpactShake(), 0.0D, MAX_SHAKE_INTENSITY);
        double visible = clamp(LittleBirdConfig.mainGunImpactVisibleDistanceBlocks(), 0.0D,
                MAX_VISIBLE_DISTANCE);
        if (shakeBase <= 0.0D && visible <= 0.0D) {
            return;
        }
        // A single query has to cover the wider of the two ranges; the per-player strengths below
        // are what keep the two effects apart.
        double range = Math.max(shakeBase > 0.0D ? SHAKE_RADIUS : 0.0D, visible);
        List<ServerPlayer> listeners = server.getPlayers(
                player -> player.distanceToSqr(point.x, point.y, point.z) <= range * range);
        for (ServerPlayer player : listeners) {
            double distance = Math.sqrt(player.distanceToSqr(point.x, point.y, point.z));
            float intensity = shakeBase > 0.0D
                    ? falloffTo(shakeBase, distance, SHAKE_RADIUS)
                    : 0.0F;
            float visualStrength = visible > 0.0D ? falloffTo(1.0D, distance, visible) : 0.0F;
            if (intensity <= 0.0F && visualStrength <= 0.0F) {
                continue;
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MainGunImpactPacket((float) point.x, (float) point.y, (float) point.z,
                            intensity, visualStrength));
        }
    }

    /**
     * Squared linear falloff: full at the impact, zero at the radius, never above the strength.
     * Squaring is what makes a hit near the middle of the range still read as a hit.
     */
    private static float falloffTo(double strength, double distance, double radius) {
        double falloff = 1.0D - distance / radius;
        if (falloff <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(1.0D, strength * falloff * falloff);
    }

    /** The block the round tore up, or null when the impact was in the air. */
    private static BlockState surfaceState(ServerLevel server, Vec3 point, Vec3 axis) {
        Vec3 probe = axis == null ? point.add(0.0D, -0.4D, 0.0D) : point.subtract(axis.scale(0.4D));
        if (!finite(probe)) {
            return null;
        }
        BlockState state = server.getBlockState(BlockPos.containing(probe));
        return state.isAir() ? null : state;
    }

    /** Unit normal, or null for anything that cannot be one (absent, zero length, non-finite). */
    private static Vec3 axisOf(Vec3 normal) {
        if (normal == null || !finite(normal)) {
            return null;
        }
        Vec3 unit = normal.normalize();
        return unit.lengthSqr() < 1.0E-6D ? null : unit;
    }

    /** Same rule as the muzzle flash: scaled counts, but never fewer than one particle. */
    private static int count(int base, double scale) {
        return Math.max(1, (int) Math.round(base * scale));
    }

    private static double scaleOf(double particleScale) {
        return clamp(particleScale, 0.05D, 8.0D);
    }

    private static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    /** NaN-safe clamp: junk collapses to the low end instead of poisoning the maths. */
    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
