package io.github.littlebird.data;

import io.github.littlebird.LittleBird;
import io.github.littlebird.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/** Item models for the crafting chain: one flat icon per part, generated with the recipes. */
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, LittleBird.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.AIRFRAME_SECTION.get());
        basicItem(ModItems.ROTOR_ASSEMBLY.get());
        basicItem(ModItems.AIRCRAFT_ENGINE.get());
        basicItem(ModItems.AVIONICS_UNIT.get());
        basicItem(ModItems.CANOPY_GLASS.get());
        basicItem(ModItems.WEAPON_PYLON.get());
        basicItem(ModItems.TARGETING_COMPUTER.get());
        basicItem(ModItems.APACHE_AIRFRAME.get());
        basicItem(ModItems.APACHE_WEAPONS.get());
        basicItem(ModItems.ARMOR_PLATE.get());
        basicItem(ModItems.TRACK_ASSEMBLY.get());
        basicItem(ModItems.WHEEL_ASSEMBLY.get());
        basicItem(ModItems.TURRET_RING.get());
        basicItem(ModItems.CANNON_BARREL.get());
        basicItem(ModItems.TANK_HULL.get());
        basicItem(ModItems.IFV_HULL.get());
    }
}
