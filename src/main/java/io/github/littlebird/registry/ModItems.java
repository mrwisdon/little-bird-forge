package io.github.littlebird.registry;

import io.github.littlebird.LittleBird;
import io.github.littlebird.item.LittleBirdDeployItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LittleBird.MOD_ID);

    public static final RegistryObject<Item> LITTLE_BIRD = ITEMS.register("little_bird",
            () -> new LittleBirdDeployItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> APACHE = ITEMS.register("apache",
            () -> new io.github.littlebird.item.ApacheDeployItem(
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> MAIN_BATTLE_TANK = ITEMS.register("main_battle_tank",
            () -> new io.github.littlebird.item.MainBattleTankDeployItem(
                    new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> INFANTRY_FIGHTING_VEHICLE = ITEMS.register(
            "infantry_fighting_vehicle",
            () -> new io.github.littlebird.item.InfantryFightingVehicleDeployItem(
                    new Item.Properties().stacksTo(1)));

    // ------------------------------------------------------------------
    // Survival crafting chain: parts first, then the airframes themselves.
    // ------------------------------------------------------------------
    /** Structural frame section: the skeleton of both airframes. */
    public static final RegistryObject<Item> AIRFRAME_SECTION = ITEMS.register("airframe_section",
            () -> new Item(new Item.Properties()));

    /** Main rotor head and blades. */
    public static final RegistryObject<Item> ROTOR_ASSEMBLY = ITEMS.register("rotor_assembly",
            () -> new Item(new Item.Properties()));

    /** Turbine: the powerplant both aircraft share. */
    public static final RegistryObject<Item> AIRCRAFT_ENGINE = ITEMS.register("aircraft_engine",
            () -> new Item(new Item.Properties()));

    /** Flight control and instrument package. */
    public static final RegistryObject<Item> AVIONICS_UNIT = ITEMS.register("avionics_unit",
            () -> new Item(new Item.Properties()));

    /** Tandem canopy glazing. */
    public static final RegistryObject<Item> CANOPY_GLASS = ITEMS.register("canopy_glass",
            () -> new Item(new Item.Properties()));

    /** Weapon pylon: carries the Apache's pods and rails. */
    public static final RegistryObject<Item> WEAPON_PYLON = ITEMS.register("weapon_pylon",
            () -> new Item(new Item.Properties()));

    /** Fire control computer: the Apache's aiming core. */
    public static final RegistryObject<Item> TARGETING_COMPUTER = ITEMS.register(
            "targeting_computer", () -> new Item(new Item.Properties()));

    /** Half finished Apache airframe, before the weapons go on. */
    public static final RegistryObject<Item> APACHE_AIRFRAME = ITEMS.register("apache_airframe",
            () -> new Item(new Item.Properties()));

    /** Apache weapon system: pods, rails and the chain gun. */
    public static final RegistryObject<Item> APACHE_WEAPONS = ITEMS.register("apache_weapons",
            () -> new Item(new Item.Properties()));

    // ------------------------------------------------------------------
    // Ground vehicle crafting chain: parts, then the two hull assemblies.
    // ------------------------------------------------------------------
    /** Rolled armour plate: the skin of both ground hulls. */
    public static final RegistryObject<Item> ARMOR_PLATE = ITEMS.register("armor_plate",
            () -> new Item(new Item.Properties()));

    /** Track run for the main battle tank. */
    public static final RegistryObject<Item> TRACK_ASSEMBLY = ITEMS.register("track_assembly",
            () -> new Item(new Item.Properties()));

    /** Wheeled running gear for the infantry fighting vehicle. */
    public static final RegistryObject<Item> WHEEL_ASSEMBLY = ITEMS.register("wheel_assembly",
            () -> new Item(new Item.Properties()));

    /** Turret ring: both ground vehicles turn a turret on one. */
    public static final RegistryObject<Item> TURRET_RING = ITEMS.register("turret_ring",
            () -> new Item(new Item.Properties()));

    /** Gun barrel: the tank's 120mm or the IFV's autocannon. */
    public static final RegistryObject<Item> CANNON_BARREL = ITEMS.register("cannon_barrel",
            () -> new Item(new Item.Properties()));

    /** Half finished tank hull, before the turret and engine go on. */
    public static final RegistryObject<Item> TANK_HULL = ITEMS.register("tank_hull",
            () -> new Item(new Item.Properties()));

    /** Half finished IFV hull, before the turret and engine go on. */
    public static final RegistryObject<Item> IFV_HULL = ITEMS.register("ifv_hull",
            () -> new Item(new Item.Properties()));

    /** Ammunition crate: park next to one and it restocks the aircraft. */
    public static final RegistryObject<Item> AMMO_CRATE = ITEMS.register("ammo_crate",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.AMMO_CRATE.get(),
                    new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
