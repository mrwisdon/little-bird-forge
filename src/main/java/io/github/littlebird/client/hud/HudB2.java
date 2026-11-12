package io.github.littlebird.client.hud;

import com.mojang.math.Axis;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.B2BombCam;
import io.github.littlebird.client.B2CockpitMath;
import io.github.littlebird.client.B2HudLayout;
import io.github.littlebird.client.B2KillFeed;
import io.github.littlebird.client.B2PodMath;
import io.github.littlebird.client.B2PodView;
import io.github.littlebird.client.LittleBirdHudLayout;
import io.github.littlebird.client.LittleBirdHudMath;
import io.github.littlebird.client.LittleBirdKeyMappings;
import io.github.littlebird.entity.B2Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The B-2's two HUDs in one view: the electro-optical pod sight picture while the pod is up, and
 * the cockpit bombing HUD otherwise (rigid frame, flight cluster, bomb sight and bomb bay panel).
 *
 * <p>Everything numeric comes from the already-tested helpers - {@link B2PodView} and
 * {@link B2PodMath} for the pod, {@link B2CockpitMath} for the frame, {@link B2HudLayout} for the
 * panel geometry, {@link B2KillFeed} for the feed - so this class only decides what to draw and
 * where. All drawing goes through {@link HudCanvas} and {@link HudStyle}, which is what gives the
 * whole vehicle set one look.
 *
 * <p>The crew's kill feed and the combat feedback belong to the aeroplane rather than to one view:
 * they stay up in the pod and in the cockpit alike, and they are drawn last so the pod's
 * monochrome mask cannot dim them.
 */
public final class HudB2 implements HudView {
    /** Pod picture treatment: a green monochrome wash over the world. */
    private static final int MONO_TINT = 0x2A6FFF9C;
    private static final int SCAN_LINE = 0x20000000;
    private static final int VIGNETTE = 0x77000000;
    /** Sight ink: the pod reticle is brighter than the palette so it reads over the wash. */
    private static final int SIGHT = 0xFFD9FFE4;

    private static final int POD_PANEL_MIN_WIDTH = 190;
    private static final int POD_PANEL_TOP = 12;
    private static final int POD_PANEL_LEFT = 10;

    /** Central reticle and the outer bracket ring of the pod sight. */
    private static final int SIGHT_RETICLE_GAP = 5;
    private static final int SIGHT_RETICLE_ARM = 9;
    private static final int SIGHT_BRACKET_REACH = 60;
    private static final int SIGHT_BRACKET_INNER = 20;

    @Override
    public boolean appliesTo(HudContext context) {
        return context.minecraft.player != null
                && context.minecraft.player.getVehicle() instanceof B2Entity;
    }

    @Override
    public void render(HudContext context) {
        if (!(context.minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return;
        }
        if (B2BombCam.isActive()) {
            // Watching the stick fall is its own view: no cockpit frame over the bomb camera and no
            // pod mask, but the crew still gets the feed and the hit feedback.
            drawKillFeed(context);
            HudFeedback.hitMarker(context.graphics, context.centreX(), context.centreY());
            HudFeedback.enemyBar(context.graphics, context.font, context.width, context.height);
            return;
        }
        if (B2PodView.isActive()) {
            renderPod(context, b2);
        } else {
            renderCockpit(context, b2);
        }
        // On top of both pictures: the feed and the shared combat feedback.
        drawKillFeed(context);
        HudFeedback.hitMarker(context.graphics, context.centreX(), context.centreY());
        HudFeedback.enemyBar(context.graphics, context.font, context.width, context.height);
    }

    // ------------------------------------------------------------------
    // Pod: the sight picture
    // ------------------------------------------------------------------

    private void renderPod(HudContext context, B2Entity b2) {
        GuiGraphics graphics = context.graphics;
        int width = context.width;
        int height = context.height;

        // The electro-optical picture is monochrome, scan lined and vignetted, so the crew never
        // mistakes it for the naked eye view.
        graphics.fill(0, 0, width, height, MONO_TINT);
        for (int y = 0; y < height; y += 4) {
            graphics.fill(0, y, width, y + 1, SCAN_LINE);
        }
        int band = Math.max(8, height / 12);
        graphics.fill(0, 0, width, band, VIGNETTE);
        graphics.fill(0, height - band, width, height, VIGNETTE);
        graphics.fill(0, 0, band, height, VIGNETTE);
        graphics.fill(width - band, 0, width, height, VIGNETTE);

        podReticle(context, b2);
        podPanel(context, b2);
        HudCanvas.line(graphics, context.font, 12, height - 24,
                "EJECT POD  [" + key(LittleBirdKeyMappings.B2_POD) + "]", HudStyle.MUTED);
    }

    /** Gimbal reticle, lock brackets and the marker for the predicted impact. */
    private void podReticle(HudContext context, B2Entity b2) {
        GuiGraphics graphics = context.graphics;
        Font font = context.font;
        int cx = context.centreX();
        int cy = context.centreY();

        HudCanvas.reticle(graphics, cx, cy, SIGHT_RETICLE_GAP, SIGHT_RETICLE_ARM, SIGHT);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, SIGHT);
        cornerBrackets(graphics, cx, cy, SIGHT_BRACKET_REACH, SIGHT_BRACKET_INNER, SIGHT);

        // The old overlay compared the gimbal with itself and so called every frame "locked"; the
        // brackets now mean what a crew reads them as - a designation is held.
        if (B2PodView.designation() != null) {
            graphics.fill(cx - 34, cy - 20, cx - 30, cy - 16, HudStyle.OK);
            graphics.fill(cx + 30, cy - 20, cx + 34, cy - 16, HudStyle.OK);
            graphics.fill(cx - 34, cy + 16, cx - 30, cy + 20, HudStyle.OK);
            graphics.fill(cx + 30, cy + 16, cx + 34, cy + 20, HudStyle.OK);
        }

        int[] marker = B2PodView.impactMarkerOnScreen(context.width, context.height);
        if (marker == null) {
            return;
        }
        boolean designated = B2PodView.designation() != null;
        boolean ready = B2PodView.impactReady();
        int colour = ready ? HudStyle.OK : HudStyle.WARN;
        graphics.fill(marker[0] - 7, marker[1] - 1, marker[0] + 8, marker[1] + 1, colour);
        graphics.fill(marker[0] - 1, marker[1] - 7, marker[0] + 1, marker[1] + 8, colour);
        graphics.fill(marker[0] - 5, marker[1] - 5, marker[0] + 6, marker[1] - 4, colour);
        graphics.fill(marker[0] - 5, marker[1] + 5, marker[0] + 6, marker[1] + 6, colour);
        if (!designated) {
            return;
        }
        String text = distanceText("IMPACT", B2PodView.impactDistance()) + "   " + missText();
        int textX = Math.min(marker[0] + 10, context.width - font.width(text) - 2);
        graphics.drawString(font, text, textX, marker[1] + 6, colour, true);
        if (ready) {
            graphics.drawString(font, "RELEASE", textX, marker[1] + 18, HudStyle.OK, true);
        }
    }

    /** One panel for every pod reading the crew flies the run on. */
    private void podPanel(HudContext context, B2Entity b2) {
        GuiGraphics graphics = context.graphics;
        Font font = context.font;
        List<String> texts = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();

        double[] window = B2PodView.sightWindow();
        boolean atLimit = Math.abs(B2PodView.gimbalPitch() - window[1]) < 0.6D
                || Math.abs(B2PodView.gimbalPitch() + window[0]) < 0.6D;
        texts.add(String.format(Locale.ROOT, "GIMBAL  Y %+05.1f  P %+05.1f",
                B2PodView.gimbalYaw(), B2PodView.gimbalPitch()));
        colours.add(HudStyle.MUTED);
        texts.add(String.format(Locale.ROOT, "SIGHT   UP %.0f  DOWN %.0f%s",
                window[0], window[1], atLimit ? "  [AT LIMIT]" : ""));
        colours.add(atLimit ? HudStyle.WARN : HudStyle.MUTED);
        texts.add(String.format(Locale.ROOT, "ZOOM    FOV %.1f deg", B2PodView.zoomFov()));
        colours.add(HudStyle.MUTED);
        double range = B2PodView.rangeToSight();
        texts.add(range < 0.0D ? "RANGE   ---"
                : String.format(Locale.ROOT, "RANGE   %.0f m", range));
        colours.add(range < 0.0D ? HudStyle.MUTED : HudStyle.TEXT);

        double[] designation = B2PodView.designation();
        if (designation == null) {
            texts.add("DESIGNATE  [LMB]");
            colours.add(HudStyle.TEXT);
        } else {
            texts.add(String.format(Locale.ROOT, "TARGET  %.0f %.0f %.0f",
                    designation[0], designation[1], designation[2]));
            colours.add(HudStyle.OK);
            texts.add("CLEAR  [RMB]");
            colours.add(HudStyle.MUTED);
        }
        boolean ready = designation != null && B2PodView.impactReady();
        texts.add(dropLine());
        colours.add(ready ? HudStyle.OK : HudStyle.TEXT);
        texts.add(B2PodView.levelWarning() ? "BOMB RUN  BANK - HOLD" : "BOMB RUN  LEVEL");
        colours.add(B2PodView.levelWarning() ? HudStyle.WARN : HudStyle.TEXT);

        int content = 0;
        for (String text : texts) {
            content = Math.max(content, font.width(text));
        }
        int panelWidth = Math.max(POD_PANEL_MIN_WIDTH, content + HudStyle.PAD * 2);
        panelWidth = Math.min(panelWidth, Math.max(120, context.width - 20));
        int panelHeight = HudStyle.HEADER + HudStyle.PAD * 2 + texts.size() * HudStyle.ROW;
        int x = POD_PANEL_LEFT;
        int y = POD_PANEL_TOP;
        String title = "EO POD  \u00b7  B-2";
        String tag = designation == null ? "NO TGT" : "LOCK";
        HudCanvas.panel(graphics, font, title, tag, x, y, panelWidth, panelHeight,
                HudStyle.ACCENT);
        int contentX = x + HudStyle.PAD;
        int rowY = y + HudStyle.HEADER + HudStyle.PAD;
        for (int i = 0; i < texts.size(); i++) {
            HudCanvas.line(graphics, font, contentX, rowY, texts.get(i), colours.get(i));
            rowY += HudStyle.ROW;
        }
    }

    /** What the release key will do next, from the pod seat. */
    private static String dropLine() {
        if (B2PodView.designation() == null) {
            return "DROP  [" + key(LittleBirdKeyMappings.SALVO) + "]  AUTO-DESIGNS";
        }
        if (B2PodView.impactReady()) {
            return String.format(Locale.ROOT, "DROP NOW  [%s]   LEAD %dt",
                    key(LittleBirdKeyMappings.SALVO), B2PodView.releaseLeadTicks());
        }
        if (B2PodView.isAutoArmed()) {
            return "AUTO ARMED  " + missText();
        }
        return String.format(Locale.ROOT, "ARM AUTO  [%s]  %s  LEAD %dt",
                key(LittleBirdKeyMappings.RELOAD), missText(), B2PodView.releaseLeadTicks());
    }

    // ------------------------------------------------------------------
    // Cockpit: frame, cluster, bomb sight and bomb bay panel
    // ------------------------------------------------------------------

    private void renderCockpit(HudContext context, B2Entity b2) {
        if (b2.isCockpitView()) {
            cockpitFrame(context);
        }
        centreCluster(context, b2);
        bombSight(context, b2);
        bombPanel(context, b2);
    }

    /** The rigid cockpit: two windscreen panes split by a centre spine, glare shield below. */
    private static void cockpitFrame(HudContext context) {
        GuiGraphics graphics = context.graphics;
        int width = context.width;
        int height = context.height;
        int[] screen = B2CockpitMath.windscreenRect(width, height);
        graphics.fill(0, 0, width, screen[1], HudStyle.PANEL);
        graphics.fill(0, screen[3], width, height, HudStyle.PANEL);
        graphics.fill(0, screen[1], screen[0], screen[3], HudStyle.PANEL);
        graphics.fill(screen[2], screen[1], width, screen[3], HudStyle.PANEL);
        int[] spine = B2CockpitMath.spineRect(width, height);
        graphics.fill(spine[0], spine[1], spine[2], spine[3], HudStyle.PANEL);
        int[] glare = B2CockpitMath.glareShieldRect(width, height);
        graphics.fill(glare[0], glare[1], glare[2], glare[3], HudStyle.PANEL);
        // The windscreen rim catches the light, which is what makes the frame read as structure.
        graphics.fill(screen[0], screen[1], screen[2], screen[1] + 2, HudStyle.EDGE);
        graphics.fill(screen[0], screen[3] - 2, screen[2], screen[3], HudStyle.EDGE);
        graphics.fill(glare[0], glare[1], glare[2], glare[1] + 2, HudStyle.EDGE);
    }

    /**
     * Heading, pitch ladder, bank scale, speed and altitude: the flight cluster the B-2 shares
     * with the rest of the pack. The ladder rolls opposite the airframe so the horizon stays level
     * with the world, exactly like the gunships.
     */
    private static void centreCluster(HudContext context, B2Entity b2) {
        GuiGraphics graphics = context.graphics;
        Font font = context.font;
        int cx = context.centreX();
        int cy = context.centreY();
        float pitch = b2.getXRot();
        float roll = b2.interpolatedRenderRollDeg(context.partialTick);
        double pixelsPerDegree = Math.max(4.0D, Math.min(9.0D, context.height / 70.0D));

        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0.0F);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-roll));
        int reach = Math.max(36, context.width / 8);
        for (int line = -LittleBirdHudMath.LADDER_LIMIT_DEGREES;
                line <= LittleBirdHudMath.LADDER_LIMIT_DEGREES;
                line += LittleBirdHudMath.LADDER_STEP_DEGREES) {
            if (!LittleBirdHudMath.ladderLineVisible(line, -pitch,
                    LittleBirdHudMath.LADDER_WINDOW_DEGREES)) {
                continue;
            }
            int y = (int) Math.round((-pitch - line) * pixelsPerDegree);
            int colour = line > 0 ? HudStyle.ACCENT : HudStyle.ACCENT_DIM;
            int half = line == 0 ? reach : Math.max(26, reach - Math.abs(line) * 2);
            HudCanvas.line(graphics, -half, y, -8, y, colour);
            HudCanvas.line(graphics, 8, y, half, y, colour);
            if (line != 0) {
                String label = Integer.toString(Math.abs(line));
                graphics.drawString(font, label, half + 4, y - 4, colour, false);
                graphics.drawString(font, label, -half - 4 - font.width(label), y - 4, colour,
                        false);
            }
        }
        graphics.pose().popPose();

        int arcRadius = Math.min(96, context.width / 7);
        int arcY = cy - 26;
        for (int tick = -60; tick <= 60; tick += 15) {
            double angle = Math.toRadians(-90.0D + tick);
            int tx = cx + (int) Math.round(Math.cos(angle) * arcRadius);
            int ty = arcY + (int) Math.round(Math.sin(angle) * arcRadius);
            graphics.fill(tx, ty, tx + 1, ty + 2, HudStyle.ACCENT_DIM);
        }
        double pointer = Math.toRadians(-90.0D - LittleBirdHudMath.clampBankDegrees(roll));
        int px = cx + (int) Math.round(Math.cos(pointer) * arcRadius);
        int py = arcY + (int) Math.round(Math.sin(pointer) * arcRadius);
        graphics.fill(px - 2, py - 2, px + 3, py + 3, HudStyle.ACCENT);

        String heading = LittleBirdHudMath.headingLabel(b2.getYRot());
        readout(graphics, font, cx - font.width(heading) / 2 - 4,
                LittleBirdHudMath.clampToScreen(cy - context.height / 4, 4, context.height - 20),
                heading, HudStyle.ACCENT);

        int spread = Math.max(120, Math.min(context.width / 4, 320));
        double dx = b2.getDeltaMovement().x;
        double dz = b2.getDeltaMovement().z;
        double horizontalSpeed = Math.sqrt(dx * dx + dz * dz);
        String speed = LittleBirdHudLayout.toKilometresPerHour(horizontalSpeed) + " "
                + Component.translatable("hud.battlefield.speed").getString();
        readout(graphics, font, cx - spread - readoutWidth(font, speed), cy + 6, speed,
                HudStyle.CYAN);
        String altitude = Math.round(b2.getY()) + " "
                + Component.translatable("hud.battlefield.altitude").getString();
        readout(graphics, font, cx + spread, cy + 6, altitude, HudStyle.CYAN);
    }

    /**
     * The bombing sight: a pipper on the nose axis plus the spot a stick released right now would
     * land on, joined by a drop line. The crew aims the pipper and watches the marker walk in.
     */
    private static void bombSight(HudContext context, B2Entity b2) {
        GuiGraphics graphics = context.graphics;
        Font font = context.font;
        int cx = context.centreX();
        int cy = context.centreY();
        HudCanvas.reticle(graphics, cx, cy, SIGHT_RETICLE_GAP, SIGHT_RETICLE_ARM,
                HudStyle.ACCENT);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, HudStyle.ACCENT);

        double[] impact = B2PodView.impactPoint3();
        if (impact == null) {
            return;
        }
        double[] origin = B2PodView.releasePoint(b2);
        double[] offset = B2PodMath.angularOffset(origin[0], origin[1], origin[2],
                b2.getYRot(), b2.getXRot(), impact);
        if (offset == null) {
            return;
        }
        double fov = viewFov(context, b2);
        boolean ready = B2PodView.impactReady();
        int colour = ready ? HudStyle.OK : HudStyle.WARN;
        if (B2PodMath.offScreen(offset[0], offset[1], fov)) {
            String offSight = distanceText("IMPACT", B2PodView.impactDistance())
                    + " OFF SIGHT";
            graphics.drawString(font, offSight, cx + 16, cy + 8, colour, true);
            return;
        }
        int[] marker = B2PodMath.projectToScreen(offset[0], offset[1], fov, context.width,
                context.height);
        // Dotted drop line from the nose axis down to the predicted impact.
        for (int i = 1; i <= 8; i++) {
            int lx = cx + (marker[0] - cx) * i / 9;
            int ly = cy + (marker[1] - cy) * i / 9;
            graphics.fill(lx, ly, lx + 1, ly + 1, ready ? HudStyle.OK : HudStyle.EDGE);
        }
        graphics.fill(marker[0] - 7, marker[1] - 1, marker[0] + 8, marker[1] + 1, colour);
        graphics.fill(marker[0] - 1, marker[1] - 7, marker[0] + 1, marker[1] + 8, colour);
        graphics.fill(marker[0] - 5, marker[1] - 5, marker[0] + 6, marker[1] - 4, colour);
        graphics.fill(marker[0] - 5, marker[1] + 5, marker[0] + 6, marker[1] + 6, colour);

        String text = B2PodView.designation() == null
                ? distanceText("IMPACT", B2PodView.impactDistance())
                : distanceText("IMPACT", B2PodView.impactDistance()) + "   " + missText();
        int textX = Math.min(marker[0] + 10, context.width - font.width(text) - 2);
        graphics.drawString(font, text, textX, marker[1] + 6, colour, true);
        if (ready) {
            graphics.drawString(font, "RELEASE", textX, marker[1] + 18, HudStyle.OK, true);
        }
    }

    /**
     * Bomb bay panel: the selected store, rounds left with the bay gauge, whether the doors are
     * open, whether the pod holds a designation and what the release key will do next, plus the key
     * hints when the screen is roomy enough for them.
     */
    private static void bombPanel(HudContext context, B2Entity b2) {
        GuiGraphics graphics = context.graphics;
        Font font = context.font;
        int weapon = b2.getBombWeapon();
        int rounds = b2.bombCount(weapon);
        int capacity = Math.max(1, b2.bombCapacity(weapon));
        String store = weapon == B2Entity.WEAPON_INCENDIARY ? "INCENDIARY" : "JDAM";
        String count = rounds + " / " + capacity;
        String bay = b2.bombBayOpen() ? "BAY OPEN" : "BAY CLOSED";
        String target = b2.hasPodDesignation() ? "TARGET SET" : "NO TARGET";
        boolean ready = b2.hasPodDesignation() && B2PodView.impactReady();
        String cue = cueText(b2, ready);
        // Key hints only when there is room for them: on a small logical screen (a big GUI scale in
        // fullscreen) they are dropped rather than squeezed into the corner.
        boolean hints = B2HudLayout.hintsFit(context.width, context.height);
        String hint1 = hints ? "[" + key(LittleBirdKeyMappings.SALVO) + "] DROP   [SHIFT+"
                + key(LittleBirdKeyMappings.SALVO) + "] SALVO" : null;
        String hint2 = hints ? "[" + key(LittleBirdKeyMappings.RELOAD) + "] AUTO  ["
                + key(LittleBirdKeyMappings.WEAPON_SWITCH) + "] STORE  ["
                + key(LittleBirdKeyMappings.B2_POD) + "] POD  ["
                + key(LittleBirdKeyMappings.COCKPIT) + "] VIEW" : null;

        int contentWidth = font.width("BOMB BAY") + HudStyle.PAD * 2 + font.width(store);
        contentWidth = Math.max(contentWidth, font.width("ROUNDS") + 8 + font.width(count));
        contentWidth = Math.max(contentWidth, font.width("BAY") + 8 + font.width(bay));
        contentWidth = Math.max(contentWidth, font.width("TARGET") + 8 + font.width(target));
        contentWidth = Math.max(contentWidth, font.width(cue));
        if (hint1 != null) {
            contentWidth = Math.max(contentWidth, font.width(hint1));
            contentWidth = Math.max(contentWidth, font.width(hint2));
        }
        // All the corner rules live in B2HudLayout, which is unit tested: measured width, inside
        // the screen, and lifted clear of the hotbar when the two would share the bottom row.
        B2HudLayout.Panel box = B2HudLayout.panelFor(context.width, context.height, contentWidth,
                hints);
        HudCanvas.panel(graphics, font, "BOMB BAY", store, box.x(), box.y(), box.width(),
                box.height(), HudStyle.EDGE);

        int left = box.x() + HudStyle.PAD;
        int right = box.x() + box.width() - HudStyle.PAD;
        int rowY = box.y() + HudStyle.HEADER + HudStyle.PAD;
        int countColour = rounds > 0 ? HudStyle.OK : HudStyle.WARN;
        HudCanvas.row(graphics, font, left, right, rowY, "ROUNDS", count, countColour);
        rowY += HudStyle.ROW;
        HudCanvas.bar(graphics, left, rowY, box.width() - HudStyle.PAD * 2, HudStyle.BAR,
                rounds / (double) capacity, countColour);
        rowY += HudStyle.BAR + 6;
        HudCanvas.row(graphics, font, left, right, rowY, "BAY", bay,
                b2.bombBayOpen() ? HudStyle.WARN : HudStyle.TEXT);
        rowY += HudStyle.ROW;
        HudCanvas.row(graphics, font, left, right, rowY, "TARGET", target,
                b2.hasPodDesignation() ? HudStyle.OK : HudStyle.WARN);
        rowY += HudStyle.ROW;
        HudCanvas.line(graphics, font, left, rowY, cue, ready ? HudStyle.OK : HudStyle.WARN);
        rowY += HudStyle.ROW;
        if (hint1 != null) {
            HudCanvas.line(graphics, font, left, rowY, hint1, HudStyle.ACCENT_DIM);
            HudCanvas.line(graphics, font, left, rowY + HudStyle.ROW, hint2, HudStyle.ACCENT_DIM);
        }
    }

    /** One line telling the crew what the release key will do next. */
    private static String cueText(B2Entity b2, boolean ready) {
        if (!b2.hasPodDesignation()) {
            return "[" + key(LittleBirdKeyMappings.SALVO) + "] DROP  (AUTO-DESIGNS)";
        }
        if (ready) {
            return "[" + key(LittleBirdKeyMappings.SALVO) + "] DROP NOW  (IN THE ZONE)";
        }
        if (B2PodView.isAutoArmed()) {
            return "AUTO ARMED - FIRES AT THE LINE";
        }
        return "[" + key(LittleBirdKeyMappings.RELOAD) + "] ARM AUTO RELEASE";
    }

    // ------------------------------------------------------------------
    // Shared pieces
    // ------------------------------------------------------------------

    /**
     * Kill feed in the top right: one line per release, newest on top, each naming what it killed.
     * Weapon tagged so a mixed load reads at a glance, and right aligned so it never fights the
     * bomb panel in the other corner.
     */
    private static void drawKillFeed(HudContext context) {
        List<B2KillFeed.Display> lines = B2KillFeed.clientDisplays();
        if (lines.isEmpty()) {
            return;
        }
        GuiGraphics graphics = context.graphics;
        Font font = context.font;
        int y = 8;
        for (B2KillFeed.Display line : lines) {
            boolean incendiary = line.weapon() == B2Entity.WEAPON_INCENDIARY;
            String tag = incendiary ? "INC" : "JDAM";
            String text = line.text();
            int tagWidth = font.width(tag) + 4;
            int boxWidth = tagWidth + font.width(text) + 8;
            int x = Math.max(4, context.width - boxWidth - 6);
            int alpha = (int) (Math.max(0.0F, Math.min(1.0F, line.alpha())) * 255.0F) << 24;
            graphics.fill(x, y - 1, x + boxWidth, y + 10, alpha | (HudStyle.PANEL & 0xFFFFFF));
            graphics.drawString(font, tag, x + 4, y,
                    alpha | ((incendiary ? HudStyle.WARN : HudStyle.ACCENT) & 0xFFFFFF), false);
            graphics.drawString(font, text, x + 4 + tagWidth, y,
                    alpha | (HudStyle.TEXT & 0xFFFFFF), false);
            y += 12;
        }
    }

    /** FOV the game is actually rendering the B-2 with, so the marker lines up with the world. */
    private static double viewFov(HudContext context, B2Entity b2) {
        double base = context.minecraft.options.fov().get();
        double multiplier = b2.isCockpitView()
                ? LittleBirdConfig.cockpitFovMultiplier()
                : LittleBirdConfig.b2ThirdPersonFovMultiplier();
        return B2CockpitMath.cockpitFov(base, multiplier);
    }

    /** Boxed readout with a coloured top edge, like the rest of the pack's HUDs. */
    private static void readout(GuiGraphics graphics, Font font, int x, int y, String text,
                                int colour) {
        int width = readoutWidth(font, text);
        graphics.fill(x, y, x + width, y + 14, HudStyle.PANEL);
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.drawString(font, text, x + 4, y + 3, colour, false);
    }

    private static int readoutWidth(Font font, String text) {
        return font.width(text) + 8;
    }

    /** "IMPACT 120 m", or dashes when there is no prediction to show. */
    private static String distanceText(String label, double value) {
        return value < 0.0D ? label + " ---"
                : String.format(Locale.ROOT, "%s %.0f m", label, value);
    }

    /** Prediction error against the designation, dashes when nothing is known yet. */
    private static String missText() {
        return distanceText("MISS", B2PodView.impactMiss());
    }

    /** Four corner brackets of the pod's outer sight ring. */
    private static void cornerBrackets(GuiGraphics graphics, int cx, int cy, int reach, int inner,
                                       int colour) {
        graphics.fill(cx - reach, cy - reach, cx - inner, cy - reach + 1, colour);
        graphics.fill(cx + inner, cy - reach, cx + reach, cy - reach + 1, colour);
        graphics.fill(cx - reach, cy + reach, cx - inner, cy + reach + 1, colour);
        graphics.fill(cx + inner, cy + reach, cx + reach, cy + reach + 1, colour);
        graphics.fill(cx - reach, cy - reach, cx - reach + 1, cy - inner, colour);
        graphics.fill(cx + reach, cy - reach, cx + reach + 1, cy - inner, colour);
        graphics.fill(cx - reach, cy + inner, cx - reach + 1, cy + reach, colour);
        graphics.fill(cx + reach, cy + inner, cx + reach + 1, cy + reach, colour);
    }

    /** The bound key for a hint, so a rebound control keeps its label. */
    private static String key(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }
}
