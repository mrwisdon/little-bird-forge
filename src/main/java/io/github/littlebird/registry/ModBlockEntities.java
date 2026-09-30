package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import io.github.littlebird.block.VehicleFactoryBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Block entities. One so far: the vehicle factory. */
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, LittleBird.MOD_ID);

    public static final RegistryObject<BlockEntityType<VehicleFactoryBlockEntity>> VEHICLE_FACTORY =
            BLOCK_ENTITIES.register("vehicle_factory",
                    () -> BlockEntityType.Builder.of(VehicleFactoryBlockEntity::new,
                            ModBlocks.VEHICLE_FACTORY.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
    }
}
