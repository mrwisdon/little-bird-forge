package io.github.littlebird.entity.weapon;

/**
 * How fast an ammunition crate refills what is parked next to it.
 *
 * Ammunition stopped regenerating on its own when the crate arrived: a crew that spends its belt
 * has to fly home, which is the whole point of having a base. Rates are per second and are
 * deliberately different per class - a belt is cheap to restock, a guided missile is not.
 *
 * Pure rules: no Minecraft classes.
 */
public final class AmmoSupply {
    /** Ticks in a second, for turning the rates into ticks. */
    public static final int TICKS_PER_SECOND = 20;

    private final int bulletsPerSecond;
    private final int rocketsPerSecond;
    private final int missilesPerSecond;

    public AmmoSupply(int bulletsPerSecond, int rocketsPerSecond, int missilesPerSecond) {
        this.bulletsPerSecond = Math.max(0, bulletsPerSecond);
        this.rocketsPerSecond = Math.max(0, rocketsPerSecond);
        this.missilesPerSecond = Math.max(0, missilesPerSecond);
    }

    /** Rounds a belt gains in the given number of ticks. */
    public int bullets(int ticks) {
        return perSecond(bulletsPerSecond, ticks);
    }

    /** Rounds a rocket reserve gains in the given number of ticks. */
    public int rockets(int ticks) {
        return perSecond(rocketsPerSecond, ticks);
    }

    /** Missiles a TOW reserve gains in the given number of ticks. */
    public int missiles(int ticks) {
        return perSecond(missilesPerSecond, ticks);
    }

    private static int perSecond(int rate, int ticks) {
        if (rate <= 0 || ticks <= 0) {
            return 0;
        }
        // Integer maths on purpose: the caller asks every tick, and a rate of 4/s must hand out
        // exactly 4 rounds a second rather than one round every five ticks and then a burst.
        return Math.max(0, (int) ((long) rate * ticks / TICKS_PER_SECOND));
    }

    /** How close an aircraft has to be to the crate, in blocks. */
    public static final double SUPPLY_RADIUS_BLOCKS = 6.0D;
}
