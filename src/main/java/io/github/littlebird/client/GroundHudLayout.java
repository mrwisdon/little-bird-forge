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
    // Weapon slot bar, proportioned after the reference: one wider selected slot, then the rest.
    public static final int SLOT_HEIGHT = 22;
    public static final int SLOT_GAP = 3;
    public static final int SLOT_SELECTED_WIDTH = 54;
    public static final int SLOT_WIDTH = 30;
    /** Key legend drawn under each slot. */
    public static final int LEGEND_HEIGHT = 9;
    /** Ammunition readout that sits just above the bar and shares its exact width. */
    public static final int READOUT_HEIGHT = 34;
    public static final int WEAPON_ROWS = 3;
    /**
     * Vanilla hotbar geometry: 182 x 22 GUI pixels, centred on the bottom edge. The ground HUD is
     * drawn above it, so when a panel would sit on top of the hotbar it steps up by exactly the
     * overlap - no invented safe area, just Minecraft's own hotbar size.
     */
    public static final int HOTBAR_WIDTH = 182;
    public static final int HOTBAR_HEIGHT = 22;
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
        int weaponWidth = barWidth(rows);
        int rowHeight = SLOT_HEIGHT;
        int weaponHeight = rowHeight + LEGEND_HEIGHT;
        int healthX = margin;
        int healthY = Math.max(margin, screenHeight - margin - healthHeight);
        int weaponX = Math.max(margin, screenWidth - margin - weaponWidth);
        int weaponY = Math.max(margin, screenHeight - margin - weaponHeight);
        // Step up only where the vanilla hotbar would be covered, and only by the overlap.
        healthY -= hotbarLift(screenWidth, screenHeight, healthX, healthY, healthWidth, healthHeight);
        weaponY -= hotbarLift(screenWidth, screenHeight, weaponX, weaponY, weaponWidth, weaponHeight);
        healthY = Math.max(margin, healthY);
        weaponY = Math.max(margin, weaponY);
        return new Panels(
                new Rect(healthX, healthY, healthWidth, healthHeight),
                new Rect(weaponX, weaponY, weaponWidth, weaponHeight),
                rowHeight);
    }

    /**
     * How far a panel must rise to clear the vanilla hotbar: zero when they do not overlap, and
     * otherwise exactly the number of pixels they share, so nothing moves further than it must.
     */
    public static int hotbarLift(int screenWidth, int screenHeight, int x, int y, int width,
                                 int height) {
        int hotbarLeft = screenWidth / 2 - HOTBAR_WIDTH / 2;
        int hotbarRight = hotbarLeft + HOTBAR_WIDTH;
        int hotbarTop = screenHeight - HOTBAR_HEIGHT;
        boolean horizontal = x < hotbarRight && x + width > hotbarLeft;
        boolean vertical = y < screenHeight && y + height > hotbarTop;
        if (!horizontal || !vertical) {
            return 0;
        }
        return Math.max(0, Math.min(height, y + height - hotbarTop));
    }

    /** Total width of a bar carrying this many slots: the first one is the wide selected slot. */
    public static int barWidth(int slots) {
        int count = Math.max(1, slots);
        return SLOT_SELECTED_WIDTH + (count - 1) * SLOT_WIDTH + (count - 1) * SLOT_GAP;
    }

    /** Left edge of one slot inside the bar. */
    public static int slotX(Panels panels, int index) {
        if (index <= 0) {
            return panels.weapons().x();
        }
        return panels.weapons().x() + SLOT_SELECTED_WIDTH + SLOT_GAP
                + (index - 1) * (SLOT_WIDTH + SLOT_GAP);
    }

    /** Width of one slot: the selected position is deliberately the widest. */
    public static int slotWidth(int index) {
        return index <= 0 ? SLOT_SELECTED_WIDTH : SLOT_WIDTH;
    }

    public static Rect readout(int screenWidth, int screenHeight, int hudScale) {
        return readout(screenWidth, screenHeight, hudScale, WEAPON_ROWS);
    }

    /**
     * The ammunition readout, directly above the weapon bar and exactly as wide as it, so the two
     * boxes line up on both edges. The row count matters: a two weapon hull has a shorter bar and
     * the readout has to sit on top of that bar, not on top of a three weapon one.
     */
    public static Rect readout(int screenWidth, int screenHeight, int hudScale, int rows) {
        int margin = EDGE_MARGIN * Math.max(1, hudScale);
        Panels panels = compute(screenWidth, screenHeight, hudScale, rows);
        int x = panels.weapons().x();
        int y = Math.max(margin, panels.weapons().y() - 6 - READOUT_HEIGHT);
        return new Rect(x, y, panels.weapons().width(), READOUT_HEIGHT);
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
