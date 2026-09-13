package io.github.littlebird.entity.weapon;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;

/**
 * Flight model of the 120mm main gun shell, kept free of Minecraft classes.
 *
 * <p>A tank round is a dart rather than a rocket: it leaves the muzzle already at full speed and
 * only drops a little over a long shot, so there is no motor to spin up and no drag to fight.
 */
public final class MainGunShellBallistics {
    /**
     * Muzzle speed in blocks per tick (100 m/s at 20 ticks per second). Fast enough to read as a
     * tank round, slow enough that the player actually sees the shell leave the barrel.
     */
    public static final double MUZZLE_SPEED = 5.0D;
    /** A slight drop, so a very long shot wants a touch of lead. */
    public static final double GRAVITY = 0.02D;
    public static final int LIFETIME_TICKS = 60;

    public static Vector3 launchVelocity(Vector3 direction) {
        return direction.normalize().scale(MUZZLE_SPEED);
    }

    /** One tick of flight: the heading is kept, gravity pulls the round down. */
    public static Vector3 advance(Vector3 velocity) {
        return new Vector3(velocity.x(), velocity.y() - GRAVITY, velocity.z());
    }

    private MainGunShellBallistics() {
    }
}
