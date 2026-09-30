package io.github.littlebird.client;

import io.github.littlebird.LittleBird;
import io.github.littlebird.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client half of the impact packet: the tick that fades the shake, the local crater sparks, and the
 * far blast that keeps a long range hit readable.
 *
 * <p>The decay lives on the client clock rather than on the render path on purpose: a frame can be
 * rendered zero or many times per tick, so decaying in the renderer would make the shake length
 * depend on the frame rate. Ticking here also means the shock keeps bleeding while the world is
 * paused behind a menu, which is the behaviour a player expects from a camera punch.
 */
@Mod.EventBusSubscriber(modid = LittleBird.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class ImpactClientEvents {
    /** Near crater sparks; the server also sends the real particle list at this range. */
    private static final int LOCAL_POOF = 4;
    private static final int LOCAL_FLAME = 3;
    private static final int LOCAL_SMOKE = 2;
    private static final double LOCAL_SPREAD = 0.55D;
    private static final double LOCAL_SPEED = 0.08D;

    /** The far blast: two bright frames plus a little debris, never more than ten particles. */
    private static final int FAR_FLAME = 3;
    private static final int FAR_SMOKE = 3;
    private static final double FAR_SPREAD = 1.4D;
    private static final double FAR_SPEED = 0.12D;

    /** Ceiling on the far boom, so a hit 300 blocks away is audible without being deafening. */
    private static final float FAR_SOUND_MAX_VOLUME = 12.0F;
    private static final float FAR_SOUND_PITCH = 0.7F;
    private static final double FAR_SOUND_DISTANCE_DIVISOR = 24.0D;

    private ImpactClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        // END runs once per tick after the world and the player have updated, so the shock added by
        // this tick's impact packet is visible for a full tick before it starts to fade.
        ImpactShake.tick();
    }

    /**
     * Receive path for {@code MainGunImpactPacket}: stack the shock, then draw the hit for viewers
     * the server's own particles cannot reach.
     *
     * <p>The two strengths are independent. {@code intensity} is the camera shock and is zero past
     * the shake radius; {@code visualStrength} follows the much wider visual radius. A player close
     * enough for the server's own particle broadcast gets only the cheap local top-up, while a far
     * one gets a compact blast of their own, which is the whole point of the packet.
     */
    public static void localImpact(
            float x, float y, float z, float intensity, float visualStrength) {
        // Zero is a no-op in add(), so a far packet shakes nothing.
        ImpactShake.add(intensity);
        Minecraft minecraft = Minecraft.getInstance();
        // The level is null between worlds; a packet that arrives then has nowhere to be drawn.
        Level level = minecraft.level;
        if (level == null || !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
            return;
        }
        double shake = clamp01(intensity);
        double visual = clamp01(visualStrength);
        if (shake <= 0.0D && visual <= 0.0D) {
            return;
        }
        RandomSource random = level.random;
        if (shake > 0.0D) {
            burst(level, random, x, y, z, ParticleTypes.POOF, scaled(LOCAL_POOF, shake), LOCAL_SPREAD,
                    LOCAL_SPEED);
            burst(level, random, x, y, z, ParticleTypes.FLAME, scaled(LOCAL_FLAME, shake), LOCAL_SPREAD,
                    LOCAL_SPEED);
            burst(level, random, x, y, z, ParticleTypes.LARGE_SMOKE, scaled(LOCAL_SMOKE, shake),
                    LOCAL_SPREAD, LOCAL_SPEED);
        }
        if (visual > 0.0D) {
            farBlast(level, random, x, y, z, visual);
            farBoom(level, minecraft, x, y, z, shake);
        }
    }

    /**
     * A blast sized for a viewer the server's particle packets no longer reach.
     *
     * <p>Two bright frames are placed exactly at the point: a distance hit has to read as one clear
     * event, and a scattered orange puff alone would not. The smoke and flame that follow are spread
     * and scaled by the visual strength, but never drop below one particle each - a hit at the very
     * edge of the visual radius still has to show something.
     */
    private static void farBlast(Level level, RandomSource random, double x, double y, double z,
            double visual) {
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 0.0D, 0.0D, 0.0D);
        level.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0.0D, 0.0D, 0.0D);
        burst(level, random, x, y, z, ParticleTypes.FLAME, scaled(FAR_FLAME, visual), FAR_SPREAD,
                FAR_SPEED);
        burst(level, random, x, y, z, ParticleTypes.LARGE_SMOKE, scaled(FAR_SMOKE, visual),
                FAR_SPREAD, FAR_SPEED);
    }

    /**
     * The distant boom.
     *
     * <p>Skipped inside the shake radius on purpose: the server's own explosion sound already
     * reaches those players, and playing a second copy would just thicken it. Volume grows with
     * distance up to a ceiling, which is the cheap way to keep a hit audible at 300 blocks without
     * blowing out anyone standing at 60.
     */
    private static void farBoom(Level level, Minecraft minecraft, double x, double y, double z,
            double shake) {
        if (shake > 0.0D || minecraft.player == null) {
            return;
        }
        double distance = Math.sqrt(minecraft.player.distanceToSqr(x, y, z));
        float volume = (float) Math.min(FAR_SOUND_MAX_VOLUME,
                1.0D + distance / FAR_SOUND_DISTANCE_DIVISOR);
        level.playLocalSound(x, y, z, ModSounds.EXPLOSION.get(), SoundSource.PLAYERS, volume,
                FAR_SOUND_PITCH, false);
    }

    /** Weaker signals get proportionally fewer sparks, but a real impact always shows at least one. */
    private static int scaled(int base, double strength) {
        return Math.max(1, (int) Math.round(base * (0.35D + 0.65D * strength)));
    }

    private static double clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static void burst(Level level, RandomSource random, double x, double y, double z,
            ParticleOptions particle, int amount, double spread, double speed) {
        for (int i = 0; i < amount; i++) {
            level.addParticle(particle,
                    x + random.nextGaussian() * spread,
                    y + random.nextGaussian() * spread,
                    z + random.nextGaussian() * spread,
                    random.nextGaussian() * speed,
                    random.nextGaussian() * speed,
                    random.nextGaussian() * speed);
        }
    }
}
