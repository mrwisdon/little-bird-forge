package io.github.littlebird.entity.weapon;

/**
 * The field repair torch: how far it reaches, how fast it welds, and when it has nothing left to do.
 *
 * <p>Pure rules: no Minecraft classes, so the numbers can be pinned by a unit test and tuned
 * without a client. The item only decides <em>which</em> hull is under the crosshair; everything
 * numeric about the weld lives here.
 */
public final class RepairTorch {
    /** Blocks from the player's eyes to the hull being welded. Battlefield's tool is short ranged. */
    public static final double RANGE_BLOCKS = 4.5D;

    /** Hull points restored per second of held right click. */
    public static final float HEALTH_PER_SECOND = 15.0F;

    /** Hull points restored per game tick (a held use ticks twenty times a second). */
    public static final float HEALTH_PER_TICK = HEALTH_PER_SECOND / 20.0F;

    private RepairTorch() {
    }

    /** True while the hull is still below full integrity. */
    public static boolean needsRepair(float health, float maxHealth) {
        return maxHealth > 0.0F && health < maxHealth;
    }

    /** One tick of welding: never negative, never past the hull's maximum. */
    public static float repaired(float health, float maxHealth, float amount) {
        if (!(maxHealth > 0.0F) || !(amount > 0.0F)) {
            return Math.max(0.0F, health);
        }
        return Math.min(maxHealth, Math.max(0.0F, health) + amount);
    }

    /** Whether a hull at this squared distance is close enough to be welded. */
    public static boolean inRange(double distanceSqr) {
        if (!(distanceSqr >= 0.0D)) {
            return false;
        }
        return distanceSqr <= RANGE_BLOCKS * RANGE_BLOCKS;
    }
}
