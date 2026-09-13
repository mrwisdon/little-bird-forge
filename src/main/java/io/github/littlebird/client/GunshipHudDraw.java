package io.github.littlebird.client;

import com.mojang.math.Axis;
import io.github.littlebird.entity.weapon.ApachePods;
import io.github.littlebird.entity.weapon.LittleBirdPodTubes;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Drawing shared by every gunship HUD in the pack: the cyan centre cluster, the weapon panel
 * chrome, alerts, the hit marker and the enemy airframe readout.
 *
 * The vehicle specific parts (reticles, the panel's contents) stay with each aircraft; anything
 * that only needs a handful of numbers lives here so the Little Bird and the Apache cannot
 * drift apart.
 */
public final class GunshipHudDraw {
    public static final int CYAN = 0xFF8AF0E8;
    public static final int CYAN_DIM = 0x808AF0E8;
    public static final int BOX_BACKGROUND = 0x99101418;
    public static final int YELLOW = 0xFFF2C94C;
    public static final int TEXT = 0xFFE6E6E6;
    public static final int NOMINAL = 0xFF7FD46A;
    public static final int WARNING = 0xFFF2C94C;
    public static final int DANGER = 0xFFEB5757;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int HIT_MARKER_COLOR = 0xFFFF5A5A;
    public static final int BAR_TRACK = 0xC0000000;

    private static final int LADDER_CENTRE_GAP_PIXELS = 16;
    private static final int HIT_MARKER_MILLIS = 200;
    /** How long the enemy airframe readout stays up after the last hit. */
    private static final int ENEMY_BAR_MILLIS = 3000;
    public static final int ENEMY_BAR_WIDTH = 64;
    public static final int ENEMY_BAR_HEIGHT = 5;
    public static final int ENEMY_BAR_OFFSET_PIXELS = 16;
    // Reference rocket reticle: two bracket bars, a dot column per pod and a white circle on
    // the predicted impact point. Shared so both gunships aim with the same sight.
    public static final int ROCKET_RETICLE_COLOR = 0xFFFFC24A;
    private static final int ROCKET_BAR_OFFSET_PIXELS = 10;
    private static final int ROCKET_BAR_WIDTH_PIXELS = 2;
    private static final int ROCKET_BAR_HALF_HEIGHT_PIXELS = 6;
    private static final int ROCKET_BAR_GAP_PIXELS = 4;
    private static final int ROCKET_CIRCLE_RADIUS_PIXELS = 4;
    private static final int ROCKET_DOT_SIZE_PIXELS = 2;
    private static final int ROCKET_DOT_ROW_SPACING_PIXELS = 5;
    private static final int ROCKET_DOT_COLUMN_SPACING_PIXELS = 5;
    public static final int SLOT_WIDTH = 42;
    public static final int SLOT_HEIGHT = 20;

    private static long hitMarkerUntil;
    private static boolean hitMarkerKill;
    private static long enemyBarUntil;
    private static float enemyBarFraction;

    private GunshipHudDraw() {
    }

    /**
     * Called by the hit feedback packet for any aircraft: flashes a marker, and when the target
     * was an airframe also refreshes the health readout under the crosshair.
     */
    public static void onHitFeedback(boolean kill, boolean aircraft, float healthFraction) {
        hitMarkerUntil = System.currentTimeMillis() + HIT_MARKER_MILLIS;
        hitMarkerKill = kill;
        if (aircraft && Float.isFinite(healthFraction)) {
            enemyBarUntil = System.currentTimeMillis() + ENEMY_BAR_MILLIS;
            enemyBarFraction = Math.max(0.0F, Math.min(1.0F, healthFraction));
        }
    }

    // ------------------------------------------------------------------
    // Centre cluster
    // ------------------------------------------------------------------

    /** Heading, speed, altitude, pitch ladder and bank scale: the same cluster for every aircraft. */
    public static void centreCluster(GuiGraphics graphics, Font font, int width, int height,
                                     float yawDeg, float pitchDeg, float rollDeg,
                                     double horizontalSpeed, double altitude) {
        int cx = width / 2;
        int cy = height / 2;
        double pixelsPerDegree = Math.max(4.0D, Math.min(9.0D, height / 70.0D));

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-rollDeg));
        for (int line = -LittleBirdHudMath.LADDER_LIMIT_DEGREES;
                line <= LittleBirdHudMath.LADDER_LIMIT_DEGREES;
                line += LittleBirdHudMath.LADDER_STEP_DEGREES) {
            if (!LittleBirdHudMath.ladderLineVisible(line, -pitchDeg,
                    LittleBirdHudMath.LADDER_WINDOW_DEGREES)) {
                continue;
            }
            int y = (int) Math.round((-pitchDeg - line) * pixelsPerDegree);
            int colour = line > 0 ? CYAN : CYAN_DIM;
            int reach = Math.max(36, width / 8);
            int half = line == 0
                    ? reach
                    : Math.max(26, reach - Math.abs(line) * 2);
            segment(graphics, -half, y, -LADDER_CENTRE_GAP_PIXELS, y, colour);
            segment(graphics, LADDER_CENTRE_GAP_PIXELS, y, half, y, colour);
            if (line != 0) {
                String label = Integer.toString(Math.abs(line));
                int labelWidth = font.width(label);
                graphics.drawString(font, label, half + 4, y - 4, colour, false);
                graphics.drawString(font, label, -half - 4 - labelWidth, y - 4, colour, false);
            }
        }
        graphics.pose().popPose();

        int arcRadius = Math.min(96, width / 7);
        int arcY = cy - 26;
        for (int tick = -60; tick <= 60; tick += 15) {
            double angle = Math.toRadians(-90.0D + tick);
            int tx = cx + (int) Math.round(Math.cos(angle) * arcRadius);
            int ty = arcY + (int) Math.round(Math.sin(angle) * arcRadius);
            graphics.fill(tx, ty, tx + 1, ty + 2, CYAN_DIM);
        }
        double pointer = Math.toRadians(-90.0D - LittleBirdHudMath.clampBankDegrees(rollDeg));
        int px = cx + (int) Math.round(Math.cos(pointer) * arcRadius);
        int py = arcY + (int) Math.round(Math.sin(pointer) * arcRadius);
        graphics.fill(px - 2, py - 2, px + 3, py + 3, CYAN);

        String heading = LittleBirdHudMath.headingLabel(yawDeg);
        readout(graphics, font, cx - font.width(heading) / 2 - 4,
                LittleBirdHudMath.clampToScreen(cy - height / 4, 4, height - 20), heading, CYAN);

        int spread = Math.max(120, Math.min(width / 4, 320));
        String speed = LittleBirdHudLayout.toKilometresPerHour(horizontalSpeed) + " "
                + Component.translatable("hud.battlefield.speed").getString();
        readout(graphics, font, cx - spread - readoutWidth(font, speed), cy + 6, speed, YELLOW);

        String altitudeText = Math.round(altitude) + " "
                + Component.translatable("hud.battlefield.altitude").getString();
        readout(graphics, font, cx + spread, cy + 6, altitudeText, YELLOW);
    }

    private static void segment(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colour) {
        graphics.fill(Math.min(x1, x2), y1, Math.max(x1, x2), y1 + 1, colour);
    }

    private static int readoutWidth(Font font, String text) {
        return font.width(text) + 8;
    }

    /** Glyph drawn inside a weapon slot: no textures, so it works at any GUI scale. */
    public enum SlotIcon {
        /** Main gun: an elongated round with a pointed tip. */
        SHELL,
        /** Machine gun: a receiver with a short belt hanging under it. */
        BELT,
        /** Discharger: three rising chevrons. */
        SMOKE
    }

    /**
     * Weapon slot in the reference's style: the selected position is a filled block, every position
     * carries a procedural icon, an optional count badge and its key legend underneath.
     */
    public static void iconSlot(GuiGraphics graphics, Font font, int x, int y, int width,
                                int height, SlotIcon icon, String badge, String key,
                                boolean selected) {
        graphics.fill(x, y, x + width, y + height, selected ? CYAN : BOX_BACKGROUND);
        if (selected) {
            graphics.fill(x, y, x + width, y + 1, WHITE);
        }
        int ink = selected ? 0xFF101418 : CYAN;
        int centreX = x + width / 2;
        int centreY = y + height / 2;
        switch (icon) {
            case SHELL -> {
                graphics.fill(centreX - 7, centreY - 1, centreX + 4, centreY + 2, ink);
                graphics.fill(centreX + 4, centreY - 2, centreX + 7, centreY + 3, ink);
            }
            case BELT -> {
                graphics.fill(centreX - 8, centreY - 3, centreX + 8, centreY, ink);
                for (int tick = 0; tick < 4; tick++) {
                    graphics.fill(centreX - 6 + tick * 4, centreY + 1, centreX - 5 + tick * 4,
                            centreY + 4, ink);
                }
            }
            case SMOKE -> {
                for (int chevron = 0; chevron < 3; chevron++) {
                    int baseY = centreY + 4 - chevron * 3;
                    for (int step = 0; step < 3; step++) {
                        graphics.fill(centreX - 5 + step, baseY - step, centreX - 4 + step,
                                baseY - step + 1, ink);
                        graphics.fill(centreX + 4 - step, baseY - step, centreX + 5 - step,
                                baseY - step + 1, ink);
                    }
                }
            }
        }
        if (badge != null && !badge.isEmpty()) {
            graphics.drawString(font, badge, x + width - 2 - font.width(badge), y + height - 9,
                    selected ? 0xFF101418 : TEXT, false);
        }
        if (key != null && !key.isEmpty()) {
            graphics.drawString(font, key, centreX - font.width(key) / 2, y + height + 2,
                    selected ? WHITE : CYAN_DIM, false);
        }
    }

    /**
     * Ammunition readout: shell name, the ready count in large digits with a reload arc, and the
     * reserve to its right.
     */
    public static void ammoCounter(GuiGraphics graphics, Font font, int x, int y, int width,
                                   String shellName, String readyDigits, String reserve,
                                   float reloadFraction, boolean resupplying) {
        graphics.fill(x, y, x + width, y + 1, CYAN);
        graphics.drawString(font, shellName, x + width - font.width(shellName), y + 3, CYAN, false);

        int arcCentreX = x + 8;
        int arcCentreY = y + 20;
        int lit = Math.round(Math.max(0.0F, Math.min(1.0F, reloadFraction)) * 8.0F);
        for (int dot = 0; dot < 8; dot++) {
            double angle = Math.PI * 0.75D + dot * (Math.PI * 1.5D / 8.0D);
            int dotX = arcCentreX + (int) Math.round(Math.cos(angle) * 6.0D);
            int dotY = arcCentreY + (int) Math.round(Math.sin(angle) * 6.0D);
            graphics.fill(dotX, dotY, dotX + 1, dotY + 1, dot < lit ? CYAN : CYAN_DIM);
        }
        graphics.pose().pushPose();
        graphics.pose().scale(1.6F, 1.6F, 1.0F);
        graphics.drawString(font, readyDigits, (int) ((x + 20) / 1.6F), (int) ((y + 11) / 1.6F),
                WHITE, false);
        graphics.pose().popPose();
        // The reserve dims while its own cooldown is running back up, so the two clocks read apart:
        // the ring is the reload, the dimmed reserve is the resupply.
        graphics.drawString(font, reserve, x + width - font.width(reserve), y + 20,
                resupplying ? CYAN_DIM : TEXT, false);
    }

    /** Vehicle name and a ringed health figure, the reference's bottom line of the readout. */
    public static void vehicleChip(GuiGraphics graphics, Font font, int x, int y, int width,
                                   String name, float healthFraction, String healthText) {
        // One line only: name, then the health ring, then the value. Everything stays inside the
        // raster line the caller hands us, so the chip can never spill out of the readout box and
        // touch the weapon bar below it.
        int ringColour = healthFraction < 0.35F ? DANGER : CYAN;
        graphics.drawString(font, name, x + width - 46 - font.width(name), y, CYAN, false);
        int ringX = x + width - 36;
        int ringY = y + 4;
        for (int dot = 0; dot < 10; dot++) {
            double angle = dot * (Math.PI * 2.0D / 10.0D);
            int dotX = ringX + (int) Math.round(Math.cos(angle) * 4.0D);
            int dotY = ringY + (int) Math.round(Math.sin(angle) * 4.0D);
            graphics.fill(dotX, dotY, dotX + 1, dotY + 1, ringColour);
        }
        graphics.drawString(font, healthText, x + width - 26, y, ringColour, false);
    }

    /** Boxed readout with a coloured top edge, like the reference HUD. */
    public static void readout(GuiGraphics graphics, Font font, int x, int y,
                               String text, int colour) {
        int width = font.width(text) + 8;
        graphics.fill(x, y, x + width, y + 14, BOX_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.drawString(font, text, x + 4, y + 3, colour, false);
    }

    // ------------------------------------------------------------------
    // Panels and bars
    // ------------------------------------------------------------------

    /** Panel background with a coloured top edge. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        graphics.fill(x, y, x + width, y + height, BOX_BACKGROUND);
        graphics.fill(x, y, x + width, y + 1, accent);
    }

    /** Horizontal bar: dark track plus a fill that empties from the right. */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height,
                           float fraction, int colour) {
        graphics.fill(x, y, x + width, y + height, BAR_TRACK);
        float clamped = Float.isFinite(fraction) ? Math.max(0.0F, Math.min(1.0F, fraction)) : 0.0F;
        int filled = Math.round(width * clamped);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + height, colour);
        }
    }

    /** Weapon slot box: key label, state text or an icon, highlighted when selected. */
    public static void slot(GuiGraphics graphics, Font font, int x, int y, String key,
                            String state, boolean selected, boolean gunIcon) {
        graphics.fill(x, y, x + SLOT_WIDTH, y + SLOT_HEIGHT, selected ? CYAN : BOX_BACKGROUND);
        int textColour = selected ? 0xFF101418 : TEXT;
        graphics.drawString(font, key, x + 3, y + 2, textColour, false);
        if (gunIcon) {
            graphics.fill(x + 3, y + 12, x + SLOT_WIDTH - 3, y + 13, textColour);
            for (int tick = 0; tick < 3; tick++) {
                int tickX = x + 8 + tick * 8;
                graphics.fill(tickX, y + 14, tickX + 1, y + 17, textColour);
            }
        } else {
            graphics.drawString(font, state, x + 3, y + 11, textColour, false);
        }
    }

    // ------------------------------------------------------------------
    // Alerts, markers and the shared readouts
    // ------------------------------------------------------------------

    /** Shows the first alert only, centred above the cluster. */
    public static void alerts(GuiGraphics graphics, Font font, List<Component> alerts,
                              int width, int height) {
        if (alerts == null || alerts.isEmpty()) {
            return;
        }
        graphics.drawCenteredString(font, alerts.get(0), width / 2,
                Math.max(4, height / 4 - 18), WARNING);
    }

    /**
     * Full reference rocket reticle: per pod a pair of dot columns holding one dot per tube
     * (the inner column's top cell empty) plus the interrupted bracket, with a white circle
     * marking the predicted rocket impact point.
     */
    public static void rocketReticle(GuiGraphics graphics, int x, int y,
                                     int leftRounds, int rightRounds) {
        drawBrackets(graphics, x, y);
        drawPodDots(graphics, x, y, leftRounds, true);
        drawPodDots(graphics, x, y, rightRounds, false);
        drawCircle(graphics, x, y, ROCKET_CIRCLE_RADIUS_PIXELS, WHITE);
    }

    /**
     * Rocket reticle for the Apache: ten tubes on the left wing and nine on the right, which is
     * how its ready magazine really splits, so the dots are the crew's ammunition readout.
     */
    public static void apacheRocketReticle(GuiGraphics graphics, int x, int y, int readyRockets) {
        drawBrackets(graphics, x, y);
        drawApachePodDots(graphics, x, y, readyRockets, 0, true);
        drawApachePodDots(graphics, x, y, readyRockets, 1, false);
        drawCircle(graphics, x, y, ROCKET_CIRCLE_RADIUS_PIXELS, WHITE);
    }

    /** One dot per loaded tube of one pod, in that pod's own grid. */
    private static void drawApachePodDots(GuiGraphics graphics, int x, int y,
                                          int readyRockets, int pod, boolean leftSide) {
        for (int tube = 0; tube < ApachePods.tubeCount(pod); tube++) {
            if (!ApachePods.tubeLoaded(pod, tube, readyRockets)) {
                continue;
            }
            ApachePods.DotPosition dot = ApachePods.tubeDot(pod, tube);
            int columnOffset = (dot.column() + 1) * ROCKET_DOT_COLUMN_SPACING_PIXELS;
            int dotX = leftSide
                    ? x - ROCKET_BAR_OFFSET_PIXELS - columnOffset
                    : x + ROCKET_BAR_OFFSET_PIXELS + columnOffset;
            int dotY = y + Math.round((dot.row() - (ApachePods.HUD_ROWS - 1) * 0.5F)
                    * ROCKET_DOT_ROW_SPACING_PIXELS);
            graphics.fill(dotX, dotY, dotX + ROCKET_DOT_SIZE_PIXELS,
                    dotY + ROCKET_DOT_SIZE_PIXELS, ROCKET_RETICLE_COLOR);
        }
    }

    private static void drawBrackets(GuiGraphics graphics, int x, int y) {
        drawBracket(graphics, x - ROCKET_BAR_OFFSET_PIXELS, y);
        drawBracket(graphics, x + ROCKET_BAR_OFFSET_PIXELS, y);
    }

    private static void drawBracket(GuiGraphics graphics, int barX, int y) {
        int half = ROCKET_BAR_HALF_HEIGHT_PIXELS;
        int gap = ROCKET_BAR_GAP_PIXELS / 2;
        graphics.fill(barX, y - half, barX + ROCKET_BAR_WIDTH_PIXELS, y - gap,
                ROCKET_RETICLE_COLOR);
        graphics.fill(barX, y + gap, barX + ROCKET_BAR_WIDTH_PIXELS, y + half,
                ROCKET_RETICLE_COLOR);
    }

    /** One dot per loaded tube, in the pod's own column layout. */
    private static void drawPodDots(
            GuiGraphics graphics, int x, int y, int remaining, boolean leftSide) {
        for (int tube = 0; tube < LittleBirdCrosshairMath.TUBES_PER_POD; tube++) {
            if (!LittleBirdPodTubes.loaded(tube, remaining)) {
                continue;
            }
            LittleBirdCrosshairMath.DotPosition dot = LittleBirdCrosshairMath.tubeDot(tube);
            int columnOffset = (dot.column() + 1) * ROCKET_DOT_COLUMN_SPACING_PIXELS;
            int dotX = leftSide
                    ? x - ROCKET_BAR_OFFSET_PIXELS - columnOffset
                    : x + ROCKET_BAR_OFFSET_PIXELS + columnOffset;
            int dotY = y + Math.round((dot.row() - 1.5F) * ROCKET_DOT_ROW_SPACING_PIXELS);
            graphics.fill(dotX, dotY, dotX + ROCKET_DOT_SIZE_PIXELS,
                    dotY + ROCKET_DOT_SIZE_PIXELS, ROCKET_RETICLE_COLOR);
        }
    }

    /** Open circle, the reference's white impact marker. */
    private static void drawCircle(GuiGraphics graphics, int centreX, int centreY,
                                   int radius, int colour) {
        for (int step = 0; step < 24; step++) {
            double angle = Math.toRadians(step * 15.0D);
            int x = centreX + (int) Math.round(Math.cos(angle) * radius);
            int y = centreY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 1, y + 1, colour);
        }
    }

    /** Optional plain crosshair for pilots who want a fixed reference. */
    public static void crosshair(GuiGraphics graphics, int cx, int cy) {
        graphics.fill(cx - 9, cy - 1, cx - 3, cy, CYAN);
        graphics.fill(cx + 3, cy - 1, cx + 9, cy, CYAN);
        graphics.fill(cx - 1, cy - 9, cx, cy - 3, CYAN);
        graphics.fill(cx - 1, cy + 3, cx, cy + 9, CYAN);
    }

    /**
     * Heavy main gun cross: long arms and a wide gap, so it reads as a single slow shot next to
     * the coaxial reticle.
     */
    public static void mainGunCrosshair(GuiGraphics graphics, int cx, int cy, int colour) {
        graphics.fill(cx - 14, cy - 1, cx - 6, cy, colour);
        graphics.fill(cx + 6, cy - 1, cx + 14, cy, colour);
        graphics.fill(cx - 1, cy - 14, cx, cy - 6, colour);
        graphics.fill(cx - 1, cy + 6, cx, cy + 14, colour);
    }

    /**
     * Coaxial reticle: short arms sitting on a gap, plus corner ticks so a rapid-fire weapon is
     * told apart from the main gun even when both are on screen.
     */
    public static void coaxCrosshair(GuiGraphics graphics, int cx, int cy, int gapPixels,
                                     int colour) {
        int gap = Math.max(1, gapPixels);
        graphics.fill(cx - gap - 5, cy - 1, cx - gap, cy, colour);
        graphics.fill(cx + gap, cy - 1, cx + gap + 5, cy, colour);
        graphics.fill(cx - 1, cy - gap - 5, cx, cy - gap, colour);
        graphics.fill(cx - 1, cy + gap, cx, cy + gap + 5, colour);
        for (int i = 0; i < 3; i++) {
            graphics.fill(cx + gap + 1 + i, cy + gap + 1 + i, cx + gap + 2 + i, cy + gap + 2 + i,
                    colour);
            graphics.fill(cx - gap - 2 - i, cy + gap + 1 + i, cx - gap - 1 - i, cy + gap + 2 + i,
                    colour);
            graphics.fill(cx + gap + 1 + i, cy - gap - 2 - i, cx + gap + 2 + i, cy - gap - 1 - i,
                    colour);
            graphics.fill(cx - gap - 2 - i, cy - gap - 2 - i, cx - gap - 1 - i, cy - gap - 1 - i,
                    colour);
        }
    }

    /** Four diagonal ticks that flash when a burst connects. */
    public static void hitMarker(GuiGraphics graphics, int cx, int cy) {
        if (System.currentTimeMillis() > hitMarkerUntil) {
            return;
        }
        int colour = hitMarkerKill ? DANGER : HIT_MARKER_COLOR;
        int arm = hitMarkerKill ? 8 : 6;
        for (int i = 0; i < arm; i++) {
            graphics.fill(cx - 12 - i, cy - 12 - i, cx - 11 - i, cy - 11 - i, colour);
            graphics.fill(cx + 11 + i, cy - 12 - i, cx + 12 + i, cy - 11 - i, colour);
            graphics.fill(cx - 12 - i, cy + 11 + i, cx - 11 - i, cy + 12 + i, colour);
            graphics.fill(cx + 11 + i, cy + 11 + i, cx + 12 + i, cy + 12 + i, colour);
        }
    }

    /**
     * Enemy airframe readout: a small red bar under the crosshair that empties from its right
     * hand end, with the remaining percentage below it. Only up for a few seconds after a hit.
     */
    public static void enemyHealthBar(GuiGraphics graphics, Font font, int width, int height) {
        if (System.currentTimeMillis() > enemyBarUntil) {
            return;
        }
        int x = width / 2 - ENEMY_BAR_WIDTH / 2;
        int y = height / 2 + ENEMY_BAR_OFFSET_PIXELS;
        graphics.fill(x - 1, y - 1, x + ENEMY_BAR_WIDTH + 1, y + ENEMY_BAR_HEIGHT + 1, DANGER);
        graphics.fill(x, y, x + ENEMY_BAR_WIDTH, y + ENEMY_BAR_HEIGHT, BAR_TRACK);
        int filled = LittleBirdHudLayout.enemyHealthFillWidth(enemyBarFraction, ENEMY_BAR_WIDTH);
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + ENEMY_BAR_HEIGHT, DANGER);
        }
        String label = LittleBirdHudMath.healthPercentLabel(enemyBarFraction);
        graphics.drawString(font, label, width / 2 - font.width(label) / 2,
                y + ENEMY_BAR_HEIGHT + 3, DANGER, false);
    }
}
