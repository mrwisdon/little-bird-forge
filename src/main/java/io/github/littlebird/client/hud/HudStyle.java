package io.github.littlebird.client.hud;

/** One palette, one grid: the whole HUD set draws from here. */
public final class HudStyle {
    public static final int PANEL = 0xB00B0D0C;
    public static final int PANEL_STRIP = 0xD00E120F;
    public static final int EDGE = 0x99E0B060;
    public static final int SHADOW = 0x66000000;

    public static final int ACCENT = 0xFFE8B464;
    public static final int ACCENT_DIM = 0x55E8B464;
    public static final int TEXT = 0xFFEDEDE2;
    public static final int MUTED = 0xFF9A9C8E;
    public static final int DANGER = 0xFFFF5050;
    public static final int WARN = 0xFFFF9A3C;
    public static final int OK = 0xFF86D98A;
    public static final int CYAN = 0xFF6FD9E8;

    public static final int PAD = 4;
    public static final int ROW = 10;
    public static final int HEADER = 11;
    public static final int BAR = 4;

    private HudStyle() {
    }
}
