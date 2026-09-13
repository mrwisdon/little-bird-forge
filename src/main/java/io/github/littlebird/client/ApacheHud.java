package io.github.littlebird.client;

import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.seat.ApacheSeat;
import io.github.littlebird.entity.weapon.ChainGunTurret;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Apache HUD: the same cyan centre cluster as the Little Bird (drawn by GunshipHudDraw), a
 * panel that follows the seat - the rear seat sees the magazines, the front seat sees the
 * chain gun and its heat - and, for the gunner, a reticle on the turret's own aim.
 */
public final class ApacheHud implements IGuiOverlay {
    private static final int PANEL_WIDTH = 210;
    private static final int PANEL_HEIGHT = 104;
    private static final int RELOAD_BAR_HEIGHT = 5;
    /** One name+transfer+resupply triple, plus the gap before the next magazine. */
    private static final int MAGAZINE_ROW_HEIGHT = RELOAD_BAR_HEIGHT + 2 + 3;

    @Override
    public void render(net.minecraftforge.client.gui.overlay.ForgeGui gui,
                       GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        if (!(minecraft.player.getVehicle() instanceof ApacheEntity apache)) {
            return;
        }
        Font font = minecraft.font;
        boolean pilot = apache.seatOf(minecraft.player) == ApacheSeat.PILOT;
        // The rocket and TOW sight line is the nose, and the nose is the middle of the screen,
        // so the crew always get a fixed centre cross - the Apache has no projected impact
        // reticle to fall back on the way the Little Bird does. The gunner aims with the
        // turret, so the projected reticle below is theirs alone.
        GunshipHudDraw.crosshair(graphics, screenWidth / 2, screenHeight / 2);
        GunshipHudDraw.hitMarker(graphics, screenWidth / 2, screenHeight / 2);
        var motion = apache.getDeltaMovement();
        GunshipHudDraw.centreCluster(graphics, font, screenWidth, screenHeight,
                apache.getYRot(), apache.getXRot(), apache.getRenderRollDeg(),
                Math.sqrt(motion.x * motion.x + motion.z * motion.z), apache.getY());
        if (pilot) {
            drawRocketSight(graphics, apache);
        } else {
            drawTurretReticle(graphics, apache);
        }
        if (pilot) {
            drawPilotPanel(graphics, font, apache, screenWidth, screenHeight);
        } else {
            drawGunnerPanel(graphics, font, apache, screenWidth, screenHeight);
        }
        GunshipHudDraw.enemyHealthBar(graphics, font, screenWidth, screenHeight);
        GunshipHudDraw.alerts(graphics, font, alerts(apache, pilot), screenWidth, screenHeight);
    }

    /**
     * The pilot's rocket sight: the reference marker sitting on the predicted 70mm impact.
     * The TOW needs none of this - it flies down the nose, which is already the centre cross -
     * so the marker is shown only while the rocket bay is the selected weapon.
     */
    private static void drawRocketSight(GuiGraphics graphics, ApacheEntity apache) {
        if (apache.getWeapon() != ApacheEntity.WEAPON_ROCKETS) {
            return;
        }
        ApacheAimRenderer.Reticle reticle = ApacheAimRenderer.rocketReticle();
        if (!reticle.visible()) {
            return;
        }
        GunshipHudDraw.apacheRocketReticle(graphics, (int) Math.round(reticle.x()),
                (int) Math.round(reticle.y()), apache.getReadyRockets());
    }

    /** The gunner's reticle: four arms on the barrel's projected aim, amber when pegged. */
    private static void drawTurretReticle(GuiGraphics graphics, ApacheEntity apache) {
        ApacheAimRenderer.Reticle reticle = ApacheAimRenderer.reticle();
        if (!reticle.visible()) {
            return;
        }
        int x = (int) Math.round(reticle.x());
        int y = (int) Math.round(reticle.y());
        int colour = reticle.pegged() ? GunshipHudDraw.WARNING : GunshipHudDraw.CYAN;
        int gap = 4;
        int arm = 8;
        graphics.fill(x - gap - arm, y, x - gap, y + 1, colour);
        graphics.fill(x + gap + 1, y, x + gap + arm + 1, y + 1, colour);
        graphics.fill(x, y - gap - arm, x + 1, y - gap, colour);
        graphics.fill(x, y + gap + 1, x + 1, y + gap + arm + 1, colour);
        if (reticle.pegged()) {
            graphics.fill(x - 1, y - 1, x + 2, y + 2, colour);
        }
    }

    /** Rear seat: which magazine is selected, how much is ready, and both reload timers. */
    private static void drawPilotPanel(GuiGraphics graphics, Font font, ApacheEntity apache,
                                       int width, int height) {
        int x = width - PANEL_WIDTH - 6;
        int y = height - PANEL_HEIGHT - 6;
        GunshipHudDraw.panel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, GunshipHudDraw.CYAN);
        boolean rockets = apache.getWeapon() == ApacheEntity.WEAPON_ROCKETS;
        Component name = Component.translatable(rockets
                ? "hud.battlefield.weapon.apache_rockets"
                : "hud.battlefield.weapon.apache_missiles");
        graphics.drawString(font, name, x + 4, y + 4, GunshipHudDraw.CYAN, false);
        String ammo = rockets
                ? apache.getReadyRockets() + "/" + apache.getReserveRockets()
                : apache.getReadyMissiles() + "/" + apache.getReserveMissiles();
        graphics.drawString(font, ammo, x + PANEL_WIDTH - 6 - font.width(ammo), y + 4,
                GunshipHudDraw.TEXT, false);

        int slotY = y + 20;
        GunshipHudDraw.slot(graphics, font, x + 4, slotY, "1",
                Integer.toString(apache.getReadyRockets()), rockets, false);
        GunshipHudDraw.slot(graphics, font, x + 4 + GunshipHudDraw.SLOT_WIDTH + 4, slotY, "2",
                Integer.toString(apache.getReadyMissiles()), !rockets, false);
        GunshipHudDraw.slot(graphics, font, x + 4 + (GunshipHudDraw.SLOT_WIDTH + 4) * 2, slotY,
                "R", Component.translatable("hud.battlefield.reloading").getString(), false, false);

        // Two rows per magazine: the upper bar is the reserve-to-ready transfer (or, when
        // nothing is transferring, how full the ready position is), the thin lower one is the
        // automatic reserve resupply. Both are driven by the synced timers.
        int rowY = slotY + GunshipHudDraw.SLOT_HEIGHT + 6;
        drawMagazineRow(graphics, font, x, rowY, "hud.battlefield.magazine.rockets",
                apache.getReadyRockets(), ApacheEntity.ROCKET_READY_CAPACITY,
                apache.getRocketReloadFraction(), apache.getRocketResupplyFraction(),
                apache.getReserveRockets(), ApacheEntity.ROCKET_RESERVE_CAPACITY);
        rowY += MAGAZINE_ROW_HEIGHT + 4;
        drawMagazineRow(graphics, font, x, rowY, "hud.battlefield.magazine.missiles",
                apache.getReadyMissiles(), ApacheEntity.MISSILE_READY_CAPACITY,
                apache.getMissileReloadFraction(), apache.getMissileResupplyFraction(),
                apache.getReserveMissiles(), ApacheEntity.MISSILE_RESERVE_CAPACITY);
        drawStatus(graphics, font, apache, x, y + PANEL_HEIGHT - 14);
    }

    /**
     * One magazine: name, transfer bar, resupply bar. A bar with a running timer shows that
     * transfer; an idle one falls back to how full the magazine is, so it never reads as dead.
     */
    private static void drawMagazineRow(GuiGraphics graphics, Font font, int x, int y,
                                        String labelKey, int ready, int readyCapacity,
                                        float reloadFraction, float resupplyFraction,
                                        int reserve, int reserveCapacity) {
        int labelWidth = 34;
        int barX = x + 4 + labelWidth;
        int barWidth = PANEL_WIDTH - 8 - labelWidth;
        graphics.drawString(font, Component.translatable(labelKey), x + 4, y - 3,
                GunshipHudDraw.CYAN_DIM, false);
        boolean reloading = reloadFraction > 0.0F;
        float transfer = reloading ? reloadFraction : ready / (float) Math.max(1, readyCapacity);
        GunshipHudDraw.bar(graphics, barX, y, barWidth, RELOAD_BAR_HEIGHT, transfer,
                reloading ? GunshipHudDraw.CYAN : GunshipHudDraw.CYAN_DIM);
        boolean resupplying = resupplyFraction > 0.0F;
        float supply = resupplying
                ? resupplyFraction
                : reserve / (float) Math.max(1, reserveCapacity);
        GunshipHudDraw.bar(graphics, barX, y + RELOAD_BAR_HEIGHT + 2, barWidth, 3, supply,
                resupplying ? GunshipHudDraw.WHITE : GunshipHudDraw.TEXT);
    }

    /** Front seat: the chain gun and how close its barrel is to locking out. */
    private static void drawGunnerPanel(GuiGraphics graphics, Font font, ApacheEntity apache,
                                        int width, int height) {
        int x = width - PANEL_WIDTH - 6;
        int y = height - PANEL_HEIGHT - 6;
        GunshipHudDraw.panel(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT, GunshipHudDraw.CYAN);
        Component name = Component.translatable("hud.battlefield.weapon.apache_chain_gun");
        graphics.drawString(font, name, x + 4, y + 4, GunshipHudDraw.CYAN, false);
        String rounds = Component.translatable("hud.battlefield.ammo",
                apache.getChainAmmo()).getString();
        graphics.drawString(font, rounds, x + PANEL_WIDTH - 6 - font.width(rounds), y + 4,
                GunshipHudDraw.TEXT, false);
        String azimuth = String.format("%.0f", apache.getTurretRelativeBearingDeg());
        graphics.drawString(font, azimuth, x + PANEL_WIDTH - 6 - font.width(azimuth), y + 4,
                GunshipHudDraw.TEXT, false);
        float heat = apache.getChainGunHeatFraction();
        int colour = heat >= 0.9F ? GunshipHudDraw.DANGER
                : heat >= 0.6F ? GunshipHudDraw.WARNING : GunshipHudDraw.NOMINAL;
        GunshipHudDraw.bar(graphics, x + 4, y + 20, PANEL_WIDTH - 8, RELOAD_BAR_HEIGHT,
                heat, colour);
        drawStatus(graphics, font, apache, x, y + PANEL_HEIGHT - 14);
    }

    /** Rotor RPM and hull, shared by both seats. */
    private static void drawStatus(GuiGraphics graphics, Font font, ApacheEntity apache,
                                   int x, int statusY) {
        int rpm = Math.round(apache.getRenderRotorRpm() * 100.0F);
        String rotor = Component.translatable("hud.battlefield.rpm", rpm).getString();
        graphics.drawString(font, rotor, x + 4, statusY, GunshipHudDraw.TEXT, false);
        String hull = Component.translatable("hud.battlefield.health",
                Math.round(apache.getHealth())).getString();
        graphics.drawString(font, hull, x + PANEL_WIDTH - 6 - font.width(hull), statusY,
                GunshipHudDraw.TEXT, false);
    }

    private static List<Component> alerts(ApacheEntity apache, boolean pilot) {
        if (apache.getChainGunHeatFraction() >= 1.0F) {
            return List.of(Component.translatable("alert.battlefield.overheat"));
        }
        if (apache.getRenderRotorRpm() < 0.15F) {
            return List.of(Component.translatable("alert.battlefield.engine_off"));
        }
        int ready = pilot ? apache.getReadyRockets() : 0;
        int reserve = pilot ? apache.getReserveRockets() : 0;
        if (pilot && ready + reserve <= 0) {
            return List.of(Component.translatable("alert.battlefield.no_ammo"));
        }
        if (LittleBirdHudLayout.lowHealth(apache.getHealth(), ApacheEntity.MAX_HEALTH)) {
            return List.of(Component.translatable("alert.battlefield.low_health"));
        }
        return List.of();
    }
}
