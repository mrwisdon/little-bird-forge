package io.github.littlebird.entity.weapon;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.AirframeTargets;
import net.minecraft.world.entity.Entity;

/**
 * One table for "what does this weapon do to that vehicle".
 *
 * <p>Every vehicle weapon in the pack is a row here: flesh price, aircraft price, Apache price and
 * heavy-armour price, plus whether it is a bullet (bullets never defeat armour). The weapons read
 * their numbers from this table instead of carrying their own constants, so the whole exchange can
 * be read, tuned and tested in one place.
 *
 * <p>Numbers stay config-driven where the config owns them; the table is the single lookup.
 */
public final class VehicleDamageTable {
    /** Which hardpoint fired. */
    public enum Weapon {
        LITTLE_BIRD_GUN,
        LITTLE_BIRD_ROCKET,
        APACHE_CHAIN_GUN,
        APACHE_MISSILE,
        JET_CANNON,
        TANK_MAIN_GUN_AP,
        TANK_MAIN_GUN_HE,
        TANK_MAIN_GUN_CANISTER,
        TANK_CANISTER_PELLET,
        TANK_COAX,
        IFV_AUTOCANNON,
        IFV_ATGM,
        BRADLEY_AUTOCANNON,
        BRADLEY_M2,
        BRADLEY_TOW,
        PHL191_ROCKET,
        PLZ05_SHELL,
        JDAM,
        INCENDIARY
    }

    /** One row: the four prices, already clamped by {@link LittleBirdBallistics.WeaponDamage}. */
    public record Row(DamageType type, LittleBirdBallistics.WeaponDamage prices, float apache) {
        public float flesh() {
            return prices.unarmored();
        }

        public float aircraft() {
            return prices.airframe();
        }

        public float heavy() {
            return prices.heavy();
        }
    }

    private VehicleDamageTable() {
    }

    /** The row for a weapon. Config-backed rows are rebuilt so a config edit takes effect live. */
    public static Row row(Weapon weapon) {
        return switch (weapon) {
            case LITTLE_BIRD_GUN -> row(DamageType.BULLET,
                    LittleBirdConfig.gunDamage(), LittleBirdConfig.gunDamageAircraft(),
                    LittleBirdConfig.gunDamageApache(), 0.0F);
            case LITTLE_BIRD_ROCKET -> row(DamageType.SHELL,
                    LittleBirdConfig.rocketDirectHitDamage(),
                    LittleBirdConfig.rocketDamageAircraft(),
                    LittleBirdConfig.rocketDamageApache(),
                    LittleBirdConfig.rocketHeavyDamage());
            case APACHE_CHAIN_GUN -> row(DamageType.SHELL,
                    LittleBirdConfig.apacheChainGunDamage(),
                    LittleBirdConfig.apacheChainGunAirframeDamage(),
                    LittleBirdConfig.apacheChainGunAirframeDamage(),
                    LittleBirdConfig.apacheGunHeavyDamage());
            case APACHE_MISSILE -> row(DamageType.SHELL,
                    LittleBirdConfig.apacheMissileDamage(),
                    LittleBirdConfig.apacheMissileAirframeDamage(),
                    LittleBirdConfig.apacheMissileAirframeDamage(),
                    LittleBirdConfig.apacheMissileHeavyDamage());
            case JET_CANNON -> row(DamageType.SHELL, 8.0F, 4.0F, 4.0F, 0.0F);
            case TANK_MAIN_GUN_AP -> mainGun(MainGunAmmoType.AP);
            case TANK_MAIN_GUN_HE -> mainGun(MainGunAmmoType.HE);
            case TANK_MAIN_GUN_CANISTER -> mainGun(MainGunAmmoType.CANISTER);
            case TANK_CANISTER_PELLET -> row(DamageType.SHELL,
                    LittleBirdConfig.tankCanisterPelletDamage(),
                    LittleBirdConfig.tankCanisterPelletVehicleDamage(),
                    LittleBirdConfig.tankCanisterPelletVehicleDamage(), 0.0F);
            case TANK_COAX -> row(DamageType.BULLET,
                    LittleBirdConfig.tankCoaxDamage(), LittleBirdConfig.tankCoaxVehicleDamage(),
                    LittleBirdConfig.tankCoaxVehicleDamage(), 0.0F);
            case IFV_AUTOCANNON -> row(DamageType.SHELL,
                    LittleBirdConfig.ifvAutocannonDamage(),
                    LittleBirdConfig.ifvAutocannonVehicleDamage(),
                    LittleBirdConfig.ifvAutocannonVehicleDamage(),
                    LittleBirdConfig.ifvAutocannonHeavyDamage());
            case IFV_ATGM -> row(DamageType.SHELL,
                    LittleBirdConfig.ifvAtgmDamage(), LittleBirdConfig.ifvAtgmVehicleDamage(),
                    LittleBirdConfig.ifvAtgmVehicleDamage(),
                    LittleBirdConfig.ifvAtgmHeavyDamage());
            case BRADLEY_AUTOCANNON -> row(DamageType.SHELL,
                    LittleBirdConfig.bradleyAutocannonDamage(),
                    LittleBirdConfig.bradleyAutocannonVehicleDamage(),
                    LittleBirdConfig.bradleyAutocannonVehicleDamage(),
                    LittleBirdConfig.bradleyAutocannonHeavyDamage());
            case BRADLEY_M2 -> row(DamageType.BULLET,
                    LittleBirdConfig.bradleyM2Damage(), LittleBirdConfig.bradleyM2VehicleDamage(),
                    LittleBirdConfig.bradleyM2VehicleDamage(), 0.0F);
            case BRADLEY_TOW -> row(DamageType.SHELL,
                    LittleBirdConfig.bradleyTowDamage(), LittleBirdConfig.bradleyTowVehicleDamage(),
                    LittleBirdConfig.bradleyTowVehicleDamage(),
                    LittleBirdConfig.bradleyTowHeavyDamage());
            // The heavy launchers are area weapons: one 370mm rocket prices a hull at 40, one
            // 155mm shell at 60, whichever hull it is.
            case PHL191_ROCKET -> row(DamageType.SHELL,
                    Phl191Ballistics.DIRECT_DAMAGE, 40.0F, 40.0F, 40.0F);
            case PLZ05_SHELL -> row(DamageType.SHELL,
                    Plz05Ballistics.DIRECT_DAMAGE, 60.0F, 60.0F, 60.0F);
            case JDAM -> row(DamageType.SHELL,
                    (float) LittleBirdConfig.jdamDamage(),
                    (float) LittleBirdConfig.jdamDamage(),
                    (float) LittleBirdConfig.jdamDamage(),
                    (float) LittleBirdConfig.jdamGroundVehicleDamage());
            case INCENDIARY -> row(DamageType.SHELL,
                    (float) LittleBirdConfig.incendiaryDamage(),
                    (float) LittleBirdConfig.incendiaryDamage(),
                    (float) LittleBirdConfig.incendiaryDamage(),
                    0.0F);
        };
    }

    private static Row mainGun(MainGunAmmoType type) {
        return row(DamageType.SHELL,
                LittleBirdConfig.tankMainGunDamage(type),
                LittleBirdConfig.tankMainGunVehicleDamage(type),
                LittleBirdConfig.tankMainGunVehicleDamage(type),
                LittleBirdConfig.tankMainGunHeavyDamage(type));
    }

    private static Row row(DamageType type, float flesh, float aircraft, float apache, float heavy) {
        return new Row(type, new LittleBirdBallistics.WeaponDamage(flesh, aircraft, heavy), apache);
    }

    /**
     * What one round of this weapon does to that target: the flesh price for a mob, the aircraft
     * price for a hull that flies (the Apache has its own), the heavy price for armour. Bullets
     * never reach armour at all.
     */
    public static float damage(Weapon weapon, Entity target) {
        float base = apply(row(weapon), AirframeTargets.armorClass(target),
                AirframeTargets.classify(target));
        return base * multiplier(weapon, AirframeTargets.isArtillery(target));
    }

    /** The damage class of a weapon, without building a config-backed row. */
    public static DamageType type(Weapon weapon) {
        return switch (weapon) {
            case LITTLE_BIRD_GUN, TANK_COAX, BRADLEY_M2 -> DamageType.BULLET;
            default -> DamageType.SHELL;
        };
    }

    /**
     * Other vehicles' guns and rockets hit the two self-propelled launchers 20% harder: a
     * howitzer is a big, thinly plated target for a tank gun or a TOW. The launchers' own shells
     * are not "other vehicles" and get no bonus against each other.
     */
    public static float multiplier(Weapon weapon, boolean artilleryTarget) {
        if (!artilleryTarget) {
            return 1.0F;
        }
        if (weapon == Weapon.PHL191_ROCKET || weapon == Weapon.PLZ05_SHELL
                || weapon == Weapon.TANK_MAIN_GUN_CANISTER
                || weapon == Weapon.TANK_CANISTER_PELLET
                || weapon == Weapon.JDAM || weapon == Weapon.INCENDIARY) {
            return 1.0F;
        }
        return type(weapon) == DamageType.SHELL ? 1.2F : 1.0F;
    }

    /** The same resolution for a caller that already looked up the target classes. */
    public static float apply(Row row, ArmorClass armor, LittleBirdBallistics.Airframe airframe) {
        if (row.type() == DamageType.BULLET
                && (armor == ArmorClass.HEAVY_ARMOR || armor == ArmorClass.LIGHT_ARMOR)) {
            return 0.0F;
        }
        return switch (armor) {
            case UNARMORED -> row.flesh();
            case AIRFRAME -> airframe == LittleBirdBallistics.Airframe.APACHE
                    ? Math.max(0.0F, row.apache()) : row.aircraft();
            default -> row.heavy();
        };
    }
}
