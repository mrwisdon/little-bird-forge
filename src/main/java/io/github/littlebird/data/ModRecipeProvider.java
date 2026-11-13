package io.github.littlebird.data;

import io.github.littlebird.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The survival crafting chain: seven parts, two airframe assemblies, then the aircraft.
 *
 * Progression is carried by the unlock conditions rather than by recipe layout: the aircraft
 * recipes are only unlocked once the player has actually made a part, so the book walks them
 * through the chain instead of dumping a 7 ingredient recipe in their face.
 */
public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AIRFRAME_SECTION.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BARS)
                .pattern("IBI").pattern("B B").pattern("IBI")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ROTOR_ASSEMBLY.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BARS)
                .pattern("III").pattern("B B").pattern(" I ")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AIRCRAFT_ENGINE.get())
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .pattern("IFI").pattern("CRC").pattern("III")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AVIONICS_UNIT.get())
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .define('S', Items.GLASS)
                .define('G', Items.GOLD_INGOT)
                .pattern("CRC").pattern("SGS").pattern(" R ")
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CANOPY_GLASS.get())
                .define('S', Items.GLASS)
                .define('B', Items.IRON_BARS)
                .pattern("SSS").pattern("B B").pattern("SSS")
                .unlockedBy("has_glass", has(Items.GLASS))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WEAPON_PYLON.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BARS)
                .pattern("I I").pattern("IBI").pattern(" I ")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TARGETING_COMPUTER.get())
                .define('G', Items.GOLD_INGOT)
                .define('R', Items.REDSTONE)
                .define('D', Items.DIAMOND)
                .pattern("GRG").pattern("RDR").pattern("GRG")
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(writer);

        // The Apache is assembled in two halves, because nine parts is more than a grid holds.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.APACHE_AIRFRAME.get())
                .requires(ModItems.AIRFRAME_SECTION.get(), 4)
                .requires(ModItems.ROTOR_ASSEMBLY.get(), 2)
                .requires(ModItems.AIRCRAFT_ENGINE.get(), 2)
                .requires(ModItems.AVIONICS_UNIT.get())
                .unlockedBy("has_airframe_section", has(ModItems.AIRFRAME_SECTION.get()))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.APACHE_WEAPONS.get())
                .requires(ModItems.WEAPON_PYLON.get(), 2)
                .requires(ModItems.TARGETING_COMPUTER.get())
                .requires(Items.IRON_BLOCK, 2)
                .requires(Items.COPPER_INGOT, 4)
                .unlockedBy("has_targeting_computer", has(ModItems.TARGETING_COMPUTER.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.AMMO_CRATE.get())
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GUNPOWDER)
                .define('C', Blocks.CHEST)
                .pattern("IGI").pattern("ICI").pattern("IGI")
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.LITTLE_BIRD.get())
                .define('A', ModItems.AIRFRAME_SECTION.get())
                .define('R', ModItems.ROTOR_ASSEMBLY.get())
                .define('E', ModItems.AIRCRAFT_ENGINE.get())
                .define('V', ModItems.AVIONICS_UNIT.get())
                .define('C', ModItems.CANOPY_GLASS.get())
                .pattern("ARA").pattern(" E ").pattern("ACV")
                .unlockedBy("has_airframe_section", has(ModItems.AIRFRAME_SECTION.get()))
                .save(writer);

        // The two repair recipes are hand written in src/main/resources: the generator did not
        // emit same-item-in same-item-out shapeless recipes, and a recipe that silently does not
        // exist is worse than one written by hand.

        // ------------------------------------------------------------------
        // Ground vehicles: rolled plate first, then running gear, turret and gun,
        // then the two hull assemblies, then the finished vehicles.
        // ------------------------------------------------------------------
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ARMOR_PLATE.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BARS)
                .pattern("III").pattern("IBI").pattern("III")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TRACK_ASSEMBLY.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BARS)
                .pattern("I I").pattern("IBI").pattern("I I")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WHEEL_ASSEMBLY.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BARS)
                .define('C', Items.COPPER_INGOT)
                .pattern("IBI").pattern("ICI").pattern("IBI")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TURRET_RING.get())
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .define('G', Items.GOLD_INGOT)
                .pattern("IRI").pattern("RGR").pattern("IRI")
                .unlockedBy("has_redstone", has(Items.REDSTONE))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CANNON_BARREL.get())
                .define('I', Items.IRON_INGOT)
                .define('B', Items.IRON_BARS)
                .pattern("III").pattern(" B ").pattern(" I ")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.TANK_HULL.get())
                .requires(ModItems.ARMOR_PLATE.get(), 4)
                .requires(ModItems.TRACK_ASSEMBLY.get(), 2)
                .requires(ModItems.TURRET_RING.get())
                .unlockedBy("has_armor_plate", has(ModItems.ARMOR_PLATE.get()))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.IFV_HULL.get())
                .requires(ModItems.ARMOR_PLATE.get(), 2)
                .requires(ModItems.WHEEL_ASSEMBLY.get(), 4)
                .requires(ModItems.TURRET_RING.get())
                .unlockedBy("has_armor_plate", has(ModItems.ARMOR_PLATE.get()))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MAIN_BATTLE_TANK.get())
                .requires(ModItems.TANK_HULL.get())
                .requires(ModItems.ARMOR_PLATE.get())
                .requires(ModItems.CANNON_BARREL.get())
                .requires(ModItems.AIRCRAFT_ENGINE.get())
                .requires(ModItems.TARGETING_COMPUTER.get())
                .unlockedBy("has_tank_hull", has(ModItems.TANK_HULL.get()))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,
                        ModItems.INFANTRY_FIGHTING_VEHICLE.get())
                .requires(ModItems.IFV_HULL.get())
                .requires(ModItems.CANNON_BARREL.get())
                .requires(ModItems.AIRCRAFT_ENGINE.get())
                .requires(ModItems.AVIONICS_UNIT.get())
                .unlockedBy("has_ifv_hull", has(ModItems.IFV_HULL.get()))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BRADLEY.get())
                .requires(ModItems.IFV_HULL.get())
                .requires(ModItems.CANNON_BARREL.get())
                .requires(ModItems.ARMOR_PLATE.get())
                .requires(ModItems.AVIONICS_UNIT.get())
                .requires(ModItems.TARGETING_COMPUTER.get())
                .unlockedBy("has_ifv_hull", has(ModItems.IFV_HULL.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.APACHE.get())
                .define('A', ModItems.APACHE_AIRFRAME.get())
                .define('W', ModItems.APACHE_WEAPONS.get())
                .define('C', ModItems.CANOPY_GLASS.get())
                .pattern("A  ").pattern(" W ").pattern(" C ")
                .unlockedBy("has_apache_airframe", has(ModItems.APACHE_AIRFRAME.get()))
                .save(writer);

        // The two jets are eleven parts - four airframe sections, two engines, avionics, canopy,
        // two pylons and the fire control computer - which a 3x3 grid cannot hold. They assemble
        // in three stages like the Apache, and the two airframes use different layouts so the
        // crafting table can tell an F-35 from a Su-57.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.F35_AIRFRAME.get())
                .define('A', ModItems.AIRFRAME_SECTION.get())
                .define('C', ModItems.CANOPY_GLASS.get())
                .define('V', ModItems.AVIONICS_UNIT.get())
                .pattern("A A").pattern("ACA").pattern(" V ")
                .unlockedBy("has_airframe_section", has(ModItems.AIRFRAME_SECTION.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SU57_AIRFRAME.get())
                .define('A', ModItems.AIRFRAME_SECTION.get())
                .define('C', ModItems.CANOPY_GLASS.get())
                .define('V', ModItems.AVIONICS_UNIT.get())
                .pattern("A A").pattern("AVA").pattern(" C ")
                .unlockedBy("has_airframe_section", has(ModItems.AIRFRAME_SECTION.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.F35_WEAPONS.get())
                .define('P', ModItems.WEAPON_PYLON.get())
                .define('T', ModItems.TARGETING_COMPUTER.get())
                .pattern("P P").pattern(" T ")
                .unlockedBy("has_weapon_pylon", has(ModItems.WEAPON_PYLON.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SU57_WEAPONS.get())
                .define('P', ModItems.WEAPON_PYLON.get())
                .define('T', ModItems.TARGETING_COMPUTER.get())
                .pattern("PTP")
                .unlockedBy("has_weapon_pylon", has(ModItems.WEAPON_PYLON.get()))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.F35.get())
                .requires(ModItems.F35_AIRFRAME.get())
                .requires(ModItems.AIRCRAFT_ENGINE.get(), 2)
                .requires(ModItems.F35_WEAPONS.get())
                .unlockedBy("has_f35_airframe", has(ModItems.F35_AIRFRAME.get()))
                .save(writer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SU57.get())
                .requires(ModItems.SU57_AIRFRAME.get())
                .requires(ModItems.AIRCRAFT_ENGINE.get(), 2)
                .requires(ModItems.SU57_WEAPONS.get())
                .unlockedBy("has_su57_airframe", has(ModItems.SU57_AIRFRAME.get()))
                .save(writer);

        // Field repair torch: a copper nozzle on an iron frame, lit by redstone.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.REPAIR_TORCH.get())
                .define('C', Items.COPPER_INGOT)
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE)
                .pattern(" C ").pattern("IRI").pattern(" I ")
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer);

        // The factory itself: an iron and redstone frame around a crafting table.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VEHICLE_FACTORY.get())
                .define('I', Items.IRON_BLOCK)
                .define('R', Items.REDSTONE)
                .define('C', Items.CRAFTING_TABLE)
                .pattern("IRI").pattern("RCR").pattern("IRI")
                .unlockedBy("has_iron_block", has(Items.IRON_BLOCK))
                .save(writer);
    }
}
