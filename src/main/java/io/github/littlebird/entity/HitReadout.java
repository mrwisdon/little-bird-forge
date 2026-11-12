package io.github.littlebird.entity;

/**
 * The numbers and the text of a landed-hit readout, kept pure so the shooter's HUD and its tests
 * agree on what "60 / 120" means.
 *
 * <p>Two kinds of target state exist in this pack. A pack hull answers {@code healthFraction()}
 * and {@code getHealth()} but publishes no maximum, while a living target answers both through
 * vanilla. A hull's fraction <em>is</em> {@code health / max}, so the maximum can be recovered from
 * the pair and no hull has to learn a new method.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class HitReadout {

    /** Longest target name a HUD line can hold without running into the readout. */
    public static final int MAX_NAME_LENGTH = 32;

    private HitReadout() {
    }

    /**
     * Full integrity of a hull, recovered from its remaining points and its fraction. Anything
     * unusable - a dead hull, a zero or non-finite fraction - reads as 0, which the readout treats
     * as "not known" rather than as an empty bar.
     */
    public static float maxHealthFrom(float health, float fraction) {
        if (!Float.isFinite(health) || !Float.isFinite(fraction)
                || health <= 0.0F || fraction <= 0.0F) {
            return 0.0F;
        }
        float max = health / fraction;
        return Float.isFinite(max) && max > 0.0F ? max : 0.0F;
    }

    /** A health figure rounded for display; anything unusable reads as zero. */
    public static int points(float health) {
        if (!Float.isFinite(health) || health <= 0.0F) {
            return 0;
        }
        return Math.round(health);
    }

    /**
     * "60 / 120" - remaining out of full - while the target is up. Empty once it is down or the
     * maximum is unknown, because "0 / 0" would be worse than no readout at all.
     */
    public static String healthText(float health, float maxHealth, boolean kill) {
        if (kill) {
            return "";
        }
        int max = points(maxHealth);
        if (max <= 0) {
            return "";
        }
        return points(health) + " / " + max;
    }

    /** What the readout calls the target: its name, trimmed to something a HUD line can hold. */
    public static String targetName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String trimmed = name.strip();
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }
}
