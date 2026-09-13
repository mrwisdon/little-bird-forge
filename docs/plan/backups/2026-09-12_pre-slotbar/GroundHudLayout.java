package io.github.littlebird.client;

/**
 * Where the ground vehicle HUD panels sit, kept free of Minecraft classes so the layout can be
 * unit tested the way the aircraft HUD layout is.
 *
 * <p>Following the Battlefield habit: vehicle condition bottom left, weapon stack bottom right.
 * The panel sizes are fixed pixels - the font size is fixed too, so shrinking them with the HUD
 * scale would spill the text out of the boxes - while the edge margin uses the same
 * {@code hudScale} multiplier the flight HUD applies (2 means a two pixel margin per base pixel).
 */
public final class GroundHudLayout {
    /** Same margin as the flight HUD so the two never fight for the same corner. */
    public static final int EDGE_MARGIN = 6;
    public static final int HEALTH_WIDTH = 96;
    public static final int HEALTH_HEIGHT = 26;
    public static final int WEAPON_WIDTH = 138;
    public static final int WEAPON_ROW_HEIGHT = 18;
    public static final int WEAPON_ROWS = 3;
    /** At or below this fraction the hull counts as damaged and the HUD warns. */
    public static final float HEALTH_WARNING_FRACTION = 0.35F;

    /** A scaled rectangle in GUI pixels. */
    public record Rect(int x, int y, int width, int height) {
    }

    /** The two panels; the weapon panel grows upward from the bottom edge. */
    public record Panels(Rect health, Rect weapons, int rowHeight) {
    }

    private GroundHudLayout() {
    }

    public static Panels compute(int screenWidth, int screenHeight, int hudScale) {
        return compute(screenWidth, screenHeight, hudScale, WEAPON_ROWS);
    }

    /** The weapon stack grows with the number of weapons the vehicle actually carries. */
    public static Panels compute(int screenWidth, int screenHeight, int hudScale, int rows) {
        int margin = EDGE_MARGIN * Math.max(1, hudScale);
        int healthWidth = HEALTH_WIDTH;
        int healthHeight = HEALTH_HEIGHT;
        int weaponWidth = WEAPON_WIDTH;
        int rowHeight = WEAPON_ROW_HEIGHT;
        int weaponHeight = rowHeight * Math.max(1, rows);
        int healthX = margin;
        int healthY = Math.max(margin, screenHeight - margin - healthHeight);
        int weaponX = Math.max(margin, screenWidth - margin - weaponWidth);
        int weaponY = Math.max(margin, screenHeight - margin - weaponHeight);
        return new Panels(
                new Rect(healthX, healthY, healthWidth, healthHeight),
                new Rect(weaponX, weaponY, weaponWidth, weaponHeight),
                rowHeight);
    }

    /** Top edge of one row inside the weapon panel. */
    public static int rowY(Panels panels, int row) {
        return panels.weapons().y() + Math.max(0, row) * panels.rowHeight();
    }

    /** Fill width for a 0..1 reading, clamped into the bar. */
    public static int fillWidth(float fraction, int fullWidth) {
        if (!Float.isFinite(fraction)) {
            return 0;
        }
        float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
        return Math.round(fullWidth * clamped);
    }

    /** Whole seconds left on a cooldown, rounded up so 1 tick still reads as 1 s. */
    public static int cooldownSeconds(int ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (ticks + 19) / 20;
    }

    public static boolean healthWarning(float fraction) {
        return Float.isFinite(fraction) && fraction <= HEALTH_WARNING_FRACTION;
    }

}
