package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LittleBird.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.battlefield"))
                    .icon(() -> ModItems.LITTLE_BIRD.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.LITTLE_BIRD.get());
                        output.accept(ModItems.APACHE.get());
                        output.accept(ModItems.MAIN_BATTLE_TANK.get());
                        output.accept(ModItems.INFANTRY_FIGHTING_VEHICLE.get());
                        output.accept(ModItems.BRADLEY.get());
                        output.accept(ModItems.PHL191.get());
                        output.accept(ModItems.PLZ05.get());
                        output.accept(ModItems.F35.get());
                        output.accept(ModItems.F35_PREVIEW.get());
                        output.accept(ModItems.SU57.get());
                        output.accept(ModItems.B2.get());
                        output.accept(ModItems.B2_PREVIEW.get());
                        output.accept(ModItems.AIRFRAME_SECTION.get());
                        output.accept(ModItems.ROTOR_ASSEMBLY.get());
                        output.accept(ModItems.AIRCRAFT_ENGINE.get());
                        output.accept(ModItems.AVIONICS_UNIT.get());
                        output.accept(ModItems.CANOPY_GLASS.get());
                        output.accept(ModItems.WEAPON_PYLON.get());
                        output.accept(ModItems.TARGETING_COMPUTER.get());
                        output.accept(ModItems.APACHE_AIRFRAME.get());
                        output.accept(ModItems.APACHE_WEAPONS.get());
                        output.accept(ModItems.AMMO_CRATE.get());
                        output.accept(ModItems.VEHICLE_FACTORY.get());
                        output.accept(ModItems.ARMOR_PLATE.get());
                        output.accept(ModItems.TRACK_ASSEMBLY.get());
                        output.accept(ModItems.WHEEL_ASSEMBLY.get());
                        output.accept(ModItems.TURRET_RING.get());
                        output.accept(ModItems.CANNON_BARREL.get());
                        output.accept(ModItems.TANK_HULL.get());
                        output.accept(ModItems.IFV_HULL.get());
                        output.accept(ModItems.SMOKE_GRENADE.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        TABS.register(modBus);
    }
}
