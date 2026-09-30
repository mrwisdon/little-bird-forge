package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import io.github.littlebird.block.VehicleFactoryBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Blocks: the ammunition crate, and the factory that builds vehicles from parts. */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LittleBird.MOD_ID);

    public static final RegistryObject<Block> AMMO_CRATE = BLOCKS.register("ammo_crate",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(3.0F, 6.0F)));

    /** The vehicle factory: feed it the parts for a vehicle and it builds one on a timer. */
    public static final RegistryObject<Block> VEHICLE_FACTORY = BLOCKS.register("vehicle_factory",
            () -> new VehicleFactoryBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)
                    .strength(5.0F, 6.0F)));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
