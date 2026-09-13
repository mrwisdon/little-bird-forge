package io.github.littlebird.client;

/**
 * HUD geometry for the Little Bird, kept free of Minecraft classes so every
 * placement rule can be unit-tested instead of eyeballed.
 *
 * All rectangles are in GUI-scaled pixels. The caller scales them by the
 * configured HUD scale before drawing.
 */
public final class LittleBirdHudLayout {
    public static final int HEAT_BAR_WIDTH = 80;
    public static final int HEAT_BAR_HEIGHT = 6;
    public static final int ROCKET_CELL_WIDTH = 5;
    public static final int ROCKET_CELL_HEIGHT = 8;
    public static final int ROCKET_CELL_GAP = 2;
    public static final int HEADING_TAPE_WIDTH = 160;
    public static final int HEADING_TAPE_HEIGHT = 10;
    public static final int HEADING_WINDOW_DEGREES = 80;
    public static final int HEALTH_BAR_WIDTH = 60;
    public static final int HEALTH_BAR_HEIGHT = 5;
    public static final int BANK_HALF_WIDTH = 60;
    public static final int EDGE_MARGIN = 6;

    private LittleBirdHudLayout() {
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    public record Panels(
            Rect heatBar,
            Rect rocketGrid,
            Rect healthBar,
            Rect headingTape,
            Rect bankIndicator,
            Rect flightReadout,
            Rect statusReadout,
            Rect alertLine) {
    }

    /**
     * Anchors every HUD element. Vertical anchors only depend on the screen
     * height and horizontal ones are centred, so extreme window shapes cannot
     * make elements overlap.
     */
    public static Panels compute(int screenWidth, int screenHeight, int hudScale) {
        int scale = Math.max(1, hudScale);
        int centreX = screenWidth / 2;
        int centreY = screenHeight / 2;
        int rocketGridWidth = rocketGridWidth();
        int weaponRowWidth = HEAT_BAR_WIDTH + 16 + rocketGridWidth;
        int weaponRowLeft = centreX - weaponRowWidth / 2;

        Rect headingTape = new Rect(
                centreX - HEADING_TAPE_WIDTH / 2, EDGE_MARGIN * scale,
                HEADING_TAPE_WIDTH, HEADING_TAPE_HEIGHT);
        Rect bankIndicator = new Rect(
                centreX - BANK_HALF_WIDTH, centreY + 34 * scale,
                BANK_HALF_WIDTH * 2, 8);
        Rect heatBar = new Rect(
                weaponRowLeft, screenHeight - EDGE_MARGIN * scale - 8 * scale,
                HEAT_BAR_WIDTH, HEAT_BAR_HEIGHT);
        Rect rocketGrid = new Rect(
                weaponRowLeft + HEAT_BAR_WIDTH + 16,
                heatBar.y() - ROCKET_CELL_HEIGHT + HEAT_BAR_HEIGHT,
                rocketGridWidth, ROCKET_CELL_HEIGHT * 2 + ROCKET_CELL_GAP);
        Rect healthBar = new Rect(
                screenWidth - EDGE_MARGIN * scale - HEALTH_BAR_WIDTH,
                screenHeight - EDGE_MARGIN * scale - 20 * scale,
                HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT);
        Rect flightReadout = new Rect(
                EDGE_MARGIN * scale, screenHeight - 48 * scale, 90, 34);
        Rect statusReadout = new Rect(
                screenWidth - EDGE_MARGIN * scale - 90, screenHeight - 48 * scale, 90, 34);
        Rect alertLine = new Rect(
                centreX - 90, centreY - 70 * scale, 180, 20);
        return new Panels(heatBar, rocketGrid, healthBar, headingTape, bankIndicator,
                flightReadout, statusReadout, alertLine);
    }

    public static int rocketGridWidth() {
        return 7 * ROCKET_CELL_WIDTH + 6 * ROCKET_CELL_GAP;
    }

    /** Pixel rectangle of one rocket cell; row 0 is the left pod, row 1 the right. */
    public static Rect rocketCell(Rect grid, int pod, int tube) {
        int x = grid.x() + tube * (ROCKET_CELL_WIDTH + ROCKET_CELL_GAP);
        int y = grid.y() + pod * (ROCKET_CELL_HEIGHT + ROCKET_CELL_GAP);
        return new Rect(x, y, ROCKET_CELL_WIDTH, ROCKET_CELL_HEIGHT);
    }

    /** A tube is spent when its index is beyond the rounds left in that pod. */
    public static boolean rocketCellSpent(int pod, int tube, int leftRounds, int rightRounds) {
        int remaining = pod == 0 ? leftRounds : rightRounds;
        return tube >= remaining;
    }

    /** Enemy airframe readout: a short bar under the crosshair. */
    public static final int ENEMY_BAR_WIDTH = 64;
    public static final int ENEMY_BAR_HEIGHT = 5;
    public static final int ENEMY_BAR_OFFSET_PIXELS = 16;

    /**
     * Filled width of the enemy health bar. The fill is anchored on the left, so losing
     * health shortens the bar from its right hand end.
     */
    public static int enemyHealthFillWidth(float fraction, int fullWidth) {
        if (fullWidth <= 0 || !Float.isFinite(fraction)) {
            return 0;
        }
        float clamped = Math.max(0.0F, Math.min(1.0F, fraction));
        return Math.round(fullWidth * clamped);
    }

    /** Filled width of the heat bar, clamped to the bar. */
    public static int heatFillWidth(int heat, int maxHeat) {
        if (maxHeat <= 0 || heat <= 0) {
            return 0;
        }
        int filled = (int) Math.round(HEAT_BAR_WIDTH * Math.min(heat, maxHeat) / (double) maxHeat);
        return Math.max(0, Math.min(HEAT_BAR_WIDTH, filled));
    }

    /** Horizontal shift of the heading tape for the current yaw, wrapped. */
    public static int headingShift(float yawDeg) {
        float wrapped = yawDeg % 360.0F;
        if (wrapped < 0.0F) {
            wrapped += 360.0F;
        }
        float relative = wrapped > 180.0F ? wrapped - 360.0F : wrapped;
        double pixelsPerDegree = HEADING_TAPE_WIDTH / (double) HEADING_WINDOW_DEGREES;
        return (int) Math.round(-relative * pixelsPerDegree);
    }

    /** Offset of the bank indicator from centre, clamped to the track. */
    public static int bankOffset(float rollDeg, float maxRollDeg) {
        if (maxRollDeg <= 0.0F) {
            return 0;
        }
        float fraction = Math.max(-1.0F, Math.min(1.0F, rollDeg / maxRollDeg));
        return (int) Math.round(fraction * BANK_HALF_WIDTH);
    }

    /** Blocks per tick to whole kilometres per hour. */
    public static int toKilometresPerHour(double blocksPerTick) {
        return (int) Math.round(blocksPerTick * 20.0D * 3.6D);
    }

    /** Vertical offset of a speed arrow: -1 climb, 1 descent, 0 level. */
    public static int verticalArrow(double verticalSpeed) {
        if (verticalSpeed > 0.02D) {
            return -1;
        }
        return verticalSpeed < -0.02D ? 1 : 0;
    }

    /** True when the health fraction should flash as a warning. */
    public static boolean lowHealth(float health, float maxHealth) {
        return maxHealth > 0.0F && health / maxHealth < 0.3F;
    }
}
