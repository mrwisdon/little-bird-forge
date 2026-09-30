package io.github.littlebird.client;

import io.github.littlebird.LittleBirdConfig;
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
import io.github.littlebird.entity.weapon.WeaponSelection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/**
 * Ground vehicle HUD for both hulls: weapon reticles, the vehicle condition panel (bottom left) and
 * the weapon slot bar with its ammunition readout (bottom right, laid out after the reference
 * screenshot).
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
    /** Bradley: three driver weapon slots (25mm / TOW / smoke), so one row more. */
    private static final int BRADLEY_ROWS = 3;

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
        } else if (minecraft.player.getVehicle() instanceof BradleyEntity bradley) {
            renderBradley(graphics, font, minecraft, bradley, partialTick, width, height, hudScale);
        } else {
            return;
        }
        // The same target readout the aircraft HUDs draw, so a ground crew sees the damage it lands.
        GunshipHudDraw.enemyHealthBar(graphics, font, width, height);
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
        if (tank.isInteriorView()) {
            // Turret interior: the fire control is the gun line, wherever the barrel points.
            drawTurretInterior(graphics, font, minecraft, tank.position(),
                    MainBattleTankGeometry.turretPivotHeightBlocks(),
                    tank.renderTurretYawDeg(partialTick), tank.renderTurretPitchDeg(partialTick),
                    partialTick, width, height, MAIN_GUN_COLOUR, true);
        } else if (GroundSeat.isTankDriver(seat)) {
            GunshipHudDraw.mainGunCrosshair(graphics, width / 2, height / 2, MAIN_GUN_COLOUR);
        } else if (GroundSeat.isTankGunner(seat)) {
            Vec3 origin = tank.position().add(0.0D,
                    MainBattleTankGeometry.turretPivotHeightBlocks(), 0.0D);
            drawProjectedReticle(graphics, minecraft, origin,
                    tank.renderTurretYawDeg(partialTick), tank.renderTurretPitchDeg(partialTick),
                    partialTick, width, height,
                    COAX_COLOUR,
                    GroundReticleMath.gapPixels(COAX_SPREAD_PLACEHOLDER, COAX_MIN_GAP, COAX_MAX_GAP));
        } else {
            return;
        }
        GroundHudLayout.Panels panels =
                GroundHudLayout.compute(width, height, hudScale, TANK_ROWS);
        drawCondition(graphics, font, tank.getHealth(), MainBattleTankEntity.MAX_HEALTH, panels);
        GroundHudLayout.Rect bar = panels.weapons();
        int selected = tank.getSelectedWeapon();
        int slotHeight = panels.rowHeight();
        // Slot legends use this mod's real bindings: the driver cycles main gun, coaxial gun and
        // smoke with the weapon key. The two machine guns have separate belts, so the belt row
        // shows the one this seat actually fires: the cupola for seat two, the coax for the driver.
        String weaponKey = LittleBirdKeyMappings.WEAPON_SWITCH.getTranslatedKeyMessage().getString();
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 0), bar.y(),
                GroundHudLayout.slotWidth(0), slotHeight, GunshipHudDraw.SlotIcon.SHELL,
                Integer.toString(tank.getReadyRounds()), weaponKey,
                selected == WeaponSelection.MAIN_GUN);
        int beltAmmo = GroundSeat.isTankGunner(seat) ? tank.getCupolaAmmo() : tank.getCoaxAmmo();
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 1), bar.y(),
                GroundHudLayout.slotWidth(1), slotHeight, GunshipHudDraw.SlotIcon.BELT,
                Integer.toString(beltAmmo), weaponKey,
                selected == WeaponSelection.COAX);
        int smokeCooldown = GroundHudLayout.cooldownSeconds(tank.getSmokeCooldownTicks());
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 2), bar.y(),
                GroundHudLayout.slotWidth(2), slotHeight, GunshipHudDraw.SlotIcon.SMOKE,
                smokeCooldown > 0 ? smokeCooldown + "s"
                        : Integer.toString(tank.getSmokeCharges()),
                weaponKey, selected == WeaponSelection.SMOKE);

        MainGunAmmoType shell = MainGunAmmoType.byOrdinal(tank.getShellTypeOrdinal());
        GroundHudLayout.Rect readout =
                GroundHudLayout.readout(width, height, hudScale, TANK_ROWS);
        String shellName = Component.translatable("hud.battlefield.weapon.main_gun").getString()
                + " " + Component.translatable(shell.translationKey()).getString()
                + "  " + LittleBirdKeyMappings.SHELL_SWITCH.getTranslatedKeyMessage().getString();
        // The ring is the reload cooldown; the reserve dims while the resupply cooldown runs.
        int reloadTotal = LittleBirdConfig.tankMainGunMagazine().reloadTicks();
        int reloadLeft = tank.getMainGunReloadTicks();
        float reloadFraction = reloadTotal <= 0 || reloadLeft <= 0
                ? 1.0F : 1.0F - Math.min(1.0F, reloadLeft / (float) reloadTotal);
        GunshipHudDraw.ammoCounter(graphics, font, readout.x(), readout.y(), readout.width(),
                shellName, String.format("%03d", tank.getReadyRounds()),
                Integer.toString(tank.getReserveRounds()), reloadFraction,
                tank.getMainGunResupplyTicks() > 0);
        // Bottom aligned inside the readout box: the chip is one line tall, so this leaves the
        // six pixel gap between the readout and the weapon bar empty.
        GunshipHudDraw.vehicleChip(graphics, font, readout.x(),
                readout.y() + GroundHudLayout.READOUT_HEIGHT - 10, readout.width(),
                Component.translatable("hud.battlefield.vehicle.tank").getString(),
                tank.getHealth() / MainBattleTankEntity.MAX_HEALTH,
                Integer.toString(Math.round(tank.getHealth())));
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
        if (ifv.isInteriorView()) {
            // Turret interior: same fire control as the tank, on the IFV's gun line.
            drawTurretInterior(graphics, font, minecraft, ifv.position(),
                    InfantryFightingVehicleGeometry.turretPivotHeightBlocks(),
                    ifv.renderTurretYawDeg(partialTick), ifv.renderTurretPitchDeg(partialTick),
                    partialTick, width, height,
                    COAX_COLOUR, false);
        } else if (GroundSeat.isIfvDriver(seat)) {
            // The 30mm is a rapid weapon: a tight reticle rather than the tank's heavy cross.
            int gap = GroundReticleMath.gapPixels(AUTO_SPREAD_PLACEHOLDER, AUTO_MIN_GAP,
                    AUTO_MAX_GAP);
            GunshipHudDraw.coaxCrosshair(graphics, width / 2, height / 2, gap, COAX_COLOUR);
        } else if (GroundSeat.isIfvGunner(seat)) {
            Vec3 origin = ifv.position().add(0.0D,
                    InfantryFightingVehicleGeometry.turretPivotHeightBlocks(), 0.0D);
            drawProjectedReticle(graphics, minecraft, origin,
                    ifv.renderTurretYawDeg(partialTick), ifv.renderTurretPitchDeg(partialTick),
                    partialTick, width, height,
                    MAIN_GUN_COLOUR, COAX_MIN_GAP + 3);
        } else {
            return;
        }
        GroundHudLayout.Panels panels = GroundHudLayout.compute(width, height, hudScale, IFV_ROWS);
        drawCondition(graphics, font, ifv.getHealth(), InfantryFightingVehicleEntity.MAX_HEALTH,
                panels);
        GroundHudLayout.Rect bar = panels.weapons();
        int slotHeight = panels.rowHeight();
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 0), bar.y(),
                GroundHudLayout.slotWidth(0), slotHeight, GunshipHudDraw.SlotIcon.BELT,
                Integer.toString(ifv.getAutocannonAmmo()),
                Component.translatable("hud.battlefield.slot.driver").getString(), true);
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 1), bar.y(),
                GroundHudLayout.slotWidth(1), slotHeight, GunshipHudDraw.SlotIcon.SHELL,
                Integer.toString(ifv.getAtgmReady()),
                Component.translatable("hud.battlefield.slot.gunner").getString(), false);

        GroundHudLayout.Rect readout =
                GroundHudLayout.readout(width, height, hudScale, IFV_ROWS);
        GunshipHudDraw.ammoCounter(graphics, font, readout.x(), readout.y(), readout.width(),
                Component.translatable("hud.battlefield.weapon.ifv_autocannon").getString(),
                String.format("%03d", ifv.getAutocannonAmmo()),
                Integer.toString(ifv.getAtgmReserve()), 1.0F, false);
        GunshipHudDraw.vehicleChip(graphics, font, readout.x(),
                readout.y() + GroundHudLayout.READOUT_HEIGHT - 10, readout.width(),
                Component.translatable("hud.battlefield.vehicle.ifv").getString(),
                ifv.getHealth() / InfantryFightingVehicleEntity.MAX_HEALTH,
                Integer.toString(Math.round(ifv.getHealth())));
    }

    // ------------------------------------------------------------------
    // M2A2 Bradley
    // ------------------------------------------------------------------

    /**
     * The Bradley's fire control. Seat 0 works the turret and cycles three weapons with the weapon
     * key, so its reticle changes with the slot: a tight 25mm cross, a projected TOW line (the
     * missile flies to the spot the sight ends on) and a smoke marker. Seat 1's roof gun follows
     * its own gunner's view, so its cross is pinned to the centre like a driver's.
     */
    private void renderBradley(GuiGraphics graphics, Font font, Minecraft minecraft,
                               BradleyEntity bradley, float partialTick, int width,
                               int height, int hudScale) {
        int seat = bradley.seatOf(minecraft.player);
        if (seat < 0) {
            return;
        }
        if (bradley.isInteriorView()) {
            drawTurretInterior(graphics, font, minecraft, bradley.position(),
                    BradleyGeometry.turretPivotHeightBlocks(),
                    bradley.renderTurretYawDeg(partialTick),
                    bradley.renderTurretPitchDeg(partialTick),
                    partialTick, width, height, COAX_COLOUR, false);
        } else if (GroundSeat.isBradleyDriver(seat)) {
            if (bradley.getWeapon() == BradleyEntity.WEAPON_TOW) {
                Vec3 origin = bradley.position().add(0.0D,
                        BradleyGeometry.turretPivotHeightBlocks(), 0.0D);
                drawProjectedReticle(graphics, minecraft, origin,
                        bradley.renderTurretYawDeg(partialTick),
                        bradley.renderTurretPitchDeg(partialTick),
                        partialTick, width, height, MAIN_GUN_COLOUR, COAX_MIN_GAP + 2);
            } else if (bradley.getWeapon() == BradleyEntity.WEAPON_SMOKE) {
                GunshipHudDraw.coaxCrosshair(graphics, width / 2, height / 2,
                        AUTO_MIN_GAP + 6, SMOKE_COLOUR);
            } else {
                int gap = GroundReticleMath.gapPixels(AUTO_SPREAD_PLACEHOLDER, AUTO_MIN_GAP,
                        AUTO_MAX_GAP);
                GunshipHudDraw.coaxCrosshair(graphics, width / 2, height / 2, gap, COAX_COLOUR);
            }
        } else if (GroundSeat.isBradleyGunner(seat)) {
            int gap = GroundReticleMath.gapPixels(COAX_SPREAD_PLACEHOLDER, COAX_MIN_GAP,
                    COAX_MAX_GAP);
            GunshipHudDraw.coaxCrosshair(graphics, width / 2, height / 2, gap, MAIN_GUN_COLOUR);
        } else {
            return;
        }
        GroundHudLayout.Panels panels =
                GroundHudLayout.compute(width, height, hudScale, BRADLEY_ROWS);
        drawCondition(graphics, font, bradley.getHealth(), BradleyEntity.MAX_HEALTH, panels);
        GroundHudLayout.Rect bar = panels.weapons();
        int slotHeight = panels.rowHeight();
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 0), bar.y(),
                GroundHudLayout.slotWidth(0), slotHeight, GunshipHudDraw.SlotIcon.BELT,
                Integer.toString(bradley.getAutocannonReady()),
                Component.translatable("hud.battlefield.slot.driver").getString(),
                bradley.getWeapon() == BradleyEntity.WEAPON_AUTOCANNON);
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 1), bar.y(),
                GroundHudLayout.slotWidth(1), slotHeight, GunshipHudDraw.SlotIcon.SHELL,
                Integer.toString(bradley.getTowReady()),
                Component.translatable("hud.battlefield.slot.gunner").getString(),
                bradley.getWeapon() == BradleyEntity.WEAPON_TOW);
        GunshipHudDraw.iconSlot(graphics, font, GroundHudLayout.slotX(panels, 2), bar.y(),
                GroundHudLayout.slotWidth(2), slotHeight, GunshipHudDraw.SlotIcon.SMOKE,
                Integer.toString(bradley.getSmokeCharges()),
                Component.translatable("hud.battlefield.slot.smoke").getString(),
                bradley.getWeapon() == BradleyEntity.WEAPON_SMOKE);

        GroundHudLayout.Rect readout =
                GroundHudLayout.readout(width, height, hudScale, BRADLEY_ROWS);
        String weaponName = switch (bradley.getWeapon()) {
            case BradleyEntity.WEAPON_TOW -> Component.translatable(
                    "hud.battlefield.weapon.bradley_tow").getString();
            case BradleyEntity.WEAPON_SMOKE -> Component.translatable(
                    "hud.battlefield.weapon.bradley_smoke").getString();
            default -> Component.translatable(
                    "hud.battlefield.weapon.bradley_autocannon").getString();
        };
        GunshipHudDraw.ammoCounter(graphics, font, readout.x(), readout.y(), readout.width(),
                weaponName, String.format("%03d", bradley.getAutocannonReady()),
                Integer.toString(bradley.getAutocannonReserve()), 1.0F, false);
        GunshipHudDraw.vehicleChip(graphics, font, readout.x(),
                readout.y() + GroundHudLayout.READOUT_HEIGHT - 10, readout.width(),
                Component.translatable("hud.battlefield.vehicle.bradley").getString(),
                bradley.getHealth() / BradleyEntity.MAX_HEALTH,
                Integer.toString(Math.round(bradley.getHealth())));
    }

    // ------------------------------------------------------------------
    // Shared drawing
    // ------------------------------------------------------------------

    /** Projects a turret line onto the screen and draws the reticle where it actually points. */
    private void drawProjectedReticle(GuiGraphics graphics, Minecraft minecraft, Vec3 origin,
                                      double aimYawDeg, double aimPitchDeg, float partialTick,
                                      int width, int height, int colour, int gap) {
        // Exterior reticles keep the player's view yaw: the camera is not slaved to anything here.
        AimProjection.Screen screen = projectAimPoint(minecraft, origin, aimYawDeg, aimPitchDeg,
                minecraft.player.getViewYRot(partialTick), partialTick, width, height);
        if (screen.visible()) {
            GunshipHudDraw.coaxCrosshair(graphics, (int) Math.round(screen.x()),
                    (int) Math.round(screen.y()), gap, colour);
        }
    }

    /**
     * Turret interior fire control: the crosshair sits on the gun line rather than in the middle of
     * the screen, with the range to the first collider and the hit marker. Drawn only while the
     * interior camera is up; the exterior camera keeps the plain reticles and shows none of this.
     */
    private void drawTurretInterior(GuiGraphics graphics, Font font, Minecraft minecraft,
                                    Vec3 vehiclePos, double pivotHeight, double aimYawDeg,
                                    double aimPitchDeg, float partialTick, int width, int height,
                                    int colour, boolean heavyGun) {
        Vec3 origin = vehiclePos.add(0.0D, pivotHeight, 0.0D);
        // The interior camera's yaw is the turret's, so the gun line is projected against that same
        // yaw; otherwise the reticle drifts off the barrel by the turret's slew lag.
        AimProjection.Screen screen = projectAimPoint(minecraft, origin, aimYawDeg, aimPitchDeg,
                (float) aimYawDeg, partialTick, width, height);
        int centreX = screen.visible() ? (int) Math.round(screen.x()) : width / 2;
        int centreY = screen.visible() ? (int) Math.round(screen.y()) : height / 2;
        if (heavyGun) {
            GunshipHudDraw.mainGunCrosshair(graphics, centreX, centreY, colour);
        } else {
            GunshipHudDraw.coaxCrosshair(graphics, centreX, centreY, COAX_MIN_GAP + 2, colour);
        }
        GunshipHudDraw.hitMarker(graphics, centreX, centreY);

        double range = rangeAlongGunLine(minecraft, origin, aimYawDeg, aimPitchDeg);
        String rangeText = Math.round(range) + " m";
        graphics.drawString(font, rangeText, centreX - font.width(rangeText) / 2, centreY + 20,
                TEXT_COLOUR, false);
        double bearing = GroundReticleMath.relativeBearingDeg(aimYawDeg,
                minecraft.player.getViewYRot(partialTick));
        if (Math.abs(bearing) >= 1.0D) {
            String gunText = "GUN " + Math.round(bearing) + "\u00b0";
            graphics.drawString(font, gunText, centreX - font.width(gunText) / 2, centreY - 30,
                    GunshipHudDraw.WARNING, false);
        }
    }

    /** Where a turret line lands on the screen, or an invisible marker when it is off camera. */
    private AimProjection.Screen projectAimPoint(Minecraft minecraft, Vec3 origin, double aimYawDeg,
                                                 double aimPitchDeg, float cameraYawDeg,
                                                 float partialTick, int width, int height) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 eye = camera.getPosition();
        LittleBirdBallistics.Vector3 aim = MainGun.aimDirection(aimYawDeg, aimPitchDeg);
        Vec3 probe = origin.add(aim.x() * AIM_PROBE_BLOCKS, aim.y() * AIM_PROBE_BLOCKS,
                aim.z() * AIM_PROBE_BLOCKS);
        float verticalFov = minecraft.options.fov().get();
        double horizontalFov = GroundReticleMath.horizontalFovDeg(verticalFov, width, height);
        double bearing = GroundReticleMath.relativeBearingDeg(aimYawDeg, cameraYawDeg);
        AimProjection.Screen screen = AimProjection.project(
                new AimProjection.Vector(probe.x, probe.y, probe.z),
                new AimProjection.Vector(eye.x, eye.y, eye.z),
                cameraYawDeg,
                minecraft.player.getViewXRot(partialTick),
                0.0F, verticalFov, width, height);
        if (!screen.visible() || !GroundReticleMath.withinFov(bearing, horizontalFov)) {
            return new AimProjection.Screen(0.0D, 0.0D, false);
        }
        return screen;
    }

    /** Distance from the muzzle to the first collider on the gun line, in blocks. */
    private double rangeAlongGunLine(Minecraft minecraft, Vec3 origin, double aimYawDeg,
                                     double aimPitchDeg) {
        if (minecraft.level == null) {
            return 0.0D;
        }
        LittleBirdBallistics.Vector3 aim = MainGun.aimDirection(aimYawDeg, aimPitchDeg);
        Vec3 direction = new Vec3(aim.x(), aim.y(), aim.z());
        Vec3 far = origin.add(direction.scale(GroundWeapons.RANGE_BLOCKS));
        BlockHitResult hit = minecraft.level.clip(new ClipContext(
                origin, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, minecraft.player));
        return hit.getType() == HitResult.Type.MISS
                ? GroundWeapons.RANGE_BLOCKS : origin.distanceTo(hit.getLocation());
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

}
