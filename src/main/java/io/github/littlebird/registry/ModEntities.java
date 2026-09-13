package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.ApacheGeometry;
import io.github.littlebird.entity.ApacheMissileEntity;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
import io.github.littlebird.entity.InfantryFightingVehicleGeometry;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.LittleBirdGeometry;
import io.github.littlebird.entity.MainBattleTankEntity;
import io.github.littlebird.entity.MainBattleTankGeometry;
import io.github.littlebird.entity.MainGunShellEntity;
import io.github.littlebird.entity.LittleBirdRocketEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Entity type registration. */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, LittleBird.MOD_ID);

    public static final RegistryObject<EntityType<LittleBirdEntity>> LITTLE_BIRD =
            ENTITY_TYPES.register("little_bird", () -> EntityType.Builder
                    .<LittleBirdEntity>of(LittleBirdEntity::new, MobCategory.MISC)
                    .sized(LittleBirdGeometry.aabbWidthBlocks(),
                            LittleBirdGeometry.aabbHeightBlocks())
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build("little_bird"));

    public static final RegistryObject<EntityType<ApacheEntity>> APACHE =
            ENTITY_TYPES.register("apache", () -> EntityType.Builder
                    .<ApacheEntity>of(ApacheEntity::new, MobCategory.MISC)
                    .sized(ApacheGeometry.aabbWidthBlocks(), ApacheGeometry.aabbHeightBlocks())
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build("apache"));

    public static final RegistryObject<EntityType<MainBattleTankEntity>> MAIN_BATTLE_TANK =
            ENTITY_TYPES.register("main_battle_tank", () -> EntityType.Builder
                    .<MainBattleTankEntity>of(MainBattleTankEntity::new, MobCategory.MISC)
                    .sized(MainBattleTankGeometry.aabbWidthBlocks(),
                            MainBattleTankGeometry.aabbHeightBlocks())
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build("main_battle_tank"));

    public static final RegistryObject<EntityType<InfantryFightingVehicleEntity>>
            INFANTRY_FIGHTING_VEHICLE =
            ENTITY_TYPES.register("infantry_fighting_vehicle", () -> EntityType.Builder
                    .<InfantryFightingVehicleEntity>of(
                            InfantryFightingVehicleEntity::new, MobCategory.MISC)
                    .sized(InfantryFightingVehicleGeometry.aabbWidthBlocks(),
                            InfantryFightingVehicleGeometry.aabbHeightBlocks())
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build("infantry_fighting_vehicle"));

    public static final RegistryObject<EntityType<LittleBirdRocketEntity>> LITTLE_BIRD_ROCKET =
            ENTITY_TYPES.register("little_bird_rocket", () -> EntityType.Builder
                    .<LittleBirdRocketEntity>of(LittleBirdRocketEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("little_bird_rocket"));

    public static final RegistryObject<EntityType<ApacheMissileEntity>> APACHE_MISSILE =
            ENTITY_TYPES.register("apache_missile", () -> EntityType.Builder
                    .<ApacheMissileEntity>of(ApacheMissileEntity::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(16)
                    .updateInterval(1)
                    .build("apache_missile"));

    /** The 120mm main gun shell: fast and flat, not drawn as a model (its fireball is). */
    public static final RegistryObject<EntityType<MainGunShellEntity>> MAIN_GUN_SHELL =
            ENTITY_TYPES.register("main_gun_shell", () -> EntityType.Builder
                    .<MainGunShellEntity>of(MainGunShellEntity::new, MobCategory.MISC)
                    .sized(0.3F, 0.3F)
                    .clientTrackingRange(64)
                    .updateInterval(1)
                    .build("main_gun_shell"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
