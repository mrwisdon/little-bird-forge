package io.github.littlebird.client;

import io.github.littlebird.entity.JetEntity;
import io.github.littlebird.entity.weapon.JetCannon;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * F-35 HUD: the shared cyan centre cluster plus a weapons panel - selected store, its ammo, the
 * five slots, cannon heat, throttle and afterburner - and a lock indicator that reads the synced
 * seeker progress. All numbers come from synched entity data, so the panel is identical for the
 * pilot and for anyone watching the airframe.
 */
public final class JetHud implements IGuiOverlay {
    private static final int PANEL_WIDTH = 230;
    private static final int PANEL_HEIGHT = 108;
    private static final int BAR_HEIGHT = 5;
    private static final int SLOT_GAP = 2;
    private static final int BAR_LABEL_WIDTH = 34;

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui,
                       GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof JetEntity jet)
                || jet instanceof io.github.littlebird.entity.B2Entity) {
            // The B-2 flies the bomber stack (B2Hud / B2PodOverlay); the fighter panels are not
            // shown in it at all, which is what the crew asked for.
            return;
        }
        Font font = minecraft.font;
        GunshipHudDraw.crosshair(graphics, screenWidth / 2, screenHeight / 2);
        GunshipHudDraw.hitMarker(graphics, screenWidth / 2, screenHeight / 2);
        var motion = jet.getDeltaMovement();
        GunshipHudDraw.centreCluster(graphics, font, screenWidth, screenHeight,
                jet.getYRot(), jet.getXRot(), jet.getRenderRollDeg(),
                Math.sqrt(motion.x * motion.x + motion.z * motion.z), jet.getY());
        drawLock(graphics, font, jet, screenWidth, screenHeight);
        drawPanel(graphics, font, jet, screenWidth, screenHeight);
        GunshipHudDraw.enemyHealthBar(graphics, font, screenWidth, screenHeight);
        GunshipHudDraw.alerts(graphics, font, alerts(jet), screenWidth, screenHeight);
    }

    /** A bracket at the centre when the seeker is locked, a filling bar while it is not. */
    private static void drawLock(GuiGraphics graphics, Font font, JetEntity jet,
                                 int width, int height) {
        int weapon = jet.getWeapon();
        boolean seeker = weapon == JetEntity.WEAPON_IR_MISSILE
                || weapon == JetEntity.WEAPON_AGM
                || weapon == JetEntity.WEAPON_RADAR_MISSILE;
        if (!seeker) {
            return;
        }
        int cx = width / 2;
        int cy = height / 2;
        if (jet.isLocked()) {
            int r = 14;
            int len = 6;
            int colour = GunshipHudDraw.DANGER;
            graphics.fill(cx - r, cy - r, cx - r + len, cy - r + 1, colour);
            graphics.fill(cx - r, cy - r, cx - r + 1, cy - r + len, colour);
            graphics.fill(cx + r - len, cy - r, cx + r, cy - r + 1, colour);
            graphics.fill(cx + r - 1, cy - r, cx + r, cy - r + len, colour);
            graphics.fill(cx - r, cy + r - 1, cx - r + len, cy + r, colour);
            graphics.fill(cx - r, cy + r - len, cx - r + 1, cy + r, colour);
            graphics.fill(cx + r - len, cy + r - 1, cx + r, cy + r, colour);
            graphics.fill(cx + r - 1, cy + r - len, cx + r, cy + r, colour);
            drawCentred(graphics, font,
                    Component.translatable("hud.battlefield.locked").getString(),
                    cx, cy + r + 4, colour);
            return;
        }
        int acquire = JetEntity.lockAcquireTicksFor(weapon);
        float progress = acquire <= 0 ? 0.0F : jet.getLockProgress() / (float) acquire;
        if (jet.getLockTargetId() < 0 && progress <= 0.0F) {
            return;
        }
        drawCentred(graphics, font,
                Component.translatable("hud.battlefield.locking").getString(),
                cx, cy + 24, GunshipHudDraw.WARNING);
        GunshipHudDraw.bar(graphics, cx - 30, cy + 36, 60, 3, progress, GunshipHudDraw.WARNING);
    }

    private static void drawCentred(GuiGraphics graphics, Font font, String text,
                                    int cx, int y, int colour) {
        graphics.drawString(font, text, cx - font.width(text) / 2, y, colour, false);
    }

    private static void drawPanel(GuiGraphics graphics, Font font, JetEntity jet,
                                  int width, int height) {
        int x = width - PANEL_WIDTH - 6;
        int y = height - PANEL_HEIGHT - 6;
        GunshipHudDraw.panel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, GunshipHudDraw.CYAN);

        int weapon = jet.getWeapon();
        graphics.drawString(font, Component.translatable(weaponNameKey(weapon)),
                x + 4, y + 4, GunshipHudDraw.CYAN, false);
        String ammo = ammoText(jet, weapon);
        graphics.drawString(font, ammo, x + PANEL_WIDTH - 6 - font.width(ammo), y + 4,
                GunshipHudDraw.TEXT, false);

        int slotY = y + 20;
        int sx = x + 4;
        GunshipHudDraw.slot(graphics, font, sx, slotY, "1", Integer.toString(jet.getCannonAmmo()),
                weapon == JetEntity.WEAPON_CANNON, false);
        sx += GunshipHudDraw.SLOT_WIDTH + SLOT_GAP;
        GunshipHudDraw.slot(graphics, font, sx, slotY, "2", Integer.toString(jet.getRocketReady()),
                weapon == JetEntity.WEAPON_ROCKETS, false);
        sx += GunshipHudDraw.SLOT_WIDTH + SLOT_GAP;
        String missiles = Integer.toString(jet.getMissileAmmo());
        GunshipHudDraw.slot(graphics, font, sx, slotY, "3", missiles,
                weapon == JetEntity.WEAPON_IR_MISSILE, false);
        sx += GunshipHudDraw.SLOT_WIDTH + SLOT_GAP;
        GunshipHudDraw.slot(graphics, font, sx, slotY, "4", missiles,
                weapon == JetEntity.WEAPON_AGM, false);
        sx += GunshipHudDraw.SLOT_WIDTH + SLOT_GAP;
        GunshipHudDraw.slot(graphics, font, sx, slotY, "5", missiles,
                weapon == JetEntity.WEAPON_RADAR_MISSILE, false);

        int barY = slotY + GunshipHudDraw.SLOT_HEIGHT + 6;
        int barX = x + 4 + BAR_LABEL_WIDTH;
        int barWidth = PANEL_WIDTH - 8 - BAR_LABEL_WIDTH;
        float heat = jet.getCannonHeat() / (float) Math.max(1, jet.cannonMaxHeat());
        int heatColour = heat >= 0.9F ? GunshipHudDraw.DANGER
                : heat >= 0.6F ? GunshipHudDraw.WARNING : GunshipHudDraw.NOMINAL;
        drawBar(graphics, font, x + 4, barX, barY, barWidth,
                "hud.battlefield.heat", heat, heatColour);
        drawBar(graphics, font, x + 4, barX, barY + BAR_HEIGHT + 4, barWidth,
                "hud.battlefield.throttle", jet.getRenderThrottle(), GunshipHudDraw.CYAN);
        drawBar(graphics, font, x + 4, barX, barY + (BAR_HEIGHT + 4) * 2, barWidth,
                "hud.battlefield.afterburner", jet.getRenderAfterburnerCharge(),
                GunshipHudDraw.YELLOW);

        int statusY = y + PANEL_HEIGHT - 14;
        String flares = Component.translatable("hud.battlefield.flares", jet.getFlares()).getString();
        graphics.drawString(font, flares, x + 4, statusY,
                jet.getFlares() > 0 ? GunshipHudDraw.TEXT : GunshipHudDraw.DANGER, false);
        String hull = Component.translatable("hud.battlefield.health",
                Math.round(jet.getHealth())).getString();
        graphics.drawString(font, hull, x + PANEL_WIDTH - 6 - font.width(hull), statusY,
                GunshipHudDraw.TEXT, false);
        if (jet.isVtol()) {
            String vtol = Component.translatable("hud.battlefield.vtol").getString();
            graphics.drawString(font, vtol, x + PANEL_WIDTH / 2 - font.width(vtol) / 2, statusY,
                    GunshipHudDraw.WARNING, false);
        }
    }

    private static void drawBar(GuiGraphics graphics, Font font, int labelX, int barX, int y,
                                int width, String labelKey, float fraction, int colour) {
        graphics.drawString(font, Component.translatable(labelKey), labelX, y - 3,
                GunshipHudDraw.CYAN_DIM, false);
        GunshipHudDraw.bar(graphics, barX, y, width, BAR_HEIGHT, fraction, colour);
    }

    private static String weaponNameKey(int weapon) {
        return switch (weapon) {
            case JetEntity.WEAPON_ROCKETS -> "hud.battlefield.weapon.jet_rockets";
            case JetEntity.WEAPON_IR_MISSILE -> "hud.battlefield.weapon.jet_ir";
            case JetEntity.WEAPON_AGM -> "hud.battlefield.weapon.jet_agm";
            case JetEntity.WEAPON_RADAR_MISSILE -> "hud.battlefield.weapon.jet_radar";
            default -> "hud.battlefield.weapon.jet_cannon";
        };
    }

    private static String ammoText(JetEntity jet, int weapon) {
        int rounds = switch (weapon) {
            case JetEntity.WEAPON_ROCKETS -> jet.getRocketReady();
            case JetEntity.WEAPON_IR_MISSILE, JetEntity.WEAPON_AGM,
                    JetEntity.WEAPON_RADAR_MISSILE -> jet.getMissileAmmo();
            default -> jet.getCannonAmmo();
        };
        return Component.translatable("hud.battlefield.ammo", rounds).getString();
    }

    private static List<Component> alerts(JetEntity jet) {
        if (jet.getCannonState() == JetCannon.OVERHEATED) {
            return List.of(Component.translatable("alert.battlefield.overheat"));
        }
        if (!jet.isEngineOn()) {
            return List.of(Component.translatable("alert.battlefield.engine_off"));
        }
        if (jet.getCannonAmmo() <= 0 && jet.getRocketReady() <= 0
                && jet.getMissileAmmo() <= 0) {
            return List.of(Component.translatable("alert.battlefield.no_ammo"));
        }
        if (LittleBirdHudLayout.lowHealth(jet.getHealth(), JetEntity.MAX_HEALTH)) {
            return List.of(Component.translatable("alert.battlefield.low_health"));
        }
        return List.of();
    }
}
