package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The vehicle-vs-vehicle table is the single place these numbers live, so it gets tested. */
class VehicleDamageTableTest {
    private static VehicleDamageTable.Row row(VehicleDamageTable.Weapon weapon) {
        return VehicleDamageTable.row(weapon);
    }

    @Test
    void artilleryRowsMatchTheBallisticsConstants() {
        VehicleDamageTable.Row howitzer = row(VehicleDamageTable.Weapon.PLZ05_SHELL);
        assertEquals(Plz05Ballistics.DIRECT_DAMAGE, howitzer.flesh(), 1.0E-6F);
        assertEquals(Plz05Ballistics.VEHICLE_DAMAGE, howitzer.aircraft(), 1.0E-6F);
        assertEquals(Plz05Ballistics.HEAVY_DAMAGE, howitzer.heavy(), 1.0E-6F);

        VehicleDamageTable.Row launcher = row(VehicleDamageTable.Weapon.PHL191_ROCKET);
        assertEquals(Phl191Ballistics.DIRECT_DAMAGE, launcher.flesh(), 1.0E-6F);
        assertEquals(Phl191Ballistics.VEHICLE_DAMAGE, launcher.aircraft(), 1.0E-6F);
        assertEquals(Phl191Ballistics.HEAVY_DAMAGE, launcher.heavy(), 1.0E-6F);
    }

    @Test
    void artilleryPricesEveryHullAtSixtyAndForty() {
        VehicleDamageTable.Row howitzer = row(VehicleDamageTable.Weapon.PLZ05_SHELL);
        assertEquals(60.0F, howitzer.aircraft(), 1.0E-6F);
        assertEquals(60.0F, howitzer.apache(), 1.0E-6F);
        assertEquals(60.0F, howitzer.heavy(), 1.0E-6F);
        VehicleDamageTable.Row launcher = row(VehicleDamageTable.Weapon.PHL191_ROCKET);
        assertEquals(40.0F, launcher.aircraft(), 1.0E-6F);
        assertEquals(40.0F, launcher.apache(), 1.0E-6F);
        assertEquals(40.0F, launcher.heavy(), 1.0E-6F);
    }

    @Test
    void theHowitzerPunchesHarderThanTheRocketOnArmour() {
        assertTrue(row(VehicleDamageTable.Weapon.PLZ05_SHELL).heavy()
                > row(VehicleDamageTable.Weapon.PHL191_ROCKET).heavy());
        assertTrue(row(VehicleDamageTable.Weapon.PLZ05_SHELL).flesh()
                > row(VehicleDamageTable.Weapon.PHL191_ROCKET).flesh());
    }

    @Test
    void bulletsNeverReachArmourButDoHurtAirframes() {
        VehicleDamageTable.Row bullet = new VehicleDamageTable.Row(DamageType.BULLET,
                new LittleBirdBallistics.WeaponDamage(5.0F, 3.0F, 0.0F), 3.0F);
        assertEquals(0.0F, VehicleDamageTable.apply(bullet, ArmorClass.HEAVY_ARMOR,
                LittleBirdBallistics.Airframe.NONE));
        assertEquals(0.0F, VehicleDamageTable.apply(bullet, ArmorClass.LIGHT_ARMOR,
                LittleBirdBallistics.Airframe.NONE));
        assertEquals(5.0F, VehicleDamageTable.apply(bullet, ArmorClass.UNARMORED,
                LittleBirdBallistics.Airframe.NONE));
        assertEquals(3.0F, VehicleDamageTable.apply(bullet, ArmorClass.AIRFRAME,
                LittleBirdBallistics.Airframe.LITTLE_BIRD));
    }

    @Test
    void theApacheHasItsOwnAirframePrice() {
        VehicleDamageTable.Row bullet = new VehicleDamageTable.Row(DamageType.BULLET,
                new LittleBirdBallistics.WeaponDamage(5.0F, 3.0F, 0.0F), 1.0F);
        assertEquals(1.0F, VehicleDamageTable.apply(bullet, ArmorClass.AIRFRAME,
                LittleBirdBallistics.Airframe.APACHE));
    }

    @Test
    void otherVehiclesGunsHitArtilleryHarder() {
        assertEquals(1.2F, VehicleDamageTable.multiplier(
                VehicleDamageTable.Weapon.TANK_MAIN_GUN_AP, true), 1.0E-6F);
        assertEquals(1.2F, VehicleDamageTable.multiplier(
                VehicleDamageTable.Weapon.APACHE_MISSILE, true), 1.0E-6F);
        assertEquals(1.0F, VehicleDamageTable.multiplier(
                VehicleDamageTable.Weapon.PLZ05_SHELL, true), 1.0E-6F);
        assertEquals(1.0F, VehicleDamageTable.multiplier(
                VehicleDamageTable.Weapon.BRADLEY_M2, true), 1.0E-6F);
        assertEquals(1.0F, VehicleDamageTable.multiplier(
                VehicleDamageTable.Weapon.TANK_MAIN_GUN_AP, false), 1.0E-6F);
    }

    @Test
    void shellsAlwaysPriceArmourAndNothingIsNegative() {
        for (VehicleDamageTable.Weapon weapon : VehicleDamageTable.Weapon.values()) {
            if (weapon == VehicleDamageTable.Weapon.TANK_MAIN_GUN_AP
                    || weapon == VehicleDamageTable.Weapon.TANK_MAIN_GUN_HE
                    || weapon == VehicleDamageTable.Weapon.TANK_MAIN_GUN_CANISTER
                    || weapon == VehicleDamageTable.Weapon.TANK_CANISTER_PELLET
                    || weapon == VehicleDamageTable.Weapon.TANK_COAX
                    || weapon == VehicleDamageTable.Weapon.IFV_AUTOCANNON
                    || weapon == VehicleDamageTable.Weapon.IFV_ATGM
                    || weapon == VehicleDamageTable.Weapon.BRADLEY_AUTOCANNON
                    || weapon == VehicleDamageTable.Weapon.BRADLEY_M2
                    || weapon == VehicleDamageTable.Weapon.BRADLEY_TOW
                    || weapon == VehicleDamageTable.Weapon.LITTLE_BIRD_GUN
                    || weapon == VehicleDamageTable.Weapon.LITTLE_BIRD_ROCKET
                    || weapon == VehicleDamageTable.Weapon.APACHE_CHAIN_GUN
                    || weapon == VehicleDamageTable.Weapon.APACHE_MISSILE
                    || weapon == VehicleDamageTable.Weapon.JDAM
                    || weapon == VehicleDamageTable.Weapon.INCENDIARY) {
                continue; // config-backed rows need a loaded config, checked in game
            }
            VehicleDamageTable.Row row = row(weapon);
            for (float price : new float[]{row.flesh(), row.aircraft(), row.heavy(),
                    row.apache()}) {
                assertTrue(Float.isFinite(price) && price >= 0.0F,
                        weapon + " has a bad price: " + price);
            }
        }
        VehicleDamageTable.Row shell = row(VehicleDamageTable.Weapon.PLZ05_SHELL);
        assertTrue(VehicleDamageTable.apply(shell, ArmorClass.HEAVY_ARMOR,
                LittleBirdBallistics.Airframe.NONE) > 0.0F);
        assertTrue(VehicleDamageTable.apply(shell, ArmorClass.LIGHT_ARMOR,
                LittleBirdBallistics.Airframe.NONE) > 0.0F);
    }
}
