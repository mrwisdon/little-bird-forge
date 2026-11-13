package io.github.littlebird.client.hud;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.AimProjection;
import io.github.littlebird.client.GroundHudLayout;
import io.github.littlebird.client.GroundReticleMath;
import io.github.littlebird.client.GunSightDraw;
import io.github.littlebird.client.GunSightMath;
import io.github.littlebird.client.LittleBirdKeyMappings;
import io.github.littlebird.client.TankOptic;
import io.github.littlebird.entity.BradleyEntity;
import io.github.littlebird.entity.BradleyGeometry;
import io.github.littlebird.entity.InfantryFightingVehicleEntity;
import io.github.littlebird.entity.InfantryFightingVehicleGeometry;
import io.github.littlebird.entity.MainBattleTankEntity;
import io.github.littlebird.entity.MainBattleTankGeometry;
import io.github.littlebird.entity.ground.GroundWeapons;
import io.github.littlebird.entity.ground.MainGun;
import io.github.littlebird.entity.seat.GroundSeat;
import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import io.github.littlebird.entity.weapon.MainGunAmmoType;
import io.github.littlebird.entity.weapon.MainGunFireControl;
import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import io.github.littlebird.entity.weapon.WeaponSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Ground vehicle HUD for all three hulls - the M1A5 (M1A2 SEP) tank, the EBAA Wildcat IFV and the
 * M2A2 Bradley - drawn on the shared {@link HudCanvas} chrome.
 *
 * <p>Every number is read from synched entity state (the ready rack and its reserve, the coax and
 * cupola belts, the autocannon and ATGM racks, the smoke charges and the hull health), so the HUD
 * never shows a value the server does not agree with. Each seat sees its own reticle: a driver lays
 * the weapon with their own view, so their cross is pinned to the centre, while the other seat's
 * weapon is slaved to the turret line and is therefore projected onto the screen. A turret interior
 * view replaces the plain reticle with the gun line's own optical sight.
 *
 * <p>Layout: hull condition bottom left, weapon stack and ammunition readout bottom right. The
 * geometry comes from {@link GroundHudLayout}, so the edge margin keeps the {@code hudScale}
 * multiplier, the vanilla hotbar lift and the aircraft HUD's corner clearances.
 */
public final class HudGround implements HudView {
    private static final int MAIN_GUN_COLOUR = HudStyle.ACCENT;
    private static final int COAX_COLOUR = HudStyle.OK;
    private static final int SMOKE_COLOUR = HudStyle.MUTED;
    private static final int TEXT_COLOUR = HudStyle.TEXT;
    private static final int INK_ON_ACCENT = 0xFF101418;

    /** How far down the barrel line a projected reticle is cast, in blocks. */
    private static final double AIM_PROBE_BLOCKS = 64.0D;
    private static final int RETICLE_ARM = 5;
    private static final int MAIN_GUN_GAP = 6;
    private static final int MAIN_GUN_ARM = 8;
    private static final int COAX_MIN_GAP = 3;
    private static final int COAX_MAX_GAP = 10;
    private static final int AUTO_MIN_GAP = 3;
    private static final int AUTO_MAX_GAP = 9;
    /** Placeholder spread readings until the weapons expose heat on the synched data. */
    private static final double COAX_SPREAD_PLACEHOLDER = 0.35D;
    private static final double AUTO_SPREAD_PLACEHOLDER = 0.25D;

    /** One row per weapon the hull actually carries. */
    private static final int TANK_ROWS = 3;
    private static final int IFV_ROWS = 2;
    private static final int BRADLEY_ROWS = 3;

    /** Hull condition panel: header (11) + bar (6) + state line (9) + padding. */
    private static final int STATUS_WIDTH = 140;
    private static final int STATUS_HEIGHT = 38;
    /** Weapon panel: header + slots + legends + readout rows + reload bar. */
    private static final int WEAPON_WIDTH = 160;
    private static final int WEAPON_PANEL_HEIGHT = 86;
    private static final int SLOT_HEIGHT = 26;
    private static final int SLOT_TOP = HudStyle.HEADER + 3;
    private static final int SLOT_ICON_Y = 9;
    private static final int SLOT_BADGE_Y = 13;
    /** Old alert strip warned a hair earlier than the panel state did; both thresholds kept. */
    private static final float LOW_HULL_ALERT_FRACTION = 0.35F;

    @Override
    public boolean appliesTo(HudContext context) {
        if (context.minecraft.player == null) {
            return false;
        }
        var vehicle = context.minecraft.player.getVehicle();
        return vehicle instanceof MainBattleTankEntity
                || vehicle instanceof InfantryFightingVehicleEntity
                || vehicle instanceof BradleyEntity;
    }

    @Override
    public void render(HudContext context) {
        var vehicle = context.minecraft.player.getVehicle();
        if (vehicle instanceof MainBattleTankEntity tank) {
            renderTank(context, tank);
        } else if (vehicle instanceof InfantryFightingVehicleEntity ifv) {
            renderIfv(context, ifv);
        } else if (vehicle instanceof BradleyEntity bradley) {
            renderBradley(context, bradley);
        } else {
            return;
        }
        // Alerts last, above the panels: the crew reads "low hull / reloading" before any number.
        drawAlerts(context);
        // The same target readout the aircraft HUDs draw, so a ground crew sees the damage it lands.
        HudFeedback.enemyBar(context.graphics, context.font, context.width, context.height);
    }

    // ------------------------------------------------------------------
    // M1A5
    // ------------------------------------------------------------------

    private void renderTank(HudContext context, MainBattleTankEntity tank) {
        Minecraft minecraft = context.minecraft;
        int seat = tank.seatOf(minecraft.player);
        if (seat < 0) {
            return;
        }
        double aimYaw = tank.renderTurretYawDeg(context.partialTick);
        double aimPitch = tank.renderTurretPitchDeg(context.partialTick);
        MainGunAmmoType shell = MainGunAmmoType.byOrdinal(tank.getShellTypeOrdinal());
        if (TankOptic.active(tank, minecraft.player)) {
            drawTankOptic(context, tank, shell);
        } else if (tank.isInteriorView() && GroundSeat.isTankGunner(seat)) {
            // Seat two's own cupola optic: first person, free to look all the way round, no turret
            // lock. The mark is the centre of his screen because the camera turns with him.
            drawCupolaSight(context, tank);
        } else if (tank.isInteriorView()) {
            drawTurretInterior(context, tank.position(),
                    MainBattleTankGeometry.turretPivotHeightBlocks(), aimYaw, aimPitch,
                    COAX_COLOUR, false,
                    MainGunShellBallistics.MUZZLE_SPEED, MainGunShellBallistics.GRAVITY,
                    MainBattleTankGeometry.gunTrunnionHeightBlocks(),
                    MainBattleTankGeometry.gunTrunnionForwardBlocks(),
                    MainBattleTankGeometry.barrelReachBlocks());
        } else if (GroundSeat.isTankDriver(seat)) {
            // The main gun is laid by the driver, so its heavy cross sits in the middle.
            HudCanvas.reticle(context.graphics, context.centreX(), context.centreY(),
                    MAIN_GUN_GAP, MAIN_GUN_ARM, MAIN_GUN_COLOUR);
        } else if (GroundSeat.isTankGunner(seat)) {
            // Seat two works his own cupola gun, so the mark follows his own view rather than the
            // turret the driver is traversing. In the interior view the lockstep camera already
            // makes his view and the turret one, so the branch above stays in charge there.
            Vec3 origin = tank.position().add(0.0D,
                    MainBattleTankGeometry.turretPivotHeightBlocks(), 0.0D);
            drawProjectedReticle(context, origin,
                    minecraft.player.getViewYRot(context.partialTick),
                    minecraft.player.getViewXRot(context.partialTick), COAX_COLOUR,
                    GroundReticleMath.gapPixels(COAX_SPREAD_PLACEHOLDER, COAX_MIN_GAP,
                            COAX_MAX_GAP));
        } else {
            return;
        }

        GroundHudLayout.Panels panels =
                GroundHudLayout.compute(context.width, context.height, hudScale(), TANK_ROWS);
        drawStatusPanel(context, panels, tank.getHealth(), MainBattleTankEntity.MAX_HEALTH,
                "hud.battlefield.vehicle.tank");

        String weaponKey = keyName(LittleBirdKeyMappings.WEAPON_SWITCH);
        int selected = tank.getSelectedWeapon();
        // The two machine guns have separate belts, so the belt row shows the one this seat
        // actually fires: the cupola for seat two, the coax for the driver.
        int beltAmmo = GroundSeat.isTankGunner(seat) ? tank.getCupolaAmmo() : tank.getCoaxAmmo();
        int smokeCooldown = GroundHudLayout.cooldownSeconds(tank.getSmokeCooldownTicks());
        int reloadTotal = LittleBirdConfig.tankMainGunMagazine().reloadTicks();
        int reloadLeft = tank.getMainGunReloadTicks();
        double reloadRemaining = reloadTotal > 0 && reloadLeft > 0
                ? Math.min(1.0D, reloadLeft / (double) reloadTotal) : 0.0D;
        double reloadFraction = reloadTotal <= 0 || reloadLeft <= 0 ? 1.0D
                : 1.0D - Math.min(1.0D, reloadLeft / (double) reloadTotal);

        Slot[] slots = new Slot[]{
            new Slot(SlotIcon.SHELL, Integer.toString(tank.getReadyRounds()), weaponKey,
                    selected == WeaponSelection.MAIN_GUN, reloadRemaining),
            new Slot(SlotIcon.BELT, Integer.toString(beltAmmo), weaponKey,
                    selected == WeaponSelection.COAX, 0.0D),
            new Slot(SlotIcon.SMOKE,
                    smokeCooldown > 0 ? smokeCooldown + "s" : Integer.toString(tank.getSmokeCharges()),
                    weaponKey, selected == WeaponSelection.SMOKE, 0.0D)
        };

        String shellName = tr("hud.battlefield.weapon.main_gun") + " " + tr(shell.translationKey())
                + "  " + keyName(LittleBirdKeyMappings.SHELL_SWITCH);
        // The bar is the reload cooldown; the reserve dims while the resupply clock runs.
        drawWeaponPanel(context, panels, TANK_ROWS, shellName,
                reloadLeft > 0 ? "RELOAD" : "READY", slots,
                new Readout("READY", String.format(Locale.ROOT, "%03d", tank.getReadyRounds()),
                        tank.getReadyRounds() > 0 ? MAIN_GUN_COLOUR : HudStyle.DANGER),
                new Readout("RESERVE", Integer.toString(tank.getReserveRounds()),
                        tank.getMainGunResupplyTicks() > 0 ? HudStyle.MUTED : TEXT_COLOUR),
                reloadFraction, reloadLeft > 0);
        drawHint(context, true, true);
    }

    private void drawTankOptic(HudContext context, MainBattleTankEntity tank, MainGunAmmoType shell) {
        TankOptic.Reading reading = TankOptic.reading(tank);
        GunSightMath.Tube tube = GunSightDraw.tankSight(context.graphics, context.width, context.height,
                LittleBirdConfig.groundSightFovMultiplier(), TankOptic.focalLengthPixels(context.height));
        GunSightMath.Rect opening = tube.opening();
        int x = context.centreX();
        HudFeedback.hitMarker(context.graphics, x, context.centreY());
        int lineHeight = context.font.lineHeight;
        if (opening.height() >= 3 * lineHeight + 12) {
            drawOpticText(context, opening, "120 mm  " + tr(shell.translationKey()),
                    x, opening.y() + 5, GunSightDraw.RETICLE_COLOUR);
            double range = reading == null ? Double.NaN : reading.laserRangeBlocks();
            String rangeValue = Double.isFinite(range) ? Math.round(range) + " m" : "—";
            String rangeText = Component.translatable("hud.battlefield.optic.range", rangeValue)
                    .getString();
            drawOpticText(context, opening, rangeText, x, opening.bottom() - 2 * lineHeight - 9,
                    GunSightDraw.RETICLE_COLOUR);
        }
        if (reading == null) {
            return;
        }

        GunSightDraw.tankBore(context.graphics, tube,
                TankOptic.project(reading.borePoint(), context.width, context.height));
        TankOptic.Flight flight = reading.flight();
        String fireControl;
        if (flight.hasImpact()) {
            boolean entity = flight.kind() == TankOptic.ImpactKind.ENTITY;
            boolean drawn = GunSightDraw.tankImpact(context.graphics, tube,
                    TankOptic.project(flight.point(), context.width, context.height), entity);
            String key = drawn
                    ? (entity ? "hud.battlefield.optic.target" : "hud.battlefield.optic.impact")
                    : "hud.battlefield.optic.off_sight";
            fireControl = Component.translatable(key, Math.round(flight.rangeBlocks()),
                    String.format(Locale.ROOT, "%.1f", flight.ticks() / 20.0D)).getString();
        } else {
            fireControl = switch (flight.kind()) {
                case CANISTER -> Component.translatable("hud.battlefield.optic.canister",
                        Math.round(LittleBirdConfig.tankCanisterRange())).getString();
                case UNLOADED -> tr("hud.battlefield.optic.unloaded");
                default -> tr("hud.battlefield.optic.no_impact");
            };
        }
        drawOpticText(context, opening, fireControl, x, opening.bottom() - context.font.lineHeight - 5,
                GunSightDraw.IMPACT_COLOUR);
    }

    private static void drawOpticText(HudContext context, GunSightMath.Rect opening,
                                      String text, int centreX, int y, int colour) {
        int width = context.font.width(text);
        float scale = Math.min(1.0F, (opening.width() - 8.0F) / Math.max(1, width));
        if (scale < 0.65F || y < opening.y() + 2
                || y + context.font.lineHeight + 1 >= opening.bottom()) {
            return;
        }
        context.graphics.pose().pushPose();
        context.graphics.pose().translate(centreX, y, 0.0F);
        context.graphics.pose().scale(scale, scale, 1.0F);
        int left = -width / 2;
        context.graphics.fill(left - 3, -2, left + width + 3, context.font.lineHeight + 1,
                0xCC090E0C);
        context.graphics.drawString(context.font, text, left, 0, colour, false);
        context.graphics.pose().popPose();
    }

    // ------------------------------------------------------------------
    // EBAA Wildcat
    // ------------------------------------------------------------------

    private void renderIfv(HudContext context, InfantryFightingVehicleEntity ifv) {
        Minecraft minecraft = context.minecraft;
        int seat = ifv.seatOf(minecraft.player);
        if (seat < 0) {
            return;
        }
        double aimYaw = ifv.renderTurretYawDeg(context.partialTick);
        double aimPitch = ifv.renderTurretPitchDeg(context.partialTick);
        if (GroundSeat.isIfvDriver(seat)) {
            drawIfvReticle(context, ifv, aimYaw, aimPitch);
        } else if (GroundSeat.isIfvGunner(seat)) {
            Vec3 origin = ifv.position().add(0.0D,
                    InfantryFightingVehicleGeometry.turretPivotHeightBlocks(), 0.0D);
            drawProjectedReticle(context, origin, aimYaw, aimPitch, MAIN_GUN_COLOUR,
                    COAX_MIN_GAP + 3);
        } else {
            return;
        }

        GroundHudLayout.Panels panels =
                GroundHudLayout.compute(context.width, context.height, hudScale(), IFV_ROWS);
        drawStatusPanel(context, panels, ifv.getHealth(), InfantryFightingVehicleEntity.MAX_HEALTH,
                "hud.battlefield.vehicle.ifv");
        Slot[] slots = new Slot[]{
            new Slot(SlotIcon.BELT, Integer.toString(ifv.getAutocannonAmmo()),
                    tr("hud.battlefield.slot.driver"), true, 0.0D),
            new Slot(SlotIcon.SHELL, Integer.toString(ifv.getAtgmReady()),
                    tr("hud.battlefield.slot.gunner"), false, 0.0D)
        };
        drawWeaponPanel(context, panels, IFV_ROWS,
                tr("hud.battlefield.weapon.ifv_autocannon"), "READY", slots,
                new Readout("READY", String.format(Locale.ROOT, "%03d", ifv.getAutocannonAmmo()),
                        ifv.getAutocannonAmmo() > 0 ? MAIN_GUN_COLOUR : HudStyle.DANGER),
                new Readout("ATGM", Integer.toString(ifv.getAtgmReserve()), TEXT_COLOUR),
                1.0D, false);
        drawHint(context, false, false);
    }

    // ------------------------------------------------------------------
    // M2A2 Bradley
    // ------------------------------------------------------------------

    /**
     * Seat 0 works the turret and cycles three weapons with the weapon key, so its reticle changes
     * with the slot: a tight 25mm cross, a projected TOW line (the missile flies to the spot the
     * sight ends on) and a smoke marker. Seat 1's roof gun follows its own gunner's view, so its
     * cross is pinned to the centre like a driver's.
     */
    private void renderBradley(HudContext context, BradleyEntity bradley) {
        Minecraft minecraft = context.minecraft;
        int seat = bradley.seatOf(minecraft.player);
        if (seat < 0) {
            return;
        }
        double aimYaw = bradley.renderTurretYawDeg(context.partialTick);
        double aimPitch = bradley.renderTurretPitchDeg(context.partialTick);
        if (bradley.isInteriorView()) {
            // The 25mm is the Bradley's main sight, so it gets the same optical tube the tank has -
            // but its ladder is calibrated to the autocannon's own shell. TOW and smoke keep the
            // plain cross.
            boolean autocannonSight = bradley.getWeapon() == BradleyEntity.WEAPON_AUTOCANNON;
            drawTurretInterior(context, bradley.position(),
                    BradleyGeometry.turretPivotHeightBlocks(), aimYaw, aimPitch,
                    COAX_COLOUR, autocannonSight,
                    LittleBirdConfig.bradleyAutocannonShellSpeed(), MainGunShellBallistics.GRAVITY,
                    BradleyGeometry.gunTrunnionHeightBlocks(),
                    BradleyGeometry.gunTrunnionForwardBlocks(),
                    BradleyGeometry.barrelReachBlocks());
        } else if (GroundSeat.isBradleyDriver(seat)) {
            if (bradley.getWeapon() == BradleyEntity.WEAPON_TOW) {
                Vec3 origin = bradley.position().add(0.0D,
                        BradleyGeometry.turretPivotHeightBlocks(), 0.0D);
                drawProjectedReticle(context, origin, aimYaw, aimPitch, MAIN_GUN_COLOUR,
                        COAX_MIN_GAP + 2);
            } else if (bradley.getWeapon() == BradleyEntity.WEAPON_SMOKE) {
                HudCanvas.reticle(context.graphics, context.centreX(), context.centreY(),
                        AUTO_MIN_GAP + 6, RETICLE_ARM, SMOKE_COLOUR);
            } else {
                int gap = GroundReticleMath.gapPixels(AUTO_SPREAD_PLACEHOLDER, AUTO_MIN_GAP,
                        AUTO_MAX_GAP);
                HudCanvas.reticle(context.graphics, context.centreX(), context.centreY(), gap,
                        RETICLE_ARM, COAX_COLOUR);
            }
        } else if (GroundSeat.isBradleyGunner(seat)) {
            int gap = GroundReticleMath.gapPixels(COAX_SPREAD_PLACEHOLDER, COAX_MIN_GAP,
                    COAX_MAX_GAP);
            HudCanvas.reticle(context.graphics, context.centreX(), context.centreY(), gap,
                    RETICLE_ARM, MAIN_GUN_COLOUR);
        } else {
            return;
        }

        GroundHudLayout.Panels panels =
                GroundHudLayout.compute(context.width, context.height, hudScale(), BRADLEY_ROWS);
        drawStatusPanel(context, panels, bradley.getHealth(), BradleyEntity.MAX_HEALTH,
                "hud.battlefield.vehicle.bradley");

        int weapon = bradley.getWeapon();
        Slot[] slots = new Slot[]{
            new Slot(SlotIcon.BELT, Integer.toString(bradley.getAutocannonReady()),
                    tr("hud.battlefield.slot.driver"), weapon == BradleyEntity.WEAPON_AUTOCANNON,
                    0.0D),
            new Slot(SlotIcon.SHELL, Integer.toString(bradley.getTowReady()),
                    tr("hud.battlefield.slot.gunner"), weapon == BradleyEntity.WEAPON_TOW, 0.0D),
            new Slot(SlotIcon.SMOKE, Integer.toString(bradley.getSmokeCharges()),
                    tr("hud.battlefield.slot.smoke"), weapon == BradleyEntity.WEAPON_SMOKE, 0.0D)
        };
        String weaponName = switch (weapon) {
            case BradleyEntity.WEAPON_TOW -> tr("hud.battlefield.weapon.bradley_tow");
            case BradleyEntity.WEAPON_SMOKE -> tr("hud.battlefield.weapon.bradley_smoke");
            default -> tr("hud.battlefield.weapon.bradley_autocannon");
        };
        drawWeaponPanel(context, panels, BRADLEY_ROWS, weaponName, "READY", slots,
                new Readout("READY", String.format(Locale.ROOT, "%03d", bradley.getAutocannonReady()),
                        bradley.getAutocannonReady() > 0 ? MAIN_GUN_COLOUR : HudStyle.DANGER),
                new Readout("RESERVE", Integer.toString(bradley.getAutocannonReserve()),
                        TEXT_COLOUR),
                1.0D, false);
        drawHint(context, true, false);
    }

    // ------------------------------------------------------------------
    // Panels
    // ------------------------------------------------------------------

    /** Hull condition: name, bar, percentage and a LOW HULL warning once it is hurt. */
    private void drawStatusPanel(HudContext context, GroundHudLayout.Panels panels, float health,
                                 float maxHealth, String vehicleKey) {
        Font font = context.font;
        GuiGraphics graphics = context.graphics;
        GroundHudLayout.Rect reference = panels.health();
        int margin = reference.x();
        int bottom = reference.y() + reference.height();
        int x = reference.x();
        int y = Math.max(margin, bottom - STATUS_HEIGHT);
        float fraction = maxHealth <= 0.0F ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, health / maxHealth));
        boolean low = GroundHudLayout.healthWarning(fraction);
        int colour = low ? HudStyle.DANGER : fraction < 0.7F ? HudStyle.WARN : HudStyle.OK;
        String percent = Math.round(fraction * 100.0F) + "%";
        HudCanvas.panel(graphics, font, tr(vehicleKey), percent, x, y, STATUS_WIDTH,
                STATUS_HEIGHT, colour);
        HudCanvas.bar(graphics, x + HudStyle.PAD, y + HudStyle.HEADER + HudStyle.PAD,
                STATUS_WIDTH - HudStyle.PAD * 2, 6, fraction, colour);
        String label = tr(low ? "hud.battlefield.vehicle.low_hull" : "hud.battlefield.vehicle.hull")
                + " " + percent;
        HudCanvas.line(graphics, font, x + HudStyle.PAD,
                y + HudStyle.HEADER + HudStyle.PAD + 9, label,
                low ? HudStyle.DANGER : TEXT_COLOUR);
    }

    /** Weapon slots, then the ammunition readout and the reload bar, in one bottom-right panel. */
    private void drawWeaponPanel(HudContext context, GroundHudLayout.Panels panels, int rows,
                                 String title, String tag, Slot[] slots, Readout ready,
                                 Readout reserve, double reloadFraction, boolean reloading) {
        Font font = context.font;
        GuiGraphics graphics = context.graphics;
        int right = panels.weapons().x() + panels.weapons().width();
        int bottom = panels.weapons().y() + panels.weapons().height();
        int margin = panels.health().x();
        int x = Math.max(margin, right - WEAPON_WIDTH);
        int y = Math.max(margin, bottom - WEAPON_PANEL_HEIGHT);
        HudCanvas.panel(graphics, font, title, tag, x, y, WEAPON_WIDTH, WEAPON_PANEL_HEIGHT,
                HudStyle.ACCENT);

        int barWidth = GroundHudLayout.barWidth(rows);
        int slotX = x + (WEAPON_WIDTH - barWidth) / 2;
        int slotY = y + SLOT_TOP;
        for (int index = 0; index < slots.length; index++) {
            int width = GroundHudLayout.slotWidth(index);
            drawSlot(graphics, font, slotX, slotY, width, SLOT_HEIGHT, slots[index]);
            slotX += width + GroundHudLayout.SLOT_GAP;
        }

        int dividerY = slotY + SLOT_HEIGHT + 12;
        graphics.fill(x + HudStyle.PAD, dividerY, x + WEAPON_WIDTH - HudStyle.PAD, dividerY + 1,
                HudStyle.EDGE);
        int rowRight = x + WEAPON_WIDTH - HudStyle.PAD;
        int rowY = dividerY + 5;
        HudCanvas.row(graphics, font, x + HudStyle.PAD, rowRight, rowY, ready.label(),
                ready.value(), ready.colour());
        HudCanvas.row(graphics, font, x + HudStyle.PAD, rowRight, rowY + HudStyle.ROW,
                reserve.label(), reserve.value(), reserve.colour());
        HudCanvas.bar(graphics, x + HudStyle.PAD, y + WEAPON_PANEL_HEIGHT - 9,
                WEAPON_WIDTH - HudStyle.PAD * 2, 4, reloadFraction,
                reloading ? HudStyle.WARN : HudStyle.ACCENT);
    }

    /**
     * One weapon slot in the reference's style: a procedural icon, the ammo badge, the key legend
     * underneath, a cooldown strip while it is cycling and a filled block when it is selected.
     */
    private void drawSlot(GuiGraphics graphics, Font font, int x, int y, int width, int height,
                          Slot slot) {
        int background = slot.selected() ? HudStyle.ACCENT : HudStyle.PANEL_STRIP;
        int ink = slot.selected() ? INK_ON_ACCENT : HudStyle.TEXT;
        graphics.fill(x, y, x + width, y + height, background);
        graphics.fill(x, y, x + width, y + 1,
                slot.selected() ? HudStyle.TEXT : HudStyle.EDGE);
        drawIcon(graphics, x + width / 2, y + SLOT_ICON_Y, ink, slot.icon());
        if (slot.badge() != null && !slot.badge().isEmpty()) {
            graphics.drawString(font, slot.badge(), x + width - 2 - font.width(slot.badge()),
                    y + SLOT_BADGE_Y, ink, true);
        }
        if (slot.cooldown() > 0.0D) {
            HudCanvas.bar(graphics, x + 1, y + height - 3, width - 2, 2,
                    Math.min(1.0D, slot.cooldown()), HudStyle.WARN);
        }
        if (slot.legend() != null && !slot.legend().isEmpty()) {
            graphics.drawString(font, slot.legend(),
                    x + width / 2 - font.width(slot.legend()) / 2, y + height + 2,
                    slot.selected() ? HudStyle.ACCENT : HudStyle.MUTED, false);
        }
    }

    /** The same procedural slot glyphs the weapons screen used: no textures, any GUI scale. */
    private static void drawIcon(GuiGraphics graphics, int centreX, int centreY, int ink,
                                 SlotIcon icon) {
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
    }

    // ------------------------------------------------------------------
    // Reticles
    // ------------------------------------------------------------------

    /** Projects a turret line onto the screen and draws the reticle where it actually points. */
    /**
     * The Wildcat's own reticle, deliberately not shared with the tank.
     *
     * <p>The 30mm mount elevates to 85 degrees and the tank's main-gun machinery clamps at 30 in
     * several places, so anything routed through it drags this sight down. This draws the barrel's
     * own line: raw aim angles, projected through the camera actually on screen, with the 30mm's
     * spread gap and a range readout. The mark leaves the screen when the barrel is off screen
     * (third person looking away from the gun) rather than lying about where the rounds go.
     */
    private void drawIfvReticle(HudContext context, InfantryFightingVehicleEntity ifv,
                                double aimYawDeg, double aimPitchDeg) {
        Vec3 origin = ifv.position().add(0.0D,
                InfantryFightingVehicleGeometry.turretPivotHeightBlocks(), 0.0D);
        double yaw = Math.toRadians(aimYawDeg);
        double pitch = Math.toRadians(aimPitchDeg);
        double cosPitch = Math.cos(pitch);
        Vec3 probe = origin.add(
                -Math.sin(yaw) * cosPitch * AIM_PROBE_BLOCKS,
                -Math.sin(pitch) * AIM_PROBE_BLOCKS,
                Math.cos(yaw) * cosPitch * AIM_PROBE_BLOCKS);
        Camera camera = context.minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        // Exterior camera: no sight zoom, but the third person FOV dial still applies to the real
        // camera, so the projection has to use the same scaled field of view and nothing else.
        double fovFactor = io.github.littlebird.client.GroundViewMath.fovMultiplier(true, false,
                LittleBirdConfig.thirdPersonFovMultiplier(),
                LittleBirdConfig.groundSightFovMultiplier());
        float verticalFov = (float) (context.minecraft.options.fov().get() * fovFactor);
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(probe.x, probe.y, probe.z),
                new AimProjection.Vector(eye.x, eye.y, eye.z),
                camera.getYRot(), camera.getXRot(), 0.0F, verticalFov,
                context.width, context.height);
        if (!screen.visible()) {
            return;
        }
        int x = (int) Math.round(screen.x());
        int y = (int) Math.round(screen.y());
        int gap = GroundReticleMath.gapPixels(AUTO_SPREAD_PLACEHOLDER, AUTO_MIN_GAP,
                AUTO_MAX_GAP);
        HudCanvas.reticle(context.graphics, x, y, gap, RETICLE_ARM, COAX_COLOUR);
        double range = rangeAlongGunLine(context, origin, aimYawDeg, aimPitchDeg);
        String text = Math.round(range) + " m";
        context.graphics.drawString(context.font, text, x - context.font.width(text) / 2,
                y + 18, TEXT_COLOUR, false);
    }

    /**
     * The loader/commander's cupola optic. Unlike the 120mm sight this one is not tied to the
     * turret: it is a first person view that turns with the player, so the middle of the screen is
     * always where his own machine gun points and he can keep the whole circle under watch.
     */
    private void drawCupolaSight(HudContext context, MainBattleTankEntity tank) {
        Minecraft minecraft = context.minecraft;
        int cx = context.centreX();
        int cy = context.centreY();
        HudCanvas.reticle(context.graphics, cx, cy, COAX_MIN_GAP, RETICLE_ARM, COAX_COLOUR);

        float heading = net.minecraft.util.Mth.wrapDegrees(minecraft.player.getYRot());
        String hdg = String.format(Locale.ROOT, "HDG %03d",
                Math.round((heading + 360.0F) % 360.0F));
        HudCanvas.line(context.graphics, context.font, cx - context.font.width(hdg) / 2,
                cy - 56, hdg, COAX_COLOUR);

        String belt = String.format(Locale.ROOT, "M2  %03d", tank.getCupolaAmmo());
        HudCanvas.line(context.graphics, context.font, cx - context.font.width(belt) / 2,
                cy + 48, belt, TEXT_COLOUR);

        net.minecraft.world.phys.HitResult hit = minecraft.player.pick(96.0D, 0.0F, false);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            double range = hit.getLocation().distanceTo(minecraft.player.getEyePosition());
            String rng = String.format(Locale.ROOT, "RNG %03d", (int) Math.round(range));
            HudCanvas.line(context.graphics, context.font, cx - context.font.width(rng) / 2,
                    cy + 60, rng, HudStyle.MUTED);
        }
    }

    private void drawProjectedReticle(HudContext context, Vec3 origin, double aimYawDeg,
                                      double aimPitchDeg, int colour, int gap) {
        // Exterior reticles keep the player's view yaw: the camera is not slaved to anything here.
        AimProjection.Screen screen = projectAimPoint(context, origin, aimYawDeg, aimPitchDeg,
                context.minecraft.player.getViewYRot(context.partialTick), 1.0D);
        if (screen.visible()) {
            HudCanvas.reticle(context.graphics, (int) Math.round(screen.x()),
                    (int) Math.round(screen.y()), Math.max(1, gap), RETICLE_ARM, colour);
        }
    }

    /**
     * Turret interior fire control. A sighted gun draws the full optic - a rigid tube, a mil drop
     * ladder calibrated to that gun's own shell and the centre aim point - while the plain reticles
     * keep their crosshair. Either way the mark sits on the gun line rather than in the middle of
     * the screen, with the range to the first collider and the hit marker.
     *
     * @param opticalSight            this weapon is aimed through the magnifying sight
     * @param shellSpeedBlocksPerTick the shell the ladder is calibrated for, in blocks per tick
     * @param gravityPerTick          that shell's gravity, in blocks per tick squared
     */
    private void drawTurretInterior(HudContext context, Vec3 vehiclePos, double pivotHeight,
                                    double aimYawDeg, double aimPitchDeg, int colour,
                                    boolean opticalSight, double shellSpeedBlocksPerTick,
                                    double gravityPerTick, double muzzleHeightBlocks,
                                    double muzzleForwardBlocks, double barrelReachBlocks) {
        Minecraft minecraft = context.minecraft;
        Font font = context.font;
        GuiGraphics graphics = context.graphics;
        Vec3 origin = vehiclePos.add(0.0D, pivotHeight, 0.0D);
        // The interior camera renders through the sight's own zoom, so the projection has to use that
        // same scaled field of view: projecting with the plain option would park the reticle nearer
        // the screen centre than the barrel actually is.
        double sightFov = LittleBirdConfig.groundSightFovMultiplier();
        // The interior camera's yaw is the turret's, so the gun line is projected against that same
        // yaw; otherwise the reticle drifts off the barrel by the turret's slew lag.
        AimProjection.Screen screen = projectAimPoint(context, origin, aimYawDeg, aimPitchDeg,
                (float) aimYawDeg, sightFov);
        int centreX = screen.visible() ? (int) Math.round(screen.x()) : context.centreX();
        int centreY = screen.visible() ? (int) Math.round(screen.y()) : context.centreY();
        // Only a sighted gun gets the full optic; the plain reticles are untouched, so their picture
        // does not change. The ladder is built from the caller's own shell.
        GunSightMath.Tube tube = null;
        if (opticalSight) {
            tube = GunSightDraw.sight(graphics, font, context.width, context.height,
                    minecraft.options.fov().get(), sightFov, centreX, centreY,
                    shellSpeedBlocksPerTick, gravityPerTick);
        } else {
            HudCanvas.reticle(graphics, centreX, centreY, COAX_MIN_GAP + 2, RETICLE_ARM, colour);
        }
        HudFeedback.hitMarker(graphics, centreX, centreY);

        double range = rangeAlongGunLine(context, origin, aimYawDeg, aimPitchDeg);
        String rangeText = Math.round(range) + " m";
        // With the tube up the readouts move into the dark bands around the opening, where nothing
        // the ladder draws can reach them; without it they keep the offsets the IFV always used.
        int rangeTextY = tube == null
                ? centreY + 20
                : Math.min(context.height - 12, tube.opening().bottom() + 6);
        if (opticalSight) {
            drawFireControl(context, vehiclePos, aimYawDeg, aimPitchDeg, muzzleHeightBlocks,
                    muzzleForwardBlocks, barrelReachBlocks, shellSpeedBlocksPerTick,
                    gravityPerTick, tube, centreX, centreY, range);
        }
        graphics.drawString(font, rangeText, centreX - font.width(rangeText) / 2, rangeTextY,
                TEXT_COLOUR, false);
        double bearing = GroundReticleMath.relativeBearingDeg(aimYawDeg,
                minecraft.player.getViewYRot(context.partialTick));
        if (Math.abs(bearing) >= 1.0D) {
            String gunText = "GUN " + Math.round(bearing) + "\u00b0";
            int bearingTextY = tube == null ? centreY - 30
                    : Math.max(2, tube.opening().y() - 14);
            graphics.drawString(font, gunText, centreX - font.width(gunText) / 2, bearingTextY,
                    HudStyle.WARN, false);
        }
    }

    /**
     * Fire control: flies the shell the barrel is holding forward from the muzzle with the entity's
     * own flight model, so the sight can show where it would land and what holdover the crew owe.
     */
    private void drawFireControl(HudContext context, Vec3 vehiclePos, double aimYawDeg,
                                 double aimPitchDeg, double muzzleHeightBlocks,
                                 double muzzleForwardBlocks, double barrelReachBlocks,
                                 double shellSpeedBlocksPerTick, double gravityPerTick,
                                 GunSightMath.Tube tube, int centreX, int centreY,
                                 double aimRangeBlocks) {
        Minecraft minecraft = context.minecraft;
        if (minecraft.level == null) {
            return;
        }
        Font font = context.font;
        GuiGraphics graphics = context.graphics;
        LittleBirdBallistics.Vector3 aim = MainGun.aimDirection(aimYawDeg, aimPitchDeg);
        // Same muzzle arithmetic the entity uses: the gun's own trunnion plus its barrel reach.
        Vec3 turretForward = Vec3.directionFromRotation(0.0F, (float) aimYawDeg);
        LittleBirdBallistics.Vector3 muzzle = MainGun.muzzleOrigin(
                new LittleBirdBallistics.Vector3(vehiclePos.x, vehiclePos.y, vehiclePos.z),
                new LittleBirdBallistics.Vector3(turretForward.x, turretForward.y,
                        turretForward.z),
                new LittleBirdBallistics.Vector3(0.0D, 1.0D, 0.0D),
                muzzleHeightBlocks, muzzleForwardBlocks, barrelReachBlocks, aimYawDeg,
                aimPitchDeg);
        double muzzleX = muzzle.x();
        double muzzleY = muzzle.y();
        double muzzleZ = muzzle.z();
        MainGunFireControl.Ground ground = (x, z) -> minecraft.level.getHeight(
                Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
        MainGunFireControl.Impact impact = MainGunFireControl.predict(muzzleX, muzzleY, muzzleZ,
                aim.x(), aim.y(), aim.z(), shellSpeedBlocksPerTick, gravityPerTick,
                MainGunShellBallistics.LIFETIME_TICKS, ground);

        double pixelsPerMil = GunSightMath.pixelsPerMil(context.height,
                minecraft.options.fov().get(), LittleBirdConfig.groundSightFovMultiplier());
        String label;
        String readout;
        if (impact.reached()) {
            double[] offset = MainGunFireControl.angularOffsetMils(muzzleX, muzzleY, muzzleZ,
                    aimYawDeg, aimPitchDeg, impact.x(), impact.y(), impact.z());
            label = "IMP " + Math.round(impact.rangeBlocks()) + " m";
            GunSightDraw.fireControl(graphics, font, tube, centreX, centreY, pixelsPerMil,
                    offset[0], offset[1], label);
            String hold = aimRangeBlocks > 1.0D
                    ? String.format(Locale.ROOT, "  HOLD +%.0f mil",
                            GunSightMath.dropMils(aimRangeBlocks, shellSpeedBlocksPerTick,
                                    gravityPerTick))
                    : "";
            readout = String.format(Locale.ROOT, "FCS  IMP %d m  TOF %.1fs%s",
                    Math.round(impact.rangeBlocks()), impact.flightTicks() / 20.0D, hold);
        } else {
            readout = "FCS  OUT OF RANGE";
        }
        int readoutY = tube == null || tube.opening().empty()
                ? centreY - 44
                : Math.max(2, tube.opening().y() - 26);
        graphics.drawString(font, readout, centreX - font.width(readout) / 2, readoutY,
                GunSightDraw.IMPACT_COLOUR, false);
    }

    /**
     * Where a turret line lands on the screen, or an invisible marker when it is off camera.
     *
     * @param fovMultiplier the zoom the camera is rendering with, 1.0 for an unscaled view, so the
     *                      projection lands the reticle where the magnified lens really puts the gun
     */
    private AimProjection.Screen projectAimPoint(HudContext context, Vec3 origin, double aimYawDeg,
                                                 double aimPitchDeg, float cameraYawDeg,
                                                 double fovMultiplier) {
        Minecraft minecraft = context.minecraft;
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        LittleBirdBallistics.Vector3 aim = MainGun.aimDirection(aimYawDeg, aimPitchDeg);
        Vec3 probe = origin.add(aim.x() * AIM_PROBE_BLOCKS, aim.y() * AIM_PROBE_BLOCKS,
                aim.z() * AIM_PROBE_BLOCKS);
        float verticalFov = (float) (minecraft.options.fov().get()
                * GunSightMath.clampMultiplier(fovMultiplier));
        double horizontalFov = GroundReticleMath.horizontalFovDeg(verticalFov, context.width,
                context.height);
        // Project against the camera that is actually on screen. The tank's sight camera is now
        // boresighted to the barrel in GroundCamera, so the raw mouse pitch is no longer what the
        // player sees; using it here would put the mark somewhere the lens does not show.
        float cameraYaw = camera.getYRot();
        float cameraPitch = camera.getXRot();
        double bearing = GroundReticleMath.relativeBearingDeg(aimYawDeg, cameraYaw);
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(probe.x, probe.y, probe.z),
                new AimProjection.Vector(eye.x, eye.y, eye.z),
                cameraYaw,
                cameraPitch,
                0.0F, verticalFov, context.width, context.height);
        if (!screen.visible() || !GroundReticleMath.withinFov(bearing, horizontalFov)) {
            return new AimProjection.Screen(0.0D, 0.0D, false);
        }
        return screen;
    }

    /** Distance from the muzzle to the first collider on the gun line, in blocks. */
    private double rangeAlongGunLine(HudContext context, Vec3 origin, double aimYawDeg,
                                     double aimPitchDeg) {
        Minecraft minecraft = context.minecraft;
        if (minecraft.level == null) {
            return 0.0D;
        }
        LittleBirdBallistics.Vector3 aim = MainGun.aimDirection(aimYawDeg, aimPitchDeg);
        Vec3 direction = new Vec3(aim.x(), aim.y(), aim.z());
        Vec3 far = origin.add(direction.scale(GroundWeapons.RANGE_BLOCKS));
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                origin, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                minecraft.player));
        return hit.getType() == HitResult.Type.MISS
                ? GroundWeapons.RANGE_BLOCKS : origin.distanceTo(hit.getLocation());
    }

    // ------------------------------------------------------------------
    // Alerts and hint
    // ------------------------------------------------------------------

    /** Low hull and "out of rounds while reloading", in the shared alert ribbon. */
    private void drawAlerts(HudContext context) {
        float fraction = 1.0F;
        boolean empty = false;
        boolean reloading = false;
        var vehicle = context.minecraft.player.getVehicle();
        if (vehicle instanceof MainBattleTankEntity tank) {
            fraction = tank.getHealth() / MainBattleTankEntity.MAX_HEALTH;
            empty = tank.getReadyRounds() <= 0;
            reloading = tank.getMainGunReloadTicks() > 0;
        } else if (vehicle instanceof InfantryFightingVehicleEntity ifv) {
            fraction = ifv.getHealth() / InfantryFightingVehicleEntity.MAX_HEALTH;
            empty = ifv.getAutocannonAmmo() <= 0;
        } else if (vehicle instanceof BradleyEntity bradley) {
            fraction = bradley.getHealth() / BradleyEntity.MAX_HEALTH;
            empty = bradley.getAutocannonReady() <= 0;
        } else {
            return;
        }
        List<Component> alerts = new ArrayList<>();
        if (fraction < LOW_HULL_ALERT_FRACTION) {
            alerts.add(Component.translatable("alert.battlefield.low_health"));
        }
        if (empty) {
            alerts.add(reloading
                    ? Component.translatable("hud.battlefield.reloading")
                    : Component.translatable("alert.battlefield.no_ammo"));
        }
        HudCanvas.alerts(context.graphics, context.font, context.width, alerts);
    }

    /** One bottom hint line, built from this mod's real bindings so a rebind never lies. */
    private void drawHint(HudContext context, boolean weaponSwitch, boolean shellSwitch) {
        List<String> parts = new ArrayList<>();
        parts.add(keyName(LittleBirdKeyMappings.COCKPIT) + " sight");
        if (weaponSwitch) {
            parts.add(keyName(LittleBirdKeyMappings.WEAPON_SWITCH) + " weapon");
        }
        if (shellSwitch) {
            parts.add(keyName(LittleBirdKeyMappings.SHELL_SWITCH) + " shell");
        }
        parts.add("LMB fire");
        parts.add(keyName(LittleBirdKeyMappings.RELOAD) + " reload");
        parts.add(keyName(LittleBirdKeyMappings.EXIT) + " exit");
        HudCanvas.hint(context.graphics, context.font, context.width, context.height,
                String.join("   ", parts));
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    private static int hudScale() {
        return Math.max(1, LittleBirdConfig.hudScale());
    }

    private static String tr(String translationKey) {
        return Component.translatable(translationKey).getString();
    }

    private static String keyName(KeyMapping mapping) {
        return mapping.getTranslatedKeyMessage().getString();
    }

    /** One weapon row: glyph, ammo/cooldown badge, key legend and selection state. */
    private record Slot(SlotIcon icon, String badge, String legend, boolean selected,
                        double cooldown) {
    }

    /** One tabular readout row in the weapon panel. */
    private record Readout(String label, String value, int colour) {
    }

    /** Procedural slot glyphs; no textures, so they work at any GUI scale. */
    private enum SlotIcon {
        /** Main gun: an elongated round with a pointed tip. */
        SHELL,
        /** Machine gun: a receiver with a short belt hanging under it. */
        BELT,
        /** Discharger: three rising chevrons. */
        SMOKE
    }
}
