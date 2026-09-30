package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The 120mm canister load: forty forty-damage pellets, 5 degrees wide and 2 degrees tall. */
final class CanisterShotTest {
    private static final LittleBirdBallistics.Vector3 AIM =
            new LittleBirdBallistics.Vector3(0.0D, 0.0D, 1.0D);

    @Test
    void theLoadIsFortyPelletsAtFortyDamage() {
        assertEquals(40, CanisterShot.PELLETS);
        assertEquals(40.0F, CanisterShot.PELLET_DAMAGE, 1.0E-6F);
        assertEquals(40.0F, CanisterShot.PELLET_VEHICLE_DAMAGE, 1.0E-6F);
        assertEquals(5.0D, CanisterShot.HORIZONTAL_HALF_DEGREES, 1.0E-9D);
        assertEquals(2.0D, CanisterShot.VERTICAL_HALF_DEGREES, 1.0E-9D);
    }

    @Test
    void theCentreSampleStaysOnTheAimAxis() {
        LittleBirdBallistics.Vector3 direction = CanisterShot.direction(AIM, 0.5D, 0.5D);
        assertEquals(0.0D, direction.x(), 1.0E-9D);
        assertEquals(0.0D, direction.y(), 1.0E-9D);
        assertEquals(1.0D, direction.z(), 1.0E-9D);
    }

    @Test
    void theWidestPelletsSitOnTheBounds() {
        LittleBirdBallistics.Vector3 wide = CanisterShot.direction(AIM, 1.0D, 0.5D);
        assertEquals(5.0D, CanisterShot.horizontalAngleDegrees(AIM, wide), 1.0E-6D);
        assertEquals(0.0D, CanisterShot.verticalAngleDegrees(AIM, wide), 1.0E-6D);

        LittleBirdBallistics.Vector3 tall = CanisterShot.direction(AIM, 0.5D, 1.0D);
        assertEquals(0.0D, CanisterShot.horizontalAngleDegrees(AIM, tall), 1.0E-6D);
        assertEquals(2.0D, CanisterShot.verticalAngleDegrees(AIM, tall), 1.0E-6D);
    }

    @Test
    void everySampleStaysInsideTheSpread() {
        for (int horizontal = 0; horizontal <= 20; horizontal++) {
            for (int vertical = 0; vertical <= 20; vertical++) {
                LittleBirdBallistics.Vector3 direction = CanisterShot.direction(
                        AIM, horizontal / 20.0D, vertical / 20.0D);
                assertTrue(Math.abs(CanisterShot.horizontalAngleDegrees(AIM, direction))
                                <= CanisterShot.HORIZONTAL_HALF_DEGREES + 1.0E-6D,
                        "pellet outside the horizontal bound");
                assertTrue(Math.abs(CanisterShot.verticalAngleDegrees(AIM, direction))
                                <= CanisterShot.VERTICAL_HALF_DEGREES + 1.0E-6D,
                        "pellet outside the vertical bound");
            }
        }
    }
}
