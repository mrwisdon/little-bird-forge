package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.ApacheGeometry;
import io.github.littlebird.entity.ApacheMissileEntity;
import io.github.littlebird.entity.AutocannonShellEntity;
import io.github.littlebird.entity.B2Entity;
import io.github.littlebird.entity.B2Geometry;
import io.github.littlebird.entity.BradleyEntity;
import io.github.littlebird.entity.BradleyGeometry;
import io.github.littlebird.entity.IncendiaryBombEntity;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
import io.github.littlebird.entity.InfantryFightingVehicleGeometry;
import io.github.littlebird.entity.JdamBombEntity;
import io.github.littlebird.entity.JetEntity;
import io.github.littlebird.entity.Su57Entity;
import io.github.littlebird.entity.JetGeometry;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.LittleBirdGeometry;
import io.github.littlebird.entity.MainBattleTankEntity;
import io.github.littlebird.entity.MainBattleTankGeometry;
import io.github.littlebird.entity.MainGunShellEntity;
import io.github.littlebird.entity.LittleBirdRocketEntity;
import io.github.littlebird.entity.LockOnMissileEntity;
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

    public static final RegistryObject<EntityType<JetEntity>> F35 =
            ENTITY_TYPES.register("f35", () -> EntityType.Builder
                    .<JetEntity>of(JetEntity::new, MobCategory.MISC)
                    .sized(JetGeometry.aabbWidthBlocks(), JetGeometry.aabbHeightBlocks())
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("f35"));

    public static final RegistryObject<EntityType<Su57Entity>> SU57 =
            ENTITY_TYPES.register("su57", () -> EntityType.Builder
                    .<Su57Entity>of(Su57Entity::new, MobCategory.MISC)
                    .sized(JetGeometry.aabbWidthBlocks(), JetGeometry.aabbHeightBlocks())
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("su57"));

    public static final RegistryObject<EntityType<B2Entity>> B2 =
            ENTITY_TYPES.register("b2", () -> EntityType.Builder
                    .<B2Entity>of(B2Entity::new, MobCategory.MISC)
                    .sized(B2Geometry.aabbWidthBlocks(), B2Geometry.aabbHeightBlocks())
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("b2"));

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

    public static final RegistryObject<EntityType<BradleyEntity>> BRADLEY =
            ENTITY_TYPES.register("bradley", () -> EntityType.Builder
                    .<BradleyEntity>of(BradleyEntity::new, MobCategory.MISC)
                    .sized(BradleyGeometry.aabbWidthBlocks(), BradleyGeometry.aabbHeightBlocks())
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build("bradley"));

    /** One 25mm M242 round: a ballistic shell with a two block blast. */
    public static final RegistryObject<EntityType<AutocannonShellEntity>> AUTOCANNON_SHELL =
            ENTITY_TYPES.register("autocannon_shell", () -> EntityType.Builder
                    .<AutocannonShellEntity>of(AutocannonShellEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(24)
                    .updateInterval(1)
                    .build("autocannon_shell"));

    public static final RegistryObject<EntityType<LittleBirdRocketEntity>> LITTLE_BIRD_ROCKET =
            ENTITY_TYPES.register("little_bird_rocket", () -> EntityType.Builder
                    .<LittleBirdRocketEntity>of(LittleBirdRocketEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("little_bird_rocket"));

    public static final RegistryObject<EntityType<LockOnMissileEntity>> LOCK_ON_MISSILE =
            ENTITY_TYPES.register("lock_on_missile", () -> EntityType.Builder
                    .<LockOnMissileEntity>of(LockOnMissileEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(32)
                    .updateInterval(1)
                    .build("lock_on_missile"));

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

    /** One JDAM: guided onto a pod designation when there is one, ballistic without. */
    public static final RegistryObject<EntityType<JdamBombEntity>> JDAM_BOMB =
            ENTITY_TYPES.register("jdam_bomb", () -> EntityType.Builder
                    .<JdamBombEntity>of(JdamBombEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("jdam_bomb"));

    /** One incendiary canister: smaller blast, leaves a burning patch where it lands. */
    public static final RegistryObject<EntityType<IncendiaryBombEntity>> INCENDIARY_BOMB =
            ENTITY_TYPES.register("incendiary_bomb", () -> EntityType.Builder
                    .<IncendiaryBombEntity>of(IncendiaryBombEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(48)
                    .updateInterval(1)
                    .build("incendiary_bomb"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
    }
}
