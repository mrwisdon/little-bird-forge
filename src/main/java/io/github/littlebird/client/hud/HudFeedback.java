package io.github.littlebird.client.hud;

import io.github.littlebird.entity.HitReadout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Combat feedback shared by every HUD: the hit marker, the enemy airframe readout under the
 * crosshair and the state that feeds them. The network packet calls {@link #onHitFeedback}; the
 * views call {@link #hitMarker} and {@link #enemyBar}.
 */
public final class HudFeedback {
    private static final int HIT_MILLIS = 200;
    private static final int BAR_MILLIS = 3000;
    private static final int BAR_WIDTH = 64;
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_OFFSET = 16;

    private static long hitUntil;
    private static boolean hitKill;
    private static long barUntil;
    private static boolean barKill;
    private static String barName = "";
    private static float barHealth;
    private static float barMax;
    private static float barFraction;

    private HudFeedback() {
    }

    /** Called from the hit-feedback packet: starts the marker and the readout timers. */
    public static void onHitFeedback(boolean kill, String name, float health, float maxHealth) {
        hitUntil = System.currentTimeMillis() + HIT_MILLIS;
        hitKill = kill;
        barUntil = System.currentTimeMillis() + BAR_MILLIS;
        barKill = kill;
        barName = HitReadout.targetName(name);
        barHealth = health;
        barMax = maxHealth;
        barFraction = fraction(health, maxHealth, kill);
    }

    private static float fraction(float health, float maxHealth, boolean kill) {
        if (!Float.isFinite(health) || !Float.isFinite(maxHealth) || maxHealth <= 0.0F) {
            return kill ? 0.0F : 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, health / maxHealth));
    }

    /** Four short diagonal ticks around the centre for a sixth of a second. */
    public static void hitMarker(GuiGraphics g, int cx, int cy) {
        if (System.currentTimeMillis() > hitUntil) {
            return;
        }
        int colour = hitKill ? HudStyle.DANGER : HudStyle.WARN;
        int arm = hitKill ? 8 : 6;
        for (int i = 0; i < arm; i++) {
            g.fill(cx - 12 - i, cy - 12 - i, cx - 11 - i, cy - 11 - i, colour);
            g.fill(cx + 11 + i, cy - 12 - i, cx + 12 + i, cy - 11 - i, colour);
            g.fill(cx - 12 - i, cy + 11 + i, cx - 11 - i, cy + 12 + i, colour);
            g.fill(cx + 11 + i, cy + 11 + i, cx + 12 + i, cy + 12 + i, colour);
        }
    }

    /** Enemy airframe readout: a bar under the crosshair plus the target name and health. */
    public static void enemyBar(GuiGraphics g, Font font, int width, int height) {
        if (System.currentTimeMillis() > barUntil) {
            return;
        }
        int x = width / 2 - BAR_WIDTH / 2;
        int y = height / 2 + BAR_OFFSET;
        g.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, HudStyle.DANGER);
        g.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x66000000);
        int filled = (int) Math.round(BAR_WIDTH * barFraction);
        if (filled > 0) {
            g.fill(x, y, x + filled, y + BAR_HEIGHT, HudStyle.DANGER);
        }
        String health = HitReadout.healthText(barHealth, barMax, barKill);
        String label = barName.isEmpty() ? health
                : (health.isEmpty() ? barName : barName + "  " + health);
        if (!label.isEmpty()) {
            g.drawString(font, label, width / 2 - font.width(label) / 2, y + BAR_HEIGHT + 3,
                    HudStyle.DANGER, false);
        }
    }
}
