package io.github.littlebird.client;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
import io.github.littlebird.entity.InfantryFightingVehicleGeometry;
import io.github.littlebird.entity.MainBattleTankEntity;
import io.github.littlebird.entity.MainBattleTankGeometry;
import io.github.littlebird.entity.ground.MainGun;
import io.github.littlebird.entity.seat.GroundSeat;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import io.github.littlebird.entity.weapon.WeaponSelection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Ground vehicle HUD for both hulls: weapon reticles, the vehicle condition panel (bottom left) and
 * the weapon stack (bottom right).
 *
 * <p>Everything drawn here is read from synched entity state - the shell carousel, the coax belt,
 * the autocannon and ATGM racks, the smoke charges and the hull health - so the HUD never shows a
 * value the server does not agree with. Each seat sees its own reticle: a driver lays the weapon
 * with their own view, so their cross is pinned to the centre, while the rear seat's weapon is
 * slaved to the turret line the driver lays and is therefore projected onto the screen.
 */
public final class GroundHud implements IGuiOverlay {
    private static final int MAIN_GUN_COLOUR = 0xFFFFE08A;
    private static final int COAX_COLOUR = 0xFF8AF0E8;
    private static final int SMOKE_COLOUR = 0xFFB8C4C8;
    private static final int TEXT_COLOUR = 0xFFE6E6E6;
    private static final int DIM_COLOUR = 0xFF9AA0A6;
    private static final int SELECTED_BACKDROP = 0xFF1D3A44;
    /** How far down the barrel line a projected reticle is cast, in blocks. */
    private static final double AIM_PROBE_BLOCKS = 64.0D;
    private static final int COAX_MIN_GAP = 3;
    private static final int COAX_MAX_GAP = 10;
    private static final int AUTO_MIN_GAP = 3;
    private static final int AUTO_MAX_GAP = 9;
    /** Placeholder spread readings until the weapons expose heat on the synched data. */
    private static final double COAX_SPREAD_PLACEHOLDER = 0.35D;
    private static final double AUTO_SPREAD_PLACEHOLDER = 0.25D;
    private static final int TANK_ROWS = 3;
    private static final int IFV_ROWS = 2;

    public GroundHud() {
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        Font font = gui.getFont();
        int hudScale = LittleBirdConfig.hudScale();
        if (minecraft.player.getVehicle() instanceof MainBattleTankEntity tank) {
            renderTank(graphics, font, minecraft, tank, partialTick, width, height, hudScale);
        } else if (minecraft.player.getVehicle() instanceof InfantryFightingVehicleEntity ifv) {
            renderIfv(graphics, font, minecraft, ifv, partialTick, width, height, hudScale);
        }
    }

    // ------------------------------------------------------------------
    // M1A5
    // ------------------------------------------------------------------

    private void renderTank(GuiGraphics graphics, Font font, Minecraft minecraft,
                            MainBattleTankEntity tank, float partialTick, int width, int height,
                            int hudScale) {
        int seat = tank.seatOf(minecraft.player);
        if (seat < 0) {
            return;
        }
        if (GroundSeat.isTankDriver(seat)) {
            GunshipHudDraw.mainGunCrosshair(graphics, width / 2, height / 2, MAIN_GUN_COLOUR);
        } else if (GroundSeat.isTankGunner(seat)) {
            Vec3 origin = tank.position().add(0.0D,
                    MainBattleTankGeometry.turretPivotHeightBlocks(), 0.0D);
            drawProjectedReticle(graphics, minecraft, origin,
                    tank.getTurretYawDeg(), tank.getTurretPitchDeg(), partialTick, width, height,
                    COAX_COLOUR,
                    GroundReticleMath.gapPixels(COAX_SPREAD_PLACEHOLDER, COAX_MIN_GAP, COAX_MAX_GAP));
        } else {
            return;
        }
        GroundHudLayout.Panels panels =
                GroundHudLayout.compute(width, height, hudScale, TANK_ROWS);
        drawCondition(graphics, font, tank.getHealth(), MainBattleTankEntity.MAX_HEALTH, panels);
        MainGunAmmoType shell = MainGunAmmoType.byOrdinal(tank.getShellTypeOrdinal());
        int selected = tank.getSelectedWeapon();
        drawRow(graphics, font, panels, 0, "1",
                Component.translatable("hud.battlefield.weapon.main_gun").getString(),
                tank.getReadyRounds() + "/" + tank.getReserveRounds(),
                Component.translatable(shell.translationKey()).getString(),
                selected == WeaponSelection.MAIN_GUN, MAIN_GUN_COLOUR);
        drawRow(graphics, font, panels, 1, "2",
                Component.translatable("hud.battlefield.weapon.coax").getString(),
                Integer.toString(tank.getCoaxAmmo()),
                Component.translatable("hud.battlefield.slot.gunner").getString(),
                selected == WeaponSelection.COAX, COAX_COLOUR);
        int cooldown = GroundHudLayout.cooldownSeconds(tank.getSmokeCooldownTicks());
        drawRow(graphics, font, panels, 2, "3",
                Component.translatable("hud.battlefield.weapon.smoke").getString(),
                Integer.toString(tank.getSmokeCharges()),
                cooldown > 0 ? cooldown + "s"
                        : Component.translatable("hud.battlefield.ready").getString(),
                selected == WeaponSelection.SMOKE, SMOKE_COLOUR);
    }

    // ------------------------------------------------------------------
    // EBAA Wildcat
    // ------------------------------------------------------------------

    private void renderIfv(GuiGraphics graphics, Font font, Minecraft minecraft,
                           InfantryFightingVehicleEntity ifv, float partialTick, int width,
                           int height, int hudScale) {
        int seat = ifv.seatOf(minecraft.player);
        if (seat < 0) {
            return;
        }
        if (GroundSeat.isIfvDriver(seat)) {
            // The 30mm is a rapid weapon: a tight reticle rather than the tank's heavy cross.
            int gap = GroundReticleMath.gapPixels(AUTO_SPREAD_PLACEHOLDER, AUTO_MIN_GAP,
                    AUTO_MAX_GAP);
            GunshipHudDraw.coaxCrosshair(graphics, width / 2, height / 2, gap, COAX_COLOUR);
        } else if (GroundSeat.isIfvGunner(seat)) {
            Vec3 origin = ifv.position().add(0.0D,
                    InfantryFightingVehicleGeometry.turretPivotHeightBlocks(), 0.0D);
            drawProjectedReticle(graphics, minecraft, origin,
                    ifv.getTurretYawDeg(), ifv.getTurretPitchDeg(), partialTick, width, height,
                    MAIN_GUN_COLOUR, COAX_MIN_GAP + 3);
        } else {
            return;
        }
        GroundHudLayout.Panels panels = GroundHudLayout.compute(width, height, hudScale, IFV_ROWS);
        drawCondition(graphics, font, ifv.getHealth(), InfantryFightingVehicleEntity.MAX_HEALTH,
                panels);
        drawRow(graphics, font, panels, 0, "1",
                Component.translatable("hud.battlefield.weapon.ifv_autocannon").getString(),
                Integer.toString(ifv.getAutocannonAmmo()),
                Component.translatable("hud.battlefield.slot.driver").getString(),
                true, COAX_COLOUR);
        drawRow(graphics, font, panels, 1, "2",
                Component.translatable("hud.battlefield.weapon.ifv_atgm").getString(),
                ifv.getAtgmReady() + "/" + ifv.getAtgmReserve(),
                Component.translatable("hud.battlefield.slot.gunner").getString(),
                false, MAIN_GUN_COLOUR);
    }

    // ------------------------------------------------------------------
    // Shared drawing
    // ------------------------------------------------------------------

    /** Projects a turret line onto the screen and draws the reticle where it actually points. */
    private void drawProjectedReticle(GuiGraphics graphics, Minecraft minecraft, Vec3 origin,
                                      double aimYawDeg, double aimPitchDeg, float partialTick,
                                      int width, int height, int colour, int gap) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        LittleBirdBallistics.Vector3 aim = MainGun.aimDirection(aimYawDeg, aimPitchDeg);
        Vec3 probe = origin.add(aim.x() * AIM_PROBE_BLOCKS, aim.y() * AIM_PROBE_BLOCKS,
                aim.z() * AIM_PROBE_BLOCKS);

        float verticalFov = minecraft.options.fov().get();
        double horizontalFov = GroundReticleMath.horizontalFovDeg(verticalFov, width, height);
        double bearing = GroundReticleMath.relativeBearingDeg(
                aimYawDeg, minecraft.player.getViewYRot(partialTick));
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(probe.x, probe.y, probe.z),
                new AimProjection.Vector(eye.x, eye.y, eye.z),
                minecraft.player.getViewYRot(partialTick),
                minecraft.player.getViewXRot(partialTick),
                0.0F, verticalFov, width, height);
        if (!screen.visible() || !GroundReticleMath.withinFov(bearing, horizontalFov)) {
            return;
        }
        GunshipHudDraw.coaxCrosshair(graphics, (int) Math.round(screen.x()),
                (int) Math.round(screen.y()), gap, colour);
    }

    /** Hull condition: bar, percentage and a LOW HULL warning once it is hurt. */
    private void drawCondition(GuiGraphics graphics, Font font, float health, float maxHealth,
                               GroundHudLayout.Panels panels) {
        GroundHudLayout.Rect rect = panels.health();
        float fraction = maxHealth <= 0.0F ? 0.0F : health / maxHealth;
        boolean hurt = GroundHudLayout.healthWarning(fraction);
        int colour = hurt ? GunshipHudDraw.DANGER
                : fraction < 0.7F ? GunshipHudDraw.WARNING : GunshipHudDraw.NOMINAL;
        GunshipHudDraw.panel(graphics, rect.x(), rect.y(), rect.width(), rect.height(), colour);
        GunshipHudDraw.bar(graphics, rect.x() + 4, rect.y() + 7, rect.width() - 8, 6, fraction,
                colour);
        String percent = Math.round(Math.max(0.0F, fraction) * 100.0F) + "%";
        String label = Component.translatable(hurt
                ? "hud.battlefield.vehicle.low_hull"
                : "hud.battlefield.vehicle.hull").getString() + " " + percent;
        graphics.drawString(font, label, rect.x() + 4, rect.y() + 16,
                hurt ? GunshipHudDraw.DANGER : TEXT_COLOUR, false);
    }

    private void drawRow(GuiGraphics graphics, Font font, GroundHudLayout.Panels panels, int row,
                         String key, String name, String amount, String detail, boolean selected,
                         int accent) {
        GroundHudLayout.Rect rect = panels.weapons();
        int y = GroundHudLayout.rowY(panels, row);
        int rowHeight = panels.rowHeight();
        if (selected) {
            graphics.fill(rect.x() + 1, y + 1, rect.x() + rect.width() - 1, y + rowHeight - 1,
                    SELECTED_BACKDROP);
            graphics.fill(rect.x(), y, rect.x() + 2, y + rowHeight, accent);
        }
        int keyColour = selected ? accent : DIM_COLOUR;
        graphics.drawString(font, key, rect.x() + 5, y + 3, keyColour, false);
        graphics.drawString(font, name, rect.x() + 16, y + 3,
                selected ? TEXT_COLOUR : DIM_COLOUR, false);
        int amountWidth = font.width(amount);
        graphics.drawString(font, amount, rect.x() + rect.width() - 6 - amountWidth, y + 3,
                selected ? accent : DIM_COLOUR, false);
        graphics.drawString(font, detail, rect.x() + 16, y + 3 + font.lineHeight,
                DIM_COLOUR, false);
    }
}
