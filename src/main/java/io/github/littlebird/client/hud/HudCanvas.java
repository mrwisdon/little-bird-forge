package io.github.littlebird.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The drawing primitives of the new HUD: one chamfered panel, one header strip, one tabular row,
 * one bar, one reticle, one sweep scope. Every vehicle HUD composes these, so nothing is drawn
 * with ad-hoc fills anymore.
 */
public final class HudCanvas {
    private HudCanvas() {
    }

    /** Panel with a 45 degree cut on its top-left corner, a one pixel edge and a header strip. */
    public static void panel(GuiGraphics g, Font font, String title, String tag,
                             int x, int y, int width, int height, int edge) {
        // shadow, then body drawn row by row so the corner can be cut
        g.fill(x + 2, y + 2, x + width + 2, y + height + 2, HudStyle.SHADOW);
        int cut = 6;
        for (int i = 0; i < cut; i++) {
            g.fill(x + cut - i, y + i, x + width, y + i + 1, HudStyle.PANEL_STRIP);
        }
        g.fill(x, y + cut, x + width, y + Math.max(cut, HudStyle.HEADER), HudStyle.PANEL_STRIP);
        g.fill(x, y + HudStyle.HEADER, x + width, y + height, HudStyle.PANEL);

        // border: top, right, bottom, left below the cut and the diagonal itself
        g.fill(x + cut, y, x + width, y + 1, edge);
        g.fill(x + width - 1, y, x + width, y + height, edge);
        g.fill(x, y + height - 1, x + width, y + height, edge);
        g.fill(x, y + cut, x + 1, y + height, edge);
        for (int i = 0; i < cut; i++) {
            g.fill(x + cut - 1 - i, y + i, x + cut - i, y + i + 1, edge);
        }
        g.fill(x + cut, y + HudStyle.HEADER, x + width, y + HudStyle.HEADER + 1, edge);
        if (title != null) {
            g.drawString(font, title, x + cut + 4, y + 3, edge, true);
        }
        if (tag != null) {
            g.drawString(font, tag, x + width - HudStyle.PAD - font.width(tag), y + 3,
                    HudStyle.MUTED, false);
        }
    }

    /** Label on the left, value right-aligned to {@code right}, values in the given colour. */
    public static void row(GuiGraphics g, Font font, int x, int right, int y,
                           String label, String value, int colour) {
        g.drawString(font, label, x, y, HudStyle.MUTED, false);
        g.drawString(font, value, right - font.width(value), y, colour, true);
    }

    public static void line(GuiGraphics g, Font font, int x, int y, String text, int colour) {
        g.drawString(font, text, x, y, colour, true);
    }

    /** Horizontal bar with a two pixel highlight on top. */
    public static void bar(GuiGraphics g, int x, int y, int width, int height,
                           double fraction, int colour) {
        double f = Math.max(0.0D, Math.min(1.0D, fraction));
        g.fill(x, y, x + width, y + height, 0x55101210);
        int filled = (int) Math.round(width * f);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + height, colour);
            g.fill(x, y, x + filled, y + 1, 0x55FFFFFF);
        }
        g.fill(x, y + height, x + width, y + height + 1, 0x66000000);
    }

    /** A bracketed centre reticle: four arms around a gap, no dot. */
    public static void reticle(GuiGraphics g, int cx, int cy, int gap, int arm, int colour) {
        g.fill(cx - gap - arm, cy, cx - gap, cy + 1, colour);
        g.fill(cx + gap + 1, cy, cx + gap + arm + 1, cy + 1, colour);
        g.fill(cx, cy - gap - arm, cx + 1, cy - gap, colour);
        g.fill(cx, cy + gap + 1, cx + 1, cy + gap + arm + 1, colour);
    }

    /** A small chevron pointing along the hull heading. */
    public static void chevron(GuiGraphics g, int cx, int cy, float yawDeg, int size, int colour) {
        double rad = Math.toRadians(yawDeg);
        int fx = (int) Math.round(-Math.sin(rad) * size);
        int fy = (int) Math.round(Math.cos(rad) * size);
        int sx = (int) Math.round(Math.cos(rad) * size / 2.0D);
        int sy = (int) Math.round(Math.sin(rad) * size / 2.0D);
        line(g, cx, cy, cx + fx, cy + fy, colour);
        line(g, cx + fx, cy + fy, cx + fx - sx - fx / 3, cy + fy - sy - fy / 3, colour);
        line(g, cx + fx, cy + fy, cx + fx + sx - fx / 3, cy + fy + sy - fy / 3, colour);
    }

    public static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
        int steps = Math.max(1, Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)));
        for (int i = 0; i <= steps; i++) {
            int px = x0 + (x1 - x0) * i / steps;
            int py = y0 + (y1 - y0) * i / steps;
            g.fill(px, py, px + 1, py + 1, colour);
        }
    }

    /** Top-centre alert ribbon: amber strip, one line per alert. Empty list draws nothing. */
    public static void alerts(GuiGraphics g, Font font, int width,
                              java.util.List<net.minecraft.network.chat.Component> alerts) {
        if (alerts.isEmpty()) {
            return;
        }
        int lineHeight = 12;
        int boxWidth = 0;
        for (var alert : alerts) {
            boxWidth = Math.max(boxWidth, font.width(alert));
        }
        boxWidth += 24;
        int x = (width - boxWidth) / 2;
        int y = 18;
        for (int i = 0; i < alerts.size(); i++) {
            int colour = i == 0 ? HudStyle.DANGER : HudStyle.WARN;
            g.fill(x, y + i * lineHeight, x + boxWidth, y + i * lineHeight + lineHeight - 2,
                    0x99000000);
            g.fill(x, y + i * lineHeight, x + 2, y + i * lineHeight + lineHeight - 2, colour);
            g.drawString(font, alerts.get(i), x + 8, y + i * lineHeight + 2, colour, true);
        }
    }

    /** Bottom hint line. */
    public static void hint(GuiGraphics g, Font font, int width, int height, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int w = font.width(text);
        g.drawString(font, text, (width - w) / 2, height - 14, HudStyle.MUTED, false);
    }
}
