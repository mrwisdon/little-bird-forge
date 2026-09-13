package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Synthesised weapon and rotor sounds (see tools/generate_weapon_sounds.ps1). */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, LittleBird.MOD_ID);

    public static final RegistryObject<SoundEvent> ENGINE = register("engine");
    public static final RegistryObject<SoundEvent> GUN = register("gun");
    public static final RegistryObject<SoundEvent> GUN_SHOT = register("gun_shot");
    public static final RegistryObject<SoundEvent> ROCKET_LAUNCH = register("rocket_launch");
    public static final RegistryObject<SoundEvent> EXPLOSION = register("explosion");

    private ModSounds() {
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(LittleBird.MOD_ID, name)));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}