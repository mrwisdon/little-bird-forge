package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;
import org.junit.jupiter.api.Test;

/** Flight model of the 120mm shell. */
class MainGunShellBallisticsTest {
    @Test
    void launchKeepsTheMuzzleSpeedWhicheverWayItPoints() {
        Vector3 flat = MainGunShellBallistics.launchVelocity(new Vector3(0.0D, 0.0D, 1.0D));
        assertEquals(MainGunShellBallistics.MUZZLE_SPEED, flat.length(), 1.0E-9D);
        Vector3 up = MainGunShellBallistics.launchVelocity(new Vector3(0.0D, 1.0D, 0.0D));
        assertEquals(MainGunShellBallistics.MUZZLE_SPEED, up.length(), 1.0E-9D);
        Vector3 diagonal = MainGunShellBallistics.launchVelocity(new Vector3(0.3D, 0.4D, 0.5D));
        assertEquals(MainGunShellBallistics.MUZZLE_SPEED, diagonal.length(), 1.0E-9D);
    }

    @Test
    void armourPiercingLeavesFasterThanHighExplosive() {
        Vector3 direction = new Vector3(0.3D, 0.4D, 0.5D);
        assertEquals(9.0D, MainGunShellBallistics.launchVelocity(direction, MainGunAmmoType.AP)
                .length(), 1.0E-9D);
        assertEquals(5.0D, MainGunShellBallistics.launchVelocity(direction, MainGunAmmoType.HE)
                .length(), 1.0E-9D);
    }

    @Test
    void flightOnlyLosesHeightToGravity() {
        Vector3 velocity = MainGunShellBallistics.launchVelocity(new Vector3(0.0D, 0.0D, 1.0D));
        Vector3 after = MainGunShellBallistics.advance(velocity);
        assertEquals(velocity.x(), after.x(), 1.0E-9D);
        assertEquals(velocity.z(), after.z(), 1.0E-9D);
        assertEquals(velocity.y() - MainGunShellBallistics.GRAVITY, after.y(), 1.0E-9D);
    }

    @Test
    void aRoundOutrangesTheOldHitscan() {
        double reach = MainGunShellBallistics.MUZZLE_SPEED * MainGunShellBallistics.LIFETIME_TICKS;
        assertTrue(reach > 128.0D, "the shell must outrange the 128 block hitscan it replaces");
    }
}
