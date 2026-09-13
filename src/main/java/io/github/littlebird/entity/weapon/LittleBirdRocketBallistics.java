package io.github.littlebird.entity.weapon;

import io.github.littlebird.entity.weapon.LittleBirdBallistics.Vector3;

/**
 * Shared 70mm rocket trajectory, kept free of Minecraft classes.
 *
 * The server projectile and the client impact predictor both step through this
 * code, which is the only way the aim marker on the HUD can be guaranteed to
 * agree with where the rocket actually goes.
 */
public final class LittleBirdRocketBallistics {
    public static final double INITIAL_SPEED = 2.50D;
    public static final double CRUISE_SPEED = 6.00D;
    public static final int SPIN_UP_TICKS = 6;
    public static final double GRAVITY = 0.010D;
    public static final int LIFETIME_TICKS = 40;

    private LittleBirdRocketBallistics() {
    }

    public static double accelerationPerTick() {
        return (CRUISE_SPEED - INITIAL_SPEED) / SPIN_UP_TICKS;
    }

    /** Velocity of a freshly launched rocket. */
    public static Vector3 launchVelocity(Vector3 direction) {
        return direction.normalize().scale(INITIAL_SPEED);
    }

    /**
     * One tick of flight. The motor keeps pushing along the launch axis - a real
     * unguided rocket flies where it was pointed - while gravity accumulates as a
     * separate downward component. Thrusting along the current velocity instead
     * would make the rocket nose over and lose forward speed, which both flies
     * worse and makes the aim marker wander.
     *
     * heading must be the unit launch direction.
     */
    public static Vector3 advance(Vector3 velocity, Vector3 heading) {
        Vector3 axis = heading.length() < 1.0E-6D
                ? new Vector3(0.0D, 0.0D, 1.0D)
                : heading.normalize();
        // Drive the component along the launch axis towards cruise speed; gravity
        // is added on top and has to accumulate, not be reapplied from scratch.
        double axial = velocity.dot(axis);
        double targetAxial = Math.min(CRUISE_SPEED, axial + accelerationPerTick());
        Vector3 motor = axis.scale(targetAxial - axial);
        return velocity.add(motor).add(new Vector3(0.0D, -GRAVITY, 0.0D));
    }
}