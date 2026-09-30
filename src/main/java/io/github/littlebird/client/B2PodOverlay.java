package io.github.littlebird.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Electro-optical pod overlay: the monochrome sight picture, scan lines, the gimbal reticle and
 * the readouts a bombing crew actually watches.
 */
public final class B2PodOverlay implements IGuiOverlay {
    private static final int MONO_TINT = 0x2A6FFF9C;
    private static final int SCAN_LINE = 0x20000000;
    private static final int VIGNETTE = 0x77000000;
    private static final int SIGHT = 0xFFD9FFE4;
    private static final int SIGHT_DIM = 0x99D9FFE4;
    private static final int LOCK_COLOR = 0xFF79FF9C;
    private static final int TEXT = 0xFFD9FFE4;

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick,
                       int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!B2PodView.isActive() || minecraft.options.hideGui) {
            return;
        }
        graphics.fill(0, 0, screenWidth, screenHeight, MONO_TINT);
        for (int y = 0; y < screenHeight; y += 4) {
            graphics.fill(0, y, screenWidth, y + 1, SCAN_LINE);
        }
        int band = Math.max(8, screenHeight / 12);
        graphics.fill(0, 0, screenWidth, band, VIGNETTE);
        graphics.fill(0, screenHeight - band, screenWidth, screenHeight, VIGNETTE);
        graphics.fill(0, 0, band, screenHeight, VIGNETTE);
        graphics.fill(screenWidth - band, 0, screenWidth, screenHeight, VIGNETTE);

        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
        reticle(graphics, cx, cy);
        double[] offset = B2PodMath.reticleOffset(
                new B2PodMath.Gimbal(B2PodView.gimbalYaw(), B2PodView.gimbalPitch()),
                new B2PodMath.Gimbal(B2PodView.gimbalYaw(), B2PodView.gimbalPitch()));
        boolean locked = offset[0] == 0.0D && offset[1] == 0.0D;
        if (locked) {
            graphics.fill(cx - 34, cy - 20, cx - 30, cy - 16, LOCK_COLOR);
            graphics.fill(cx + 30, cy - 20, cx + 34, cy - 16, LOCK_COLOR);
            graphics.fill(cx - 34, cy + 16, cx - 30, cy + 20, LOCK_COLOR);
            graphics.fill(cx + 30, cy + 16, cx + 34, cy + 20, LOCK_COLOR);
        }

        Font font = minecraft.font;
        int left = 12;
        int top = 14;
        graphics.drawString(font, "EO POD  B-2", left, top, TEXT, true);
        graphics.drawString(font, String.format("GIMBAL  Y %+05.1f  P %+05.1f",
                B2PodView.gimbalYaw(), B2PodView.gimbalPitch()), left, top + 12, SIGHT_DIM, true);
        double[] window = B2PodView.sightWindow();
        boolean atLimit = Math.abs(B2PodView.gimbalPitch() - window[1]) < 0.6D
                || Math.abs(B2PodView.gimbalPitch() + window[0]) < 0.6D;
        graphics.drawString(font, String.format("SIGHT   UP %.0f  DOWN %.0f%s", window[0], window[1],
                atLimit ? "  [AT LIMIT]" : ""), left, top + 36, atLimit ? LOCK_COLOR : SIGHT_DIM, true);
        graphics.drawString(font, String.format("ZOOM    FOV %.1f deg", B2PodView.zoomFov()),
                left, top + 48, SIGHT_DIM, true);
        double range = B2PodView.rangeToSight();
        graphics.drawString(font, range < 0.0D ? "RANGE   ---" : String.format("RANGE   %.0f m", range),
                left, top + 36, SIGHT_DIM, true);
        double[] designation = B2PodView.designation();
        if (designation == null) {
            graphics.drawString(font, "DESIGNATE  [LMB]", left, top + 72, TEXT, true);
        } else {
            graphics.drawString(font, String.format("TARGET  %.0f %.0f %.0f", designation[0],
                    designation[1], designation[2]), left, top + 72, LOCK_COLOR, true);
            graphics.drawString(font, "CLEAR  [RMB]", left, top + 84, SIGHT_DIM, true);
        }
        int[] marker = B2PodView.impactMarkerOnScreen(screenWidth, screenHeight);
        if (marker != null && B2PodView.designation() != null) {
            int colour = B2PodView.impactReady() ? LOCK_COLOR : SIGHT_DIM;
            graphics.fill(marker[0] - 6, marker[1] - 1, marker[0] + 7, marker[1] + 1, colour);
            graphics.fill(marker[0] - 1, marker[1] - 6, marker[0] + 1, marker[1] + 7, colour);
        }
        String release;
        if (B2PodView.designation() == null) {
            release = "DROP  [Z]  AUTO-DESIGNS";
        } else if (B2PodView.impactReady()) {
            release = String.format("DROP NOW  [Z]   LEAD %dt", B2PodView.releaseLeadTicks());
        } else if (B2PodView.isAutoArmed()) {
            release = String.format("AUTO ARMED  MISS %.0f m", B2PodView.impactMiss());
        } else {
            release = String.format("ARM AUTO  [R]  MISS %.0f m  LEAD %dt",
                    B2PodView.impactMiss(), B2PodView.releaseLeadTicks());
        }
        graphics.drawString(font, release, left, top + 120,
                B2PodView.designation() != null && B2PodView.impactReady() ? LOCK_COLOR : TEXT, true);
        graphics.drawString(font,
                B2PodView.levelWarning() ? "BOMB RUN  BANK - HOLD" : "BOMB RUN  LEVEL",
                left, top + 96, B2PodView.levelWarning() ? LOCK_COLOR : TEXT, true);
        graphics.drawString(font, "AIRCRAFT HOLDS LEVEL WHILE THE POD IS UP",
                left, top + 108, SIGHT_DIM, true);
        graphics.drawString(font, "EJECT POD  [P]", left, screenHeight - 24, SIGHT_DIM, true);
        // Always on top: the pod's monochrome mask would otherwise dim the crew's kill feed.
        B2Hud.drawKillFeed(graphics, font, screenWidth);
    }

    private static void reticle(GuiGraphics graphics, int cx, int cy) {
        graphics.fill(cx - 1, cy - 18, cx + 1, cy - 5, SIGHT);
        graphics.fill(cx - 1, cy + 5, cx + 1, cy + 18, SIGHT);
        graphics.fill(cx - 18, cy - 1, cx - 5, cy + 1, SIGHT);
        graphics.fill(cx + 5, cy - 1, cx + 18, cy + 1, SIGHT);
        graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, SIGHT);
        graphics.fill(cx - 60, cy - 60, cx - 20, cy - 59, SIGHT);
        graphics.fill(cx + 20, cy - 60, cx + 60, cy - 59, SIGHT);
        graphics.fill(cx - 60, cy + 59, cx - 20, cy + 60, SIGHT);
        graphics.fill(cx + 20, cy + 59, cx + 60, cy + 60, SIGHT);
        graphics.fill(cx - 60, cy - 60, cx - 59, cy - 20, SIGHT);
        graphics.fill(cx + 59, cy - 60, cx + 60, cy - 20, SIGHT);
        graphics.fill(cx - 60, cy + 20, cx - 59, cy + 60, SIGHT);
        graphics.fill(cx + 59, cy + 20, cx + 60, cy + 60, SIGHT);
    }
}
