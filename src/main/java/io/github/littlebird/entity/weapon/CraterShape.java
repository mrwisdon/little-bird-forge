package io.github.littlebird.entity.weapon;

/**
 * The hole a warhead leaves in the ground, as pure arithmetic: which blocks inside the blast volume
 * are cracked open, and which material is simply too tough to care.
 *
 * <p>A crater is not a sphere. A 2000 lb bomb makes a bowl that is wider than it is deep, so the
 * vertical axis is squashed by {@link #VERTICAL_SQUASH}: a radius of nine blocks reaches nine
 * blocks sideways and about five and a half blocks down, and the impact block itself is always
 * inside. Keeping the shape here instead of inline in the bomb is what lets it be tested, and it is
 * deliberately independent of the explosion radius: the hole and the kill radius are two different
 * balances, tuned with two different keys.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class CraterShape {

    /** A crater is about this much wider than it is deep. */
    public static final double VERTICAL_SQUASH = 1.6D;

    /** Blast resistance at or above which material is left standing: obsidian, anvils, bedrock. */
    public static final float MAX_RESISTANCE = 100.0F;

    /** Safety valve: one warhead never breaks more than this many blocks in a single hit. */
    public static final int MAX_BLOCKS = 20000;

    private CraterShape() {
    }

    /**
     * True when the block offset {@code (dx, dy, dz)} from the impact point is inside the bowl.
     * The impact block is always cracked; zero, negative and non-finite radii crack nothing.
     */
    public static boolean isCracked(int dx, int dy, int dz, double radius) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) {
            return false;
        }
        double scaled = dy * VERTICAL_SQUASH;
        return (double) dx * dx + (double) dz * dz + scaled * scaled <= radius * radius;
    }

    /**
     * True when material with this blast resistance gives way. A negative resistance means the
     * block is not breakable at all, and anything tougher than the limit is left standing.
     */
    public static boolean isBreakable(float resistance, float maxResistance) {
        if (!Float.isFinite(resistance) || resistance < 0.0F) {
            return false;
        }
        float limit = Float.isFinite(maxResistance) && maxResistance > 0.0F
                ? maxResistance : MAX_RESISTANCE;
        return resistance <= limit;
    }

    /** Blocks in the box a crater of this radius has to scan. */
    public static long scannedBlocks(double radius) {
        if (!(radius > 0.0D) || !Double.isFinite(radius)) {
            return 0L;
        }
        long span = 2L * (long) Math.ceil(radius) + 1L;
        return span * span * span;
    }
}
