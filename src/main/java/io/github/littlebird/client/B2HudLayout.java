package io.github.littlebird.client;

/**
 * Geometry for the B-2's bomb panel, kept out of the drawing code so the rules can be tested.
 *
 * <p>The panel lives in the bottom right corner and has two neighbours to respect: the edge of the
 * screen, and vanilla's hotbar. When they would share the bottom row the panel lifts clear of the
 * hotbar instead of drawing through it, and on a small logical screen (a big GUI scale in
 * fullscreen) the key hints are dropped rather than squeezed into the corner.
 */
public final class B2HudLayout {
    /** Row height for one line of text. */
    public static final int ROW = 11;
    /** Padding inside the panel border. */
    public static final int PAD = 6;
    /** Outer margin from the screen edges. */
    public static final int MARGIN = 6;
    /** Vanilla hotbar geometry: 182 wide and 22 tall, centred. */
    public static final int HOTBAR_HALF_WIDTH = 91;
    public static final int HOTBAR_HEIGHT = 22;
    /** Smallest width the panel may shrink to before it is clamped against the screen. */
    public static final int MIN_WIDTH = 128;
    /** Screens smaller than this in either direction lose the key hints. */
    public static final int ROOMY_WIDTH = 520;
    public static final int ROOMY_HEIGHT = 300;

    public record Panel(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    private B2HudLayout() {
    }

    /** True when the key hints fit without crowding the corner. */
    public static boolean hintsFit(int screenWidth, int screenHeight) {
        return screenWidth >= ROOMY_WIDTH && screenHeight >= ROOMY_HEIGHT;
    }

    /** Panel height for the rows actually drawn. */
    public static int heightFor(boolean hints) {
        return PAD + ROW * 2 + 10 + ROW * 3 + (hints ? ROW * 2 : 0) + PAD;
    }

    /** X of the hotbar's right edge, plus a gap: left of this the panel cannot share the row. */
    public static int hotbarRightEdge(int screenWidth) {
        return screenWidth / 2 + HOTBAR_HALF_WIDTH + 4;
    }

    /**
     * Places the panel: measured around its content, inside the screen, and clear of the hotbar.
     */
    public static Panel panelFor(int screenWidth, int screenHeight, int contentWidth,
                                 boolean hints) {
        int width = Math.max(MIN_WIDTH, contentWidth + PAD * 2);
        width = Math.min(width, Math.max(96, screenWidth - 8));
        int height = heightFor(hints);
        int x = Math.max(4, screenWidth - width - MARGIN);
        int bottomMargin = MARGIN;
        if (x < hotbarRightEdge(screenWidth) && screenHeight > 220) {
            bottomMargin += HOTBAR_HEIGHT + 4;
        }
        int y = Math.max(4, screenHeight - height - bottomMargin);
        // A screen too short for the full panel gets a truncated box rather than one hanging off
        // the bottom edge; the rows simply clip.
        return new Panel(x, y, width, Math.min(height, Math.max(0, screenHeight - y - 2)));
    }
}
