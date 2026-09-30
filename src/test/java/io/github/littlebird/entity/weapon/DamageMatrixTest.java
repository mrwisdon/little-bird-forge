package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The damage matrix: what a round does once it has been matched against what it hit.
 *
 * <p>The rule the pack is built on - a bullet cannot defeat a tracked hull, a shell can, and a
 * helicopter is neither - lives here so it cannot drift weapon by weapon.
 */
class DamageMatrixTest {
    /** A machine gun: 14 to flesh, 3 to a hull. */
    private static final float GUN_FLESH = 14.0F;
    private static final float GUN_ARMOUR = 3.0F;
    /** A 120mm shell: 80 to flesh, 40 to a hull. */
    private static final float SHELL_FLESH = 80.0F;
    private static final float SHELL_ARMOUR = 40.0F;

    @Test
    void bulletsAreAbsorbedByHeavyArmour() {
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                DamageType.BULLET, ArmorClass.HEAVY_ARMOR, GUN_FLESH, GUN_ARMOUR));
    }

    @Test
    void bulletsStillHurtHelicopters() {
        assertEquals(1.0F, LittleBirdBallistics.damageTo(
                DamageType.BULLET, ArmorClass.AIRFRAME, GUN_FLESH, 1.0F));
    }

    @Test
    void bulletsHurtLightArmourButOnlyJust() {
        assertEquals(GUN_ARMOUR, LittleBirdBallistics.damageTo(
                DamageType.BULLET, ArmorClass.LIGHT_ARMOR, GUN_FLESH, GUN_ARMOUR));
    }

    @Test
    void bulletsHurtUnarmouredTargetsFully() {
        assertEquals(GUN_FLESH, LittleBirdBallistics.damageTo(
                DamageType.BULLET, ArmorClass.UNARMORED, GUN_FLESH, GUN_ARMOUR));
    }

    @Test
    void shellsDefeatHeavyArmour() {
        assertEquals(SHELL_ARMOUR, LittleBirdBallistics.damageTo(
                DamageType.SHELL, ArmorClass.HEAVY_ARMOR, SHELL_FLESH, SHELL_ARMOUR));
    }

    @Test
    void aShellIsPricedTheSameOnEveryTarget() {
        assertEquals(SHELL_FLESH, LittleBirdBallistics.damageTo(
                DamageType.SHELL, ArmorClass.UNARMORED, SHELL_FLESH, SHELL_ARMOUR));
        assertEquals(SHELL_ARMOUR, LittleBirdBallistics.damageTo(
                DamageType.SHELL, ArmorClass.LIGHT_ARMOR, SHELL_FLESH, SHELL_ARMOUR));
        assertEquals(SHELL_ARMOUR, LittleBirdBallistics.damageTo(
                DamageType.SHELL, ArmorClass.AIRFRAME, SHELL_FLESH, SHELL_ARMOUR));
    }

    @Test
    void everyWeaponClassAgainstEveryTargetClass() {
        for (DamageType type : DamageType.values()) {
            for (ArmorClass armor : ArmorClass.values()) {
                float damage = LittleBirdBallistics.damageTo(type, armor, GUN_FLESH, GUN_ARMOUR);
                if (type == DamageType.BULLET && armor == ArmorClass.HEAVY_ARMOR) {
                    assertEquals(0.0F, damage, type + " vs " + armor + " must be absorbed");
                } else if (armor == ArmorClass.UNARMORED) {
                    assertEquals(GUN_FLESH, damage, type + " vs " + armor);
                } else {
                    assertEquals(GUN_ARMOUR, damage, type + " vs " + armor);
                }
            }
        }
    }

    @Test
    void nonsensePricesStayAtZero() {
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                DamageType.SHELL, ArmorClass.HEAVY_ARMOR, -5.0F, -1.0F));
        assertEquals(0.0F, LittleBirdBallistics.damageTo(
                DamageType.BULLET, ArmorClass.UNARMORED, Float.NaN, 1.0F));
    }

    @Test
    void theThreeNumberAircraftPricingStillHonoursTheArmourRule() {
        // The Little Bird's gun and the Apache's chain gun carry three prices: flesh, a generic
        // aircraft and an Apache. Adding the armour rule must not disturb them.
        assertEquals(1.0F, LittleBirdBallistics.damageTo(DamageType.BULLET,
                ArmorClass.AIRFRAME, LittleBirdBallistics.Airframe.LITTLE_BIRD,
                GUN_FLESH, 1.0F, 0.5F));
        assertEquals(0.5F, LittleBirdBallistics.damageTo(DamageType.BULLET,
                ArmorClass.AIRFRAME, LittleBirdBallistics.Airframe.APACHE,
                GUN_FLESH, 1.0F, 0.5F));
        assertEquals(0.0F, LittleBirdBallistics.damageTo(DamageType.BULLET,
                ArmorClass.HEAVY_ARMOR, LittleBirdBallistics.Airframe.NONE,
                GUN_FLESH, 1.0F, 0.5F),
                "a tracked hull must absorb machine gun fire whatever the aircraft price says");
    }
}
