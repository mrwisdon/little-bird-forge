package io.github.littlebird;

import io.github.littlebird.network.ModNetwork;
import io.github.littlebird.registry.ModBlockEntities;
import io.github.littlebird.registry.ModBlocks;
import io.github.littlebird.registry.ModCreativeTabs;
import io.github.littlebird.registry.ModEntities;
import io.github.littlebird.registry.ModItems;
import io.github.littlebird.registry.ModSounds;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Battlefield Aircraft: a Forge 1.20.1 helicopter pack (Little Bird and AH-64 Apache). */
@Mod(LittleBird.MOD_ID)
public final class LittleBird {
    /** Mod id, and the asset/registry namespace for every aircraft in this pack. */
    public static final String MOD_ID = "battlefield";

    public LittleBird() {
        // FMLJavaModLoadingContext.get() is deprecated for removal in newer Forge
        // builds, but it is the supported call for 47.4.x and is what the sibling
        // project uses in this same Forge version.
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModItems.register(modBus);
        ModCreativeTabs.register(modBus);
        ModSounds.register(modBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LittleBirdConfig.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LittleBirdConfig.COMMON_SPEC);
        modBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
