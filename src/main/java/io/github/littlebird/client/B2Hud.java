package io.github.littlebird.client;

import io.github.littlebird.entity.B2Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * B-2 bomb sight: the rigid cockpit frame when the hard view is up, plus the bombing panel -
 * selected store, rounds left, bomb bay state and whether the pod has a designation.
 */
public final class B2Hud implements IGuiOverlay {
    private static final int FRAME_DARK = 0xC8060A0C;
    private static final int FRAME_EDGE = 0xFF1A2126;
    private static final int PANEL_BG = 0x99101418;
    private static final int PANEL_EDGE = 0xFF8AF0E8;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int WARN = 0xFFF2C94C;
    private static final int GOOD = 0xFF7FD46A;
    /** Row height and padding come from the tested layout rules. */
    private static final int ROW = B2HudLayout.ROW;
    private static final int PAD = B2HudLayout.PAD;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof B2Entity b2)) {
            return;
        }
        // The feed belongs to the crew, not to one view: it stays up in the pod and in the
        // cockpit alike.
        drawKillFeed(graphics, minecraft.font, screenWidth);
        if (B2PodView.isActive()) {
            return;
        }
        if (b2.isCockpitView()) {
            cockpitFrame(graphics, screenWidth, screenHeight);
        }
        Font font = minecraft.font;
        GunshipHudDraw.centreCluster(graphics, font, screenWidth, screenHeight,
                b2.getYRot(), b2.getXRot(), b2.interpolatedRenderRollDeg(partialTick),
                Math.sqrt(b2.getDeltaMovement().x * b2.getDeltaMovement().x
                        + b2.getDeltaMovement().z * b2.getDeltaMovement().z),
                b2.getY());
        bombSight(graphics, font, b2, screenWidth, screenHeight);
        panel(graphics, font, b2, screenWidth, screenHeight);
    }

    /** FOV the game is actually rendering the B-2 with, so the marker lines up with the world. */
    private static double viewFov(Minecraft minecraft, B2Entity b2) {
        double base = minecraft.options.fov().get();
        return b2.isCockpitView()
                ? B2CockpitMath.cockpitFov(base, io.github.littlebird.LittleBirdConfig
                        .cockpitFovMultiplier())
                : B2CockpitMath.cockpitFov(base, io.github.littlebird.LittleBirdConfig
                        .b2ThirdPersonFovMultiplier());
    }

    /**
     * The bombing sight: a pipper on the nose axis plus the spot a stick released right now would
     * land on, joined by a drop line. The crew aims the pipper and watches the marker walk in.
     */
    private static void bombSight(GuiGraphics graphics, Font font, B2Entity b2,
                                  int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        graphics.fill(cx - 14, cy, cx - 5, cy + 1, PANEL_EDGE);
        graphics.fill(cx + 5, cy, cx + 14, cy + 1, PANEL_EDGE);
        graphics.fill(cx, cy - 14, cx + 1, cy - 5, PANEL_EDGE);
        graphics.fill(cx, cy + 5, cx + 1, cy + 14, PANEL_EDGE);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, PANEL_EDGE);

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
        double fov = viewFov(Minecraft.getInstance(), b2);
        boolean ready = B2PodView.impactReady();
        int colour = ready ? GOOD : WARN;
        if (B2PodMath.offScreen(offset[0], offset[1], fov)) {
            graphics.drawString(font, String.format("IMPACT %.0f m OFF SIGHT", B2PodView.impactMiss()),
                    cx + 16, cy + 8, colour, true);
            return;
        }
        int[] marker = B2PodMath.projectToScreen(offset[0], offset[1], fov, screenWidth, screenHeight);
        // Dotted drop line from the nose axis down to the predicted impact.
        for (int i = 1; i <= 8; i++) {
            int lx = cx + (marker[0] - cx) * i / 9;
            int ly = cy + (marker[1] - cy) * i / 9;
            graphics.fill(lx, ly, lx + 1, ly + 1, ready ? GOOD : PANEL_EDGE);
        }
        graphics.fill(marker[0] - 7, marker[1] - 1, marker[0] + 8, marker[1] + 1, colour);
        graphics.fill(marker[0] - 1, marker[1] - 7, marker[0] + 1, marker[1] + 8, colour);
        graphics.fill(marker[0] - 5, marker[1] - 5, marker[0] + 6, marker[1] - 4, colour);
        graphics.fill(marker[0] - 5, marker[1] + 5, marker[0] + 6, marker[1] + 6, colour);

        String text = B2PodView.designation() == null
                ? String.format("IMPACT  %.0f m", B2PodView.impactDistance())
                : String.format("IMPACT  %.0f m   MISS %.0f m", B2PodView.impactDistance(),
                        B2PodView.impactMiss());
        graphics.drawString(font, text, marker[0] + 10, marker[1] + 6, colour, true);
        if (ready) {
            graphics.drawString(font, "RELEASE", marker[0] + 10, marker[1] + 18, GOOD, true);
        }
    }

    /** The rigid cockpit: two windscreen panes split by a centre spine, glare shield below. */
    private static void cockpitFrame(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int[] win = B2CockpitMath.windscreenRect(screenWidth, screenHeight);
        graphics.fill(0, 0, screenWidth, win[1], FRAME_DARK);
        graphics.fill(0, win[3], screenWidth, screenHeight, FRAME_DARK);
        graphics.fill(0, win[1], win[0], win[3], FRAME_DARK);
        graphics.fill(win[2], win[1], screenWidth, win[3], FRAME_DARK);
        int[] spine = B2CockpitMath.spineRect(screenWidth, screenHeight);
        graphics.fill(spine[0], spine[1], spine[2], spine[3], FRAME_DARK);
        int[] glare = B2CockpitMath.glareShieldRect(screenWidth, screenHeight);
        graphics.fill(glare[0], glare[1], glare[2], glare[3], FRAME_DARK);
        graphics.fill(win[0], win[1], win[2], win[1] + 2, FRAME_EDGE);
        graphics.fill(win[0], win[3] - 2, win[2], win[3], FRAME_EDGE);
        graphics.fill(glare[0], glare[1], glare[2], glare[1] + 2, FRAME_EDGE);
    }

    private static void panel(GuiGraphics graphics, Font font, B2Entity b2,
                              int screenWidth, int screenHeight) {
        int weapon = b2.getBombWeapon();
        int rounds = b2.bombCount(weapon);
        int capacity = Math.max(1, b2.bombCapacity(weapon));
        String store = weapon == B2Entity.WEAPON_INCENDIARY ? "INCENDIARY" : "JDAM";
        String count = rounds + " / " + capacity;
        String bay = b2.bombBayOpen() ? "BAY OPEN" : "BAY CLOSED";
        String pod = b2.hasPodDesignation() ? "TARGET SET" : "NO TARGET";
        boolean ready = b2.hasPodDesignation() && B2PodView.impactReady();
        String cue = cueText(b2, ready);
        // Key hints only when there is room for them: on a big GUI scale the logical screen is
        // small, and cramming them in is exactly what made the corner unreadable.
        boolean hints = B2HudLayout.hintsFit(screenWidth, screenHeight);
        String hint1 = hints ? "[Z] DROP   [SHIFT+Z] SALVO" : null;
        String hint2 = hints ? "[R] AUTO  [G] STORE  [P] POD  [C] VIEW" : null;

        int contentWidth = font.width("BOMB BAY");
        for (String text : new String[]{store, count, bay, pod, cue, hint1, hint2}) {
            if (text != null) {
                contentWidth = Math.max(contentWidth, font.width(text));
            }
        }
        // All the corner rules live in B2HudLayout, which is unit tested: measured width, inside
        // the screen, and lifted clear of the hotbar when the two would share the bottom row.
        B2HudLayout.Panel box = B2HudLayout.panelFor(screenWidth, screenHeight, contentWidth, hints);
        int x = box.x();
        int y = box.y();
        int width = box.width();
        int height = box.height();

        graphics.fill(x, y, x + width, y + height, PANEL_BG);
        graphics.fill(x, y, x + width, y + 1, PANEL_EDGE);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_EDGE);

        int cursor = y + PAD;
        graphics.drawString(font, "BOMB BAY", x + PAD, cursor, PANEL_EDGE, true);
        cursor += ROW;
        graphics.drawString(font, store, x + PAD, cursor, TEXT, true);
        int countColour = rounds > 0 ? GOOD : WARN;
        graphics.drawString(font, count, x + width - PAD - font.width(count), cursor, countColour,
                true);
        cursor += ROW;
        int barWidth = width - PAD * 2;
        graphics.fill(x + PAD, cursor, x + PAD + barWidth, cursor + 4, 0xC0000000);
        int filled = (int) (barWidth * (rounds / (float) capacity));
        if (filled > 0) {
            graphics.fill(x + PAD, cursor, x + PAD + filled, cursor + 4, countColour);
        }
        cursor += 10;
        graphics.drawString(font, bay, x + PAD, cursor, b2.bombBayOpen() ? WARN : TEXT, true);
        cursor += ROW;
        graphics.drawString(font, pod, x + PAD, cursor, b2.hasPodDesignation() ? GOOD : WARN, true);
        cursor += ROW;
        graphics.drawString(font, cue, x + PAD, cursor, ready ? GOOD : WARN, true);
        cursor += ROW;
        if (hint1 != null) {
            graphics.drawString(font, hint1, x + PAD, cursor, PANEL_EDGE, true);
            graphics.drawString(font, hint2, x + PAD, cursor + ROW, PANEL_EDGE, true);
        }
    }

    /**
     * Kill feed in the top right: one line per release, newest on top, each naming what it killed.
     * Weapon tagged so a mixed load reads at a glance, and right aligned so it never fights the
     * bomb panel in the other corner.
     */
    public static void drawKillFeed(GuiGraphics graphics, Font font, int screenWidth) {
        java.util.List<io.github.littlebird.client.B2KillFeed.Display> lines =
                io.github.littlebird.client.B2KillFeed.clientDisplays();
        if (lines.isEmpty()) {
            return;
        }
        int y = 8;
        for (io.github.littlebird.client.B2KillFeed.Display line : lines) {
            String tag = line.weapon() == B2Entity.WEAPON_INCENDIARY ? "INC" : "JDAM";
            String text = line.text();
            int tagWidth = font.width(tag) + 4;
            int width = tagWidth + font.width(text) + 8;
            int x = Math.max(4, screenWidth - width - 6);
            int alpha = (int) (Math.max(0.0F, Math.min(1.0F, line.alpha())) * 255.0F) << 24;
            graphics.fill(x, y - 1, x + width, y + 10, alpha | 0x00101418);
            graphics.drawString(font, tag, x + 4, y, alpha | (line.weapon()
                    == B2Entity.WEAPON_INCENDIARY ? 0x00FF9A4A : 0x00FFC24A), false);
            graphics.drawString(font, text, x + 4 + tagWidth, y, alpha | 0x00E6E6E6, false);
            y += 12;
        }
    }

    /** One line telling the crew what the release key will do next. */
    private static String cueText(B2Entity b2, boolean ready) {
        if (!b2.hasPodDesignation()) {
            return "[Z] DROP  (AUTO-DESIGNATES)";
        }
        if (ready) {
            return "[Z] DROP NOW  (IN THE ZONE)";
        }
        if (B2PodView.isAutoArmed()) {
            return "AUTO ARMED - FIRES AT THE LINE";
        }
        return "[R] ARM AUTO RELEASE";
    }
}
