package io.github.littlebird.client.particle;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A single puff of the thrown smoke cloud.
 *
 * <p>This is the whole visual of the smoke grenade: a cloud entity keeps spawning these on the
 * client every tick, and each one lives for about a second and a half before it is replaced. The
 * look comes from the turnover rather than from any one sprite, which is why the puffs are cheap,
 * short lived and constantly moving instead of a large static shell.
 *
 * <p>Three details do most of the work. The sprite walks its sixteen frame animation over the
 * particle's life, so the smoke appears to boil. The quad grows as it ages, so the cloud swells.
 * And the light is pinned to full sky light while keeping the block light, which makes the smoke
 * read bright in daylight and dim at night, without glowing in a cave.
 */
public class SmokePuffParticle extends TextureSheetParticle {
    private static final int MIN_LIFETIME_TICKS = 6;
    private static final float SIZE_JITTER = 0.55F;
    private static final float FADE_IN_FRACTION = 0.18F;
    private static final float FADE_OUT_FRACTION = 0.32F;

    private final SpriteSet sprites;
    private final float baseSize;

    private SmokePuffParticle(ClientLevel level, double x, double y, double z,
                              double xSpeed, double ySpeed, double zSpeed, SpriteSet sprites) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        this.sprites = sprites;
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        this.quadSize = 1.0F;
        int configured = LittleBirdConfig.smokeGrenadeParticleLifetimeTicks();
        int jitter = this.random.nextInt(13) - 6;
        this.lifetime = Math.max(MIN_LIFETIME_TICKS, configured + jitter);
        this.baseSize = LittleBirdConfig.smokeGrenadeParticleSize()
                * (1.0F - SIZE_JITTER * 0.5F + this.random.nextFloat() * SIZE_JITTER);
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.friction = 0.94F;
        this.setSpriteFromAge(sprites);
        this.alpha = 0.0F;
    }

    @Override
    public void tick() {
        super.tick();
        // Walk the frame animation: sixteen sprites over the life of the puff is what makes the
        // smoke look like it is rolling rather than like a drifting decal.
        this.setSpriteFromAge(this.sprites);
        float progress = this.age / (float) this.lifetime;
        this.quadSize = this.baseSize * (0.62F + 0.62F * progress);
        float fadeIn = progress / FADE_IN_FRACTION;
        float fadeOut = (1.0F - progress) / FADE_OUT_FRACTION;
        this.alpha = Math.max(0.0F, Math.min(1.0F, Math.min(fadeIn, fadeOut)));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        int packed = super.getLightColor(partialTick);
        // Sky light pinned to fifteen: the light map still darkens it at night and in caves, but
        // the puff never falls into the blue-black of an unlit corner and disappears against it.
        return 0xF00000 | (packed & 0xFFFF);
    }

    /** Wires the sixteen frame sprite set to the smoke puff type. */
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y,
                                       double z, double xSpeed, double ySpeed, double zSpeed) {
            return new SmokePuffParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, this.sprites);
        }
    }
}
