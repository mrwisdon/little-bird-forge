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
 * The server half of a 25mm autocannon impact: small, hard and cheap.
 *
 * <p>The Bradley fires five rounds a second, so this is deliberately not the 120mm hit. A shell
 * used to land as an explosion emitter plus twenty-odd large smoke puffs and a dozen flames, which
 * at that rate built a grey wall in front of the gun and hid whatever was being shot at. What is
 * left is the shape of a real small-calibre strike: debris and sparks thrown along the surface
 * normal, one short bright frame, a little smoke, and a crack rather than a thunderclap. No dust
 * column and no shock ring on purpose - those are the 120mm's vocabulary and would read as a
 * different weapon.
 *
 * <p>Reusing the 120mm impact packet is the cheap way to keep a long-range 25mm burst visible:
 * vanilla particle packets stop a few dozen blocks out, and a gunner at 150 blocks would otherwise
 * see tracers leaving the barrel and nothing at the far end of them.
 */
public final class AutocannonImpactFx {
    /** Players this close feel the camera nudge; the 120mm radius is larger on purpose. */
    public static final double SHAKE_RADIUS = 48.0D;

    /** The camera punch at the point of impact; a fifth of the 120mm hit, never a map-wide shake. */
    private static final double SHAKE_STRENGTH = 0.30D;

    /** Ceiling on the visual radius, so a bad config value cannot walk the whole map. */
    private static final double MAX_VISIBLE_DISTANCE = 2048.0D;

    /** Five bursts a second means the particle budget is a hard ceiling, not a hint. */
    private static final int MAX_PARTICLES_PER_SHOT = 20;

    /** The single explosion frame, counted inside the budget. */
    private static final int FLASH_CORE_PARTICLES = 1;

    /** Particle bases; the sum with the core frame is 19, one short of the ceiling at scale 1. */
    private static final int FRAGMENT_BASE = 6;
    private static final int SPARK_BASE = 5;
    private static final int FLASH_BASE = 3;
    private static final int SMOKE_BASE = 4;

    /** Short and hard: a crack with a bright edge, not the 120mm's low rolling boom. */
    private static final float BLAST_VOLUME = 0.55F;
    private static final float BLAST_PITCH = 1.5F;
    private static final float CRACK_VOLUME = 0.4F;
    private static final float CRACK_PITCH = 1.7F;

    /** A 25mm round makes a dent, not a crater. */
    private static final double MAX_SPLASH_RADIUS = 16.0D;

    private AutocannonImpactFx() {
    }

    /**
     * Spawns one 25mm impact: directional debris and sparks, a brief flash, a little smoke, the
     * crack, and the camera nudge for everyone close enough to feel it.
     *
     * @param server        level the shell died in
     * @param shell         the shell itself, or null when it is already gone; passed on as the
     *                      explosion source so knockback attribution stays with the round
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
        Vec3 axis = axisOf(normal);
        int[] groups = budget(particleScale);
        BlockState debris = surfaceState(server, point, axis);

        ejecta(server, point, axis, particleScale, groups, debris);
        flash(server, point, groups);
        boom(server, shell, point, splashRadius, breakBlocks);
        sounds(server, point);
        broadcast(server, point);
    }

    /**
     * Debris and sparks thrown along the surface normal.
     *
     * <p>The spread box is stretched along the normal axis and pinched across it: vanilla particles
     * take a random velocity, so the only way to aim them is to constrain where they may appear.
     * Real block chips are preferred over glowing dots because a 25mm round is mostly a chipping
     * round, and the whole burst is kept near the surface so it cannot build the smoke wall the old
     * version did.
     */
    private static void ejecta(ServerLevel server, Vec3 point, Vec3 axis, double particleScale,
            int[] groups, BlockState debris) {
        double scale = scaleOf(particleScale);
        double across = 0.45D * scale;
        double along = 0.9D * scale;
        double sx = axis == null ? across : Math.abs(axis.x) * along + (1.0D - Math.abs(axis.x)) * across;
        double sy = axis == null ? across : Math.abs(axis.y) * along + (1.0D - Math.abs(axis.y)) * across;
        double sz = axis == null ? across : Math.abs(axis.z) * along + (1.0D - Math.abs(axis.z)) * across;
        Vec3 centre = axis == null ? point : point.add(axis.scale(Math.min(0.4D, 0.2D * scale)));
        server.sendParticles(ParticleTypes.CRIT, centre.x, centre.y, centre.z,
                groups[GROUP_SPARKS], sx, sy, sz, 0.22D);
        if (debris != null) {
            ParticleOptions chips = new BlockParticleOption(ParticleTypes.BLOCK, debris);
            server.sendParticles(chips, centre.x, centre.y, centre.z,
                    groups[GROUP_FRAGMENTS], sx, sy, sz, 0.28D);
        }
        server.sendParticles(ParticleTypes.SMOKE, centre.x, centre.y, centre.z,
                groups[GROUP_SMOKE], 0.3D * scale, 0.2D * scale, 0.3D * scale, 0.02D);
    }

    /** One bright frame plus a small flame burst: the flash has to be over almost immediately. */
    private static void flash(ServerLevel server, Vec3 point, int[] groups) {
        server.sendParticles(ParticleTypes.EXPLOSION, point.x, point.y, point.z,
                FLASH_CORE_PARTICLES, 0.0D, 0.0D, 0.0D, 0.0D);
        server.sendParticles(ParticleTypes.FLAME, point.x, point.y, point.z,
                groups[GROUP_FLASH], 0.3D, 0.3D, 0.3D, 0.03D);
    }

    /** The dent and the knockback. Damage stays with the shell entity. */
    private static void boom(ServerLevel server, Entity shell, Vec3 point, double splashRadius,
            boolean breakBlocks) {
        float radius = (float) clamp(splashRadius, 0.0D, MAX_SPLASH_RADIUS);
        Level.ExplosionInteraction interaction = breakBlocks
                ? Level.ExplosionInteraction.TNT
                : Level.ExplosionInteraction.NONE;
        server.explode(shell, point.x, point.y, point.z, radius, false, interaction);
    }

    /**
     * Two layers of the registered clips: a short explosion plus a higher-pitched gun shot on top.
     * The pitch is what separates this from the 120mm report, which is the same pair dropped low.
     */
    private static void sounds(ServerLevel server, Vec3 point) {
        server.playSound(null, point.x, point.y, point.z, ModSounds.EXPLOSION.get(),
                SoundSource.PLAYERS, BLAST_VOLUME, BLAST_PITCH);
        server.playSound(null, point.x, point.y, point.z, ModSounds.GUN_SHOT.get(),
                SoundSource.PLAYERS, CRACK_VOLUME, CRACK_PITCH);
    }

    /**
     * Hands out at most one packet per player, carrying the camera nudge and the visual strength.
     *
     * <p>Both falloffs are computed per player here rather than by {@code PacketDistributor.NEAR},
     * which would send one identical packet to the whole radius and make a hit at 45 blocks shake
     * as hard as one under the muzzle. Shake and vision have separate radii on purpose: the first
     * stays short, the second reaches the configured visual distance so a distant gunner sees where
     * the burst landed.
     */
    private static void broadcast(ServerLevel server, Vec3 point) {
        double visible = clamp(LittleBirdConfig.autocannonImpactVisibleDistanceBlocks(), 0.0D,
                MAX_VISIBLE_DISTANCE);
        double range = Math.max(SHAKE_RADIUS, visible);
        List<ServerPlayer> listeners = server.getPlayers(
                player -> player.distanceToSqr(point.x, point.y, point.z) <= range * range);
        for (ServerPlayer player : listeners) {
            double distance = Math.sqrt(player.distanceToSqr(point.x, point.y, point.z));
            float intensity = falloffTo(SHAKE_STRENGTH, distance, SHAKE_RADIUS);
            float visualStrength = visible > 0.0D ? falloffTo(1.0D, distance, visible) : 0.0F;
            if (intensity <= 0.0F && visualStrength <= 0.0F) {
                continue;
            }
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new MainGunImpactPacket((float) point.x, (float) point.y, (float) point.z,
                            intensity, visualStrength));
        }
    }

    // Group order used by the budget: fragments, sparks, flash flames, smoke.
    private static final int GROUP_FRAGMENTS = 0;
    private static final int GROUP_SPARKS = 1;
    private static final int GROUP_FLASH = 2;
    private static final int GROUP_SMOKE = 3;

    /**
     * Scales the four particle groups by the configured scale, then trims the total back under the
     * per-shot ceiling.
     *
     * <p>The trim order is the point: smoke wears the cut first, then the flash, then the sparks,
     * and the fragments last, because direction is what makes this hit readable. Every group keeps
     * at least one particle, so even a trimmed burst still shows debris, sparks, flame and smoke.
     */
    private static int[] budget(double particleScale) {
        double scale = scaleOf(particleScale);
        int[] groups = {count(FRAGMENT_BASE, scale), count(SPARK_BASE, scale),
                count(FLASH_BASE, scale), count(SMOKE_BASE, scale)};
        int total = FLASH_CORE_PARTICLES;
        for (int value : groups) {
            total += value;
        }
        int[] trimOrder = {GROUP_SMOKE, GROUP_FLASH, GROUP_SPARKS, GROUP_FRAGMENTS};
        for (int index : trimOrder) {
            while (total > MAX_PARTICLES_PER_SHOT && groups[index] > 1) {
                groups[index]--;
                total--;
            }
        }
        return groups;
    }

    /** The block the round chipped, or null when the impact was in the air. */
    private static BlockState surfaceState(ServerLevel server, Vec3 point, Vec3 axis) {
        Vec3 probe = axis == null ? point.add(0.0D, -0.3D, 0.0D) : point.subtract(axis.scale(0.3D));
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

    /** Squared linear falloff: full at the impact, zero at the radius, never above the strength. */
    private static float falloffTo(double strength, double distance, double radius) {
        double falloff = 1.0D - distance / radius;
        if (falloff <= 0.0D) {
            return 0.0F;
        }
        return (float) Math.min(1.0D, strength * falloff * falloff);
    }

    /** Same rule the 120mm uses: scaled counts, but never fewer than one particle. */
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
