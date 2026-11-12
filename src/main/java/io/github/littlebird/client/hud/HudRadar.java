package io.github.littlebird.client.hud;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The tactical scope: rings, one beam turning counter-clockwise once every three seconds, contacts,
 * the predicted impact and the hull chevron. Values in blocks, +x right and +z down on screen.
 */
public final class HudRadar {
    public record Contact(double x, double z, boolean vehicle) {
    }

    public record Spec(double radiusBlocks, float yawDeg, List<Contact> contacts,
                       boolean hasImpact, double impactX, double impactZ,
                       boolean hasTarget, double targetX, double targetZ,
                       boolean hasStrike, double strikeX, double strikeZ) {
        /** The plain picture: no fire mission drawn on it. */
        public Spec(double radiusBlocks, float yawDeg, List<Contact> contacts,
                    boolean hasImpact, double impactX, double impactZ,
                    boolean hasTarget, double targetX, double targetZ) {
            this(radiusBlocks, yawDeg, contacts, hasImpact, impactX, impactZ,
                    hasTarget, targetX, targetZ, false, 0.0D, 0.0D);
        }
    }

    private HudRadar() {
    }

    public static void draw(GuiGraphics g, Font font, int x0, int y0, int size, Spec spec) {
        int cx = x0 + size / 2;
        int cy = y0 + size / 2;
        int radius = size / 2 - 1;

        // body + rim
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int) Math.sqrt(radius * radius - dy * dy);
            g.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, 0xC0090B09);
        }
        circle(g, cx, cy, radius, HudStyle.EDGE);

        // rings with block labels
        for (int ring = 1; ring <= 4; ring++) {
            int rr = radius * ring / 4;
            circle(g, cx, cy, rr, ring == 4 ? 0x66E8B464 : 0x33E8B464);
            String label = Integer.toString((int) Math.round(spec.radiusBlocks() * ring / 4.0D));
            g.drawString(font, label, cx + 3, cy - rr + 1, 0x66A08A60, false);
        }
        g.fill(cx - radius, cy, cx + radius + 1, cy + 1, 0x22E8B464);
        g.fill(cx, cy - radius, cx + 1, cy + radius + 1, 0x22E8B464);

        // beam
        long period = 3000L;
        double sweep = -Math.PI * 2.0D * (System.currentTimeMillis() % period) / period;
        for (int i = 16; i >= 1; i--) {
            double angle = sweep + i * 0.034D;
            int alpha = Math.max(6, 60 - i * 3);
            line(g, cx, cy,
                    cx + (int) Math.round(Math.cos(angle) * radius),
                    cy + (int) Math.round(Math.sin(angle) * radius), (alpha << 24) | 0xE8B464);
        }
        line(g, cx, cy,
                cx + (int) Math.round(Math.cos(sweep) * radius),
                cy + (int) Math.round(Math.sin(sweep) * radius), 0xFFE8C060);

        // contacts
        double scale = radius / Math.max(1.0D, spec.radiusBlocks());
        for (Contact contact : spec.contacts()) {
            int px = cx + (int) Math.round(contact.x() * scale);
            int py = cy + (int) Math.round(contact.z() * scale);
            double phi = Math.atan2(contact.z(), contact.x());
            double lag = (phi - sweep) % (Math.PI * 2.0D);
            if (lag < 0.0D) {
                lag += Math.PI * 2.0D;
            }
            double fade = Math.max(0.35D, 1.0D - lag / (Math.PI * 1.7D));
            int alpha = (int) Math.round(255.0D * fade);
            int colour = contact.vehicle() ? ((alpha << 24) | 0x6FE3E8) : ((alpha << 24) | 0xFF6060);
            int half = contact.vehicle() ? 2 : 1;
            g.fill(px - half, py - half, px + half + 1, py + half + 1, colour);
        }
        if (spec.hasImpact()) {
            int px = cx + (int) Math.round(clamp(spec.impactX() * scale, radius));
            int py = cy + (int) Math.round(clamp(spec.impactZ() * scale, radius));
            g.fill(px - 3, py, px + 4, py + 1, HudStyle.ACCENT);
            g.fill(px, py - 3, px + 1, py + 4, HudStyle.ACCENT);
        }
        if (spec.hasTarget()) {
            int px = cx + (int) Math.round(clamp(spec.targetX() * scale, radius));
            int py = cy + (int) Math.round(clamp(spec.targetZ() * scale, radius));
            circle(g, px, py, 5, 0xFFFFFFFF);
            line(g, cx, cy, px, py, 0x55E8B464);
        }

        // the point given to the battery: a bracketed orange diamond, unlike the white target ring
        if (spec.hasStrike()) {
            int px = cx + (int) Math.round(clamp(spec.strikeX() * scale, radius));
            int py = cy + (int) Math.round(clamp(spec.strikeZ() * scale, radius));
            circle(g, px, py, 7, HudStyle.WARN);
            g.fill(px - 5, py - 1, px + 6, py, HudStyle.WARN);
            g.fill(px - 1, py - 5, px, py + 6, HudStyle.WARN);
        }

        // hull chevron
        HudCanvas.chevron(g, cx, cy, spec.yawDeg(), 9, HudStyle.ACCENT);
    }

    public static void circle(GuiGraphics g, int cx, int cy, int radius, int colour) {
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            g.fill(cx - span, cy + dy, cx - span + 1, cy + dy + 1, colour);
            g.fill(cx + span, cy + dy, cx + span + 1, cy + dy + 1, colour);
        }
    }

    private static void line(GuiGraphics g, int x0, int y0, int x1, int y1, int colour) {
        HudCanvas.line(g, x0, y0, x1, y1, colour);
    }

    private static double clamp(double value, int radius) {
        return Math.max(-radius, Math.min(radius, value));
    }
}
