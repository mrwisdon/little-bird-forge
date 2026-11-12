package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * The mod's own particle types.
 *
 * <p>The smoke canister needs a sprite that no vanilla particle provides: a soft, cloudy wisp that
 * can be drawn hundreds of times over itself without its square edge showing. Vanilla smoke sprites
 * are small round puffs, and a wall of them reads as a pile of cotton balls, so the cloud gets a
 * texture set of its own.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, LittleBird.MOD_ID);

    /** One puff of the thrown smoke cloud. Its sixteen frames are listed in particles/smoke_puff.json. */
    public static final RegistryObject<SimpleParticleType> SMOKE_PUFF =
            PARTICLE_TYPES.register("smoke_puff", () -> new SimpleParticleType(false));

    private ModParticles() {
    }

    public static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
