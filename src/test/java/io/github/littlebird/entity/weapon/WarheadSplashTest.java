package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Blast damage must follow the same table as a direct hit, only scaled down. */
class WarheadSplashTest {
    private static final VehicleDamageTable.Row HOWITZER =
            VehicleDamageTable.row(VehicleDamageTable.Weapon.PLZ05_SHELL);

    @Test
    void damageFallsOffWithDistanceAndStopsAtTheRadius() {
        float near = WarheadSplash.damageAt(HOWITZER, ArmorClass.UNARMORED,
                LittleBirdBallistics.Airframe.NONE, 1.0D, 10.0D, 0.6F);
        float far = WarheadSplash.damageAt(HOWITZER, ArmorClass.UNARMORED,
                LittleBirdBallistics.Airframe.NONE, 8.0D, 10.0D, 0.6F);
        assertTrue(near > far, "close blasts must hurt more than distant ones");
        assertEquals(0.0F, WarheadSplash.damageAt(HOWITZER, ArmorClass.UNARMORED,
                LittleBirdBallistics.Airframe.NONE, 10.0D, 10.0D, 0.6F));
        assertEquals(0.0F, WarheadSplash.damageAt(HOWITZER, ArmorClass.UNARMORED,
                LittleBirdBallistics.Airframe.NONE, 15.0D, 10.0D, 0.6F));
    }

    @Test
    void vehiclesTakeNoSplashAtAll() {
        for (ArmorClass armor : new ArmorClass[]{ArmorClass.HEAVY_ARMOR,
                ArmorClass.LIGHT_ARMOR, ArmorClass.AIRFRAME}) {
            assertEquals(0.0F, WarheadSplash.damageAt(HOWITZER, armor,
                    LittleBirdBallistics.Airframe.LITTLE_BIRD, 0.0D, 10.0D, 0.6F),
                    "splash must not touch " + armor);
        }
    }

    @Test
    void softTargetsTakeTheAntiPersonnelPrice() {
        float flesh = WarheadSplash.damageAt(HOWITZER, ArmorClass.UNARMORED,
                LittleBirdBallistics.Airframe.NONE, 0.0D, 10.0D, 0.6F);
        assertEquals(HOWITZER.flesh() * 0.6F, flesh, 1.0E-4F);
    }
}
