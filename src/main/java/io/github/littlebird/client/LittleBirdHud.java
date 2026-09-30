package io.github.littlebird.client;

import com.mojang.math.Axis;
import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.weapon.LittleBirdGunCycle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Pilot HUD: a Battlefield-style cyan flight cluster in the middle (heading, speed,
 * altitude, pitch ladder, bank scale) and a weapon panel in the bottom right that
 * names the selected weapon, shows its slots and its load state.
 *
 * Reads only synched entity state; it never drives game logic.
 */
public final class LittleBirdHud implements IGuiOverlay {
    private static final int CYAN = 0xFF8AF0E8;
    private static final int CYAN_DIM = 0x808AF0E8;
    private static final int BOX_BACKGROUND = 0x99101418;
    private static final int YELLOW = 0xFFF2C94C;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int NOMINAL = 0xFF7FD46A;
    private static final int WARNING = 0xFFF2C94C;
    private static final int DANGER = 0xFFEB5757;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int HIT_MARKER_COLOR = 0xFFFF5A5A;
    private static final int GUN_RETICLE_COLOR = 0xFFFF6A4A;
    private static final int LADDER_CENTRE_GAP_PIXELS = 16;

    private static final int SLOT_WIDTH = 42;
    private static final int SLOT_HEIGHT = 20;

    public LittleBirdHud() {
    }

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui,
                       GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof LittleBirdEntity helicopter)) {
            return;
        }
        Font font = minecraft.font;
        // The pilot gets the full instrument panel; a passenger keeps the flight cluster and
        // nothing else, because the weapons and the reticles are not theirs to use.
        boolean pilot = helicopter.isPilot(minecraft.player);
        if (pilot && LittleBirdConfig.showCenterCrosshair()) {
            GunshipHudDraw.crosshair(graphics, screenWidth / 2, screenHeight / 2);
        }
        GunshipHudDraw.hitMarker(graphics, screenWidth / 2, screenHeight / 2);
        drawCentreCluster(graphics, font, helicopter, screenWidth, screenHeight);
        if (pilot) {
            drawWeaponPanel(graphics, font, helicopter, screenWidth, screenHeight);
            drawImpactReticles(graphics, helicopter);
        }
        GunshipHudDraw.enemyHealthBar(graphics, font, screenWidth, screenHeight);
        drawAlerts(graphics, font, helicopter, screenWidth, screenHeight);
    }

    // ------------------------------------------------------------------
    // Centre flight cluster (cyan, Battlefield style)
    // ------------------------------------------------------------------

    /** Feeds the shared cluster the Little Bird's own numbers. */
    private static void drawCentreCluster(GuiGraphics graphics, Font font,
                                          LittleBirdEntity helicopter,
                                          int width, int height) {
        var motion = helicopter.getDeltaMovement();
        GunshipHudDraw.centreCluster(graphics, font, width, height,
                helicopter.getYRot(), helicopter.getXRot(), helicopter.getRenderRollDeg(),
                Math.sqrt(motion.x * motion.x + motion.z * motion.z), helicopter.getY());
    }

    // ------------------------------------------------------------------
    // Weapon panel (bottom right)
    // ------------------------------------------------------------------

    private static void drawWeaponPanel(GuiGraphics graphics, Font font,
                                        LittleBirdEntity helicopter,
                                        int width, int height) {
        int panelWidth = 210;
        int panelHeight = 86;
        int x = width - panelWidth - 6;
        int y = height - panelHeight - 6;
        graphics.fill(x, y, x + panelWidth, y + panelHeight, BOX_BACKGROUND);
        graphics.fill(x, y, x + panelWidth, y + 1, CYAN);

        boolean gunSelected = helicopter.getWeapon() == LittleBirdEntity.WEAPON_GUN;
        Component name = Component.translatable(gunSelected
                ? "hud.battlefield.weapon.gun" : "hud.battlefield.weapon.rockets");
        graphics.drawString(font, name, x + 4, y + 4, CYAN, false);
        String ammo = gunSelected
                ? Integer.toString(helicopter.getGunAmmo())
                : helicopter.getRocketsRemaining() + "/" + helicopter.getRocketReserve();
        graphics.drawString(font, ammo, x + panelWidth - 6 - font.width(ammo), y + 4, TEXT, false);

        int barWidth = panelWidth - 8;
        graphics.fill(x + 4, y + 16, x + 4 + barWidth, y + 20, 0x60000000);
        int filled;
        int barColour;
        if (gunSelected) {
            double heatFraction = helicopter.getGunHeat() / (double) LittleBirdGunCycle.MAX_HEAT;
            barColour = heatFraction >= 0.9D ? DANGER
                    : heatFraction >= 0.6D ? WARNING : NOMINAL;
            filled = (int) Math.round(barWidth * Math.min(1.0D, heatFraction));
        } else if (helicopter.getRocketReloadTicks() > 0) {
            // The rocket line turns into the reload cooldown while the reserve is moving up.
            barColour = YELLOW;
            filled = (int) Math.round(barWidth * helicopter.getRocketReloadFraction());
            graphics.drawString(font, Component.translatable("hud.battlefield.reloading"),
                    x + 4 + font.width(name) + 6, y + 4, YELLOW, false);
        } else {
            double readyFraction = helicopter.getRocketsRemaining()
                    / (double) io.github.littlebird.entity.weapon.RocketPodState.READY_CAPACITY;
            barColour = NOMINAL;
            filled = (int) Math.round(barWidth * Math.min(1.0D, Math.max(0.0D, readyFraction)));
        }
        if (filled > 0) {
            graphics.fill(x + 4, y + 16, x + 4 + filled, y + 20, barColour);
        }

        int slotY = y + 26;
        drawSlot(graphics, font, x + 4, slotY, "1", gunSelected, true,
                Component.translatable("hud.battlefield.weapon.gun").getString());
        drawSlot(graphics, font, x + 4 + SLOT_WIDTH + 4, slotY, "2", !gunSelected, false,
                Integer.toString(helicopter.getRocketsRemaining()));
        drawSlot(graphics, font, x + 4 + (SLOT_WIDTH + 4) * 2, slotY, "Z",
                helicopter.isSalvoMode(), false,
                Component.translatable(helicopter.isSalvoMode()
                        ? "hud.battlefield.mode.salvo" : "hud.battlefield.mode.single").getString());

        int rpm = Math.round(helicopter.getRenderRotorRpm() * 100.0F);
        String status = Component.translatable("hud.battlefield.rpm", rpm).getString();
        graphics.drawString(font, status, x + 4, slotY + 24, rpm < 15 ? WARNING : TEXT, false);
        boolean lowHull = LittleBirdHudLayout.lowHealth(
                helicopter.getHealth(), LittleBirdEntity.MAX_HEALTH);
        String hull = Component.translatable("hud.battlefield.health",
                Math.round(helicopter.getHealth())).getString();
        graphics.drawString(font, hull, x + panelWidth - 6 - font.width(hull),
                slotY + 24, lowHull ? DANGER : TEXT, false);
    }

    /** One weapon slot: key label on the left, state text on the right. */
    private static void drawSlot(GuiGraphics graphics, Font font, int x, int y,
                                 String key, boolean selected, boolean gunIcon,
                                 String state) {
        int background = selected ? CYAN : 0x60000000;
        graphics.fill(x, y, x + SLOT_WIDTH, y + SLOT_HEIGHT, background);
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
    // Impact reticles
    // ------------------------------------------------------------------

    /**
     * Predicted impact reticles, projected onto the screen. Which one is shown
     * follows the selected weapon, and the gun crosshair opens up with dispersion.
     */
    private static void drawImpactReticles(GuiGraphics graphics, LittleBirdEntity helicopter) {
        boolean follows = LittleBirdConfig.reticleFollowsWeapon();
        int weapon = helicopter.getWeapon();
        if (LittleBirdCrosshairMath.showsGunReticle(follows, weapon,
                LittleBirdConfig.showGunReticle())) {
            drawGunCrosshair(graphics, LittleBirdAimRenderer.gunReticle());
        }
        if (LittleBirdCrosshairMath.showsRocketReticle(follows, weapon,
                LittleBirdConfig.showRocketReticle())) {
            drawRocketReticle(graphics, LittleBirdAimRenderer.rocketReticle(),
                    helicopter.getRocketsLeft(), helicopter.getRocketsRight());
        }
    }

    /** Four arms with the gap driven by the current spread. No centre dot. */
    private static void drawGunCrosshair(
            GuiGraphics graphics, LittleBirdAimRenderer.GunReticle reticle) {
        if (!reticle.visible()) {
            return;
        }
        int x = (int) Math.round(reticle.x());
        int y = (int) Math.round(reticle.y());
        int gap = (int) Math.round(reticle.gapPixels());
        int arm = LittleBirdCrosshairMath.ARM_LENGTH_PIXELS;
        graphics.fill(x - gap - arm, y, x - gap, y + 1, GUN_RETICLE_COLOR);
        graphics.fill(x + gap + 1, y, x + gap + arm + 1, y + 1, GUN_RETICLE_COLOR);
        graphics.fill(x, y - gap - arm, x + 1, y - gap, GUN_RETICLE_COLOR);
        graphics.fill(x, y + gap + 1, x + 1, y + gap + arm + 1, GUN_RETICLE_COLOR);
    }

    /**
     * Reference rocket reticle. The drawing itself lives in GunshipHudDraw so both gunships
     * aim with the same sight; this only applies the projected impact point.
     */
    private static void drawRocketReticle(GuiGraphics graphics,
            LittleBirdAimRenderer.Reticle reticle, int leftRounds, int rightRounds) {
        if (!reticle.visible()) {
            return;
        }
        GunshipHudDraw.rocketReticle(graphics, (int) Math.round(reticle.x()),
                (int) Math.round(reticle.y()), leftRounds, rightRounds);
    }

    // ------------------------------------------------------------------
    // Alerts, crosshair and hit marker
    // ------------------------------------------------------------------

    private static void drawAlerts(GuiGraphics graphics, Font font,
                                   LittleBirdEntity helicopter,
                                   int width, int height) {
        Component alert = currentAlert(helicopter);
        GunshipHudDraw.alerts(graphics, font,
                alert == null ? java.util.List.of() : java.util.List.of(alert),
                width, height);
    }

    private static Component currentAlert(LittleBirdEntity helicopter) {
        if (helicopter.getGunState() == LittleBirdGunCycle.OVERHEATED) {
            return Component.translatable("alert.battlefield.overheat");
        }
        if (helicopter.getRenderRotorRpm() < 0.15F) {
            return Component.translatable("alert.battlefield.engine_off");
        }
        if (helicopter.getRocketsRemaining() <= 0) {
            return Component.translatable("alert.battlefield.no_rockets");
        }
        if (LittleBirdHudLayout.lowHealth(helicopter.getHealth(), LittleBirdEntity.MAX_HEALTH)) {
            return Component.translatable("alert.battlefield.low_health");
        }
        return null;
    }

}