package io.github.littlebird.client.hud;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.ApacheAimRenderer;
import io.github.littlebird.client.LittleBirdAimRenderer;
import io.github.littlebird.client.LittleBirdCrosshairMath;
import io.github.littlebird.client.LittleBirdHudLayout;
import io.github.littlebird.client.LittleBirdHudMath;
import io.github.littlebird.entity.ApacheEntity;
import io.github.littlebird.entity.B2Entity;
import io.github.littlebird.entity.JetEntity;
import io.github.littlebird.entity.LittleBirdEntity;
import io.github.littlebird.entity.seat.ApacheSeat;
import io.github.littlebird.entity.weapon.ApachePods;
import io.github.littlebird.entity.weapon.JetCannon;
import io.github.littlebird.entity.weapon.LittleBirdGunCycle;
import io.github.littlebird.entity.weapon.LittleBirdPodTubes;
import io.github.littlebird.entity.weapon.RocketPodState;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Little Bird, Apache and jet HUD in one view: a compact tabular flight cluster under the centre
 * cross, a weapon panel in the bottom right whose rows follow the airframe and the seat, the
 * projected reticles from the aim renderers, the shared hit marker and enemy readout, the jet's
 * seeker cue and a one line control hint.
 *
 * <p>All numbers are synched entity state; nothing here drives the aircraft. The reticle toggles
 * stay honouring {@link LittleBirdConfig}: the fixed centre cross follows
 * {@code showCenterCrosshair}, and the Little Bird's projected sights follow
 * {@code reticleFollowsWeapon} / {@code showGunReticle} / {@code showRocketReticle}.
 */
public final class HudAir implements HudView {
    private static final int MARGIN = 10;
    private static final int PANEL_WIDTH = 172;

    // flight cluster: heading, pitch, roll, speed, altitude as tabular rows
    private static final int CLUSTER_WIDTH = 118;
    private static final int CLUSTER_HEIGHT = 69;
    /** Pixels between the screen centre and the top of the flight cluster. */
    private static final int CLUSTER_GAP_Y = 34;

    // fixed centre cross (the old GunshipHudDraw.crosshair geometry)
    private static final int CROSSHAIR_GAP = 3;
    private static final int CROSSHAIR_ARM = 6;

    // chain gun turret sight (the old ApacheHud geometry)
    private static final int TURRET_GAP = 4;
    private static final int TURRET_ARM = 8;

    // jet seeker lock cue
    private static final int LOCK_RADIUS = 14;
    private static final int LOCK_TICK = 6;

    // reference rocket sight shared by both gunships
    private static final int ROCKET_BAR_OFFSET = 10;
    private static final int ROCKET_BAR_WIDTH = 2;
    private static final int ROCKET_BAR_HALF_HEIGHT = 6;
    private static final int ROCKET_BAR_GAP = 2;
    private static final int ROCKET_DOT_SIZE = 2;
    private static final int ROCKET_DOT_ROW_SPACING = 5;
    private static final int ROCKET_DOT_COLUMN_SPACING = 5;
    private static final int ROCKET_CIRCLE_RADIUS = 4;

    private static final int LITTLE_BIRD_GUN_PANEL_HEIGHT = 66;
    private static final int LITTLE_BIRD_ROCKET_PANEL_HEIGHT = 76;
    private static final int APACHE_PILOT_PANEL_HEIGHT = 88;
    private static final int APACHE_GUNNER_PANEL_HEIGHT = 76;
    private static final int JET_PANEL_HEIGHT = 120;

    private static final String HINT_LITTLE_BIRD =
            "1 GUN   2 ROCKETS   Z SALVO   R RELOAD   C VIEW";
    private static final String HINT_APACHE_PILOT =
            "1 ROCKETS   2 MISSILES   R RELOAD   H SEAT   C VIEW";
    private static final String HINT_APACHE_GUNNER = "H SEAT   C VIEW";
    private static final String HINT_JET = "1-5 STORES   F FLARES   V VTOL   C VIEW";

    @Override
    public boolean appliesTo(HudContext context) {
        var vehicle = context.minecraft.player.getVehicle();
        if (vehicle instanceof B2Entity) {
            // The B-2 flies the bomber stack; the fighter panels are not shown in it.
            return false;
        }
        return vehicle instanceof LittleBirdEntity
                || vehicle instanceof ApacheEntity
                || vehicle instanceof JetEntity;
    }

    @Override
    public void render(HudContext context) {
        var vehicle = context.minecraft.player.getVehicle();
        if (vehicle instanceof LittleBirdEntity helicopter) {
            renderLittleBird(context, helicopter);
        } else if (vehicle instanceof ApacheEntity apache) {
            renderApache(context, apache);
        } else if (vehicle instanceof JetEntity jet) {
            renderJet(context, jet);
        }
    }

    // ------------------------------------------------------------------
    // Airframes
    // ------------------------------------------------------------------

    private void renderLittleBird(HudContext context, LittleBirdEntity helicopter) {
        // The pilot gets the instruments, the weapons and the sights; a passenger keeps the
        // flight cluster alone, because the reticles are not theirs to use.
        boolean pilot = helicopter.isPilot(context.minecraft.player);
        HudFeedback.hitMarker(context.graphics, context.centreX(), context.centreY());
        drawCluster(context, helicopter.getYRot(), helicopter.getXRot(),
                helicopter.getRenderRollDeg(), horizontalSpeed(helicopter), helicopter.getY());
        if (pilot) {
            drawCentreCross(context);
            drawLittleBirdSights(context, helicopter);
            drawLittleBirdPanel(context, helicopter);
            HudCanvas.hint(context.graphics, context.font, context.width, context.height,
                    HINT_LITTLE_BIRD);
        }
        HudFeedback.enemyBar(context.graphics, context.font, context.width, context.height);
        HudCanvas.alerts(context.graphics, context.font, context.width,
                littleBirdAlerts(helicopter));
    }

    private void renderApache(HudContext context, ApacheEntity apache) {
        boolean pilot = apache.seatOf(context.minecraft.player) == ApacheSeat.PILOT;
        HudFeedback.hitMarker(context.graphics, context.centreX(), context.centreY());
        drawCentreCross(context);
        drawCluster(context, apache.getYRot(), apache.getXRot(), apache.getRenderRollDeg(),
                horizontalSpeed(apache), apache.getY());
        if (pilot) {
            // The rocket/TOW sight line is the nose, so the plane's centre cross is the sight;
            // the projected marker only rides the 70mm's predicted impact.
            drawApacheRocketSight(context, apache);
            drawApachePilotPanel(context, apache);
            HudCanvas.hint(context.graphics, context.font, context.width, context.height,
                    HINT_APACHE_PILOT);
        } else {
            // The gunner aims with the turret, not with the nose.
            drawTurretSight(context);
            drawApacheGunnerPanel(context, apache);
            HudCanvas.hint(context.graphics, context.font, context.width, context.height,
                    HINT_APACHE_GUNNER);
        }
        HudFeedback.enemyBar(context.graphics, context.font, context.width, context.height);
        HudCanvas.alerts(context.graphics, context.font, context.width,
                apacheAlerts(apache, pilot));
    }

    private void renderJet(HudContext context, JetEntity jet) {
        HudFeedback.hitMarker(context.graphics, context.centreX(), context.centreY());
        drawCentreCross(context);
        drawCluster(context, jet.getYRot(), jet.getXRot(), jet.getRenderRollDeg(),
                horizontalSpeed(jet), jet.getY());
        drawJetLock(context, jet);
        drawJetPanel(context, jet);
        HudFeedback.enemyBar(context.graphics, context.font, context.width, context.height);
        HudCanvas.alerts(context.graphics, context.font, context.width, jetAlerts(jet));
        HudCanvas.hint(context.graphics, context.font, context.width, context.height, HINT_JET);
    }

    private static double horizontalSpeed(net.minecraft.world.entity.Entity vehicle) {
        var motion = vehicle.getDeltaMovement();
        return Math.sqrt(motion.x * motion.x + motion.z * motion.z);
    }

    // ------------------------------------------------------------------
    // Centre cluster: heading, pitch, roll, speed, altitude
    // ------------------------------------------------------------------

    private void drawCluster(HudContext context, float yawDeg, float pitchDeg, float rollDeg,
                             double horizontalSpeed, double altitude) {
        // Bottom-left block: the scope first, the flight instruments to its right, both bottom
        // anchored so the weapon panel on the other corner has room to breathe.
        int radarSize = Math.min(140, Math.max(110, context.height - 220));
        int radarX = 10;
        int radarY = context.height - 34 - radarSize;
        HudRadar.Spec spec = new HudRadar.Spec(AirRadar.RADIUS, yawDeg, AirRadar.contacts(),
                false, 0.0D, 0.0D, false, 0.0D, 0.0D);
        HudRadar.draw(context.graphics, context.font, radarX, radarY, radarSize, spec);
        HudCanvas.line(context.graphics, context.font, radarX, radarY - 10,
                "RADAR " + Math.round(AirRadar.RADIUS), HudStyle.ACCENT);

        int x = radarX + radarSize + 8;
        int y = radarY + radarSize - CLUSTER_HEIGHT;
        HudCanvas.panel(context.graphics, context.font, "FLIGHT", null,
                x, y, CLUSTER_WIDTH, CLUSTER_HEIGHT, HudStyle.EDGE);
        int left = x + HudStyle.PAD;
        int right = x + CLUSTER_WIDTH - HudStyle.PAD;
        int rowY = y + HudStyle.HEADER + HudStyle.PAD;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "HDG",
                LittleBirdHudMath.headingLabel(yawDeg), HudStyle.ACCENT);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "PIT",
                signedDegrees(pitchDeg), HudStyle.TEXT);
        rowY += HudStyle.ROW;
        int rollColour = Math.abs(rollDeg) > LittleBirdHudMath.BANK_ARC_LIMIT_DEGREES
                ? HudStyle.WARN : HudStyle.TEXT;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "ROL",
                signedDegrees(rollDeg), rollColour);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "KPH",
                Integer.toString(LittleBirdHudLayout.toKilometresPerHour(horizontalSpeed)),
                HudStyle.ACCENT);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "ALT",
                Integer.toString((int) Math.round(altitude)), HudStyle.ACCENT);
    }

    // ------------------------------------------------------------------
    // Sights
    // ------------------------------------------------------------------

    /** Fixed centre cross, shared by every aircraft and gated by the config toggle. */
    private void drawCentreCross(HudContext context) {
        if (!LittleBirdConfig.showCenterCrosshair()) {
            return;
        }
        HudCanvas.reticle(context.graphics, context.centreX(), context.centreY(),
                CROSSHAIR_GAP, CROSSHAIR_ARM, HudStyle.ACCENT);
    }

    /** Little Bird: gun cross with its dispersion gap, and the reference rocket sight. */
    private void drawLittleBirdSights(HudContext context, LittleBirdEntity helicopter) {
        boolean follows = LittleBirdConfig.reticleFollowsWeapon();
        int weapon = helicopter.getWeapon();
        if (LittleBirdCrosshairMath.showsGunReticle(
                follows, weapon, LittleBirdConfig.showGunReticle())) {
            LittleBirdAimRenderer.GunReticle reticle = LittleBirdAimRenderer.gunReticle();
            if (reticle.visible()) {
                HudCanvas.reticle(context.graphics,
                        (int) Math.round(reticle.x()), (int) Math.round(reticle.y()),
                        (int) Math.round(reticle.gapPixels()),
                        LittleBirdCrosshairMath.ARM_LENGTH_PIXELS, HudStyle.DANGER);
            }
        }
        if (LittleBirdCrosshairMath.showsRocketReticle(
                follows, weapon, LittleBirdConfig.showRocketReticle())) {
            LittleBirdAimRenderer.Reticle reticle = LittleBirdAimRenderer.rocketReticle();
            if (reticle.visible()) {
                drawRocketSight(context, reticle.x(), reticle.y(),
                        helicopter.getRocketsLeft(), helicopter.getRocketsRight());
            }
        }
    }

    /** Apache pilot: the reference rocket sight, only while the rocket bay is selected. */
    private void drawApacheRocketSight(HudContext context, ApacheEntity apache) {
        if (apache.getWeapon() != ApacheEntity.WEAPON_ROCKETS) {
            return;
        }
        ApacheAimRenderer.Reticle reticle = ApacheAimRenderer.rocketReticle();
        if (!reticle.visible()) {
            return;
        }
        int x = (int) Math.round(reticle.x());
        int y = (int) Math.round(reticle.y());
        rocketBrackets(context.graphics, x, y);
        int ready = apache.getReadyRockets();
        for (int pod = 0; pod < ApachePods.PODS; pod++) {
            for (int tube = 0; tube < ApachePods.tubeCount(pod); tube++) {
                if (!ApachePods.tubeLoaded(pod, tube, ready)) {
                    continue;
                }
                ApachePods.DotPosition dot = ApachePods.tubeDot(pod, tube);
                rocketDot(context.graphics, x, y, dot.column(),
                        dot.row() - (ApachePods.HUD_ROWS - 1) * 0.5F, pod == 0);
            }
        }
        circle(context.graphics, x, y, ROCKET_CIRCLE_RADIUS, HudStyle.TEXT);
    }

    /** Little Bird rocket sight: one dot column per pod, the reference reticle exactly. */
    private void drawRocketSight(HudContext context, double px, double py,
                                 int leftRounds, int rightRounds) {
        int x = (int) Math.round(px);
        int y = (int) Math.round(py);
        rocketBrackets(context.graphics, x, y);
        for (int tube = 0; tube < LittleBirdCrosshairMath.TUBES_PER_POD; tube++) {
            if (!LittleBirdPodTubes.loaded(tube, leftRounds)) {
                continue;
            }
            LittleBirdCrosshairMath.DotPosition dot = LittleBirdCrosshairMath.tubeDot(tube);
            rocketDot(context.graphics, x, y, dot.column(), dot.row() - 1.5F, true);
        }
        for (int tube = 0; tube < LittleBirdCrosshairMath.TUBES_PER_POD; tube++) {
            if (!LittleBirdPodTubes.loaded(tube, rightRounds)) {
                continue;
            }
            LittleBirdCrosshairMath.DotPosition dot = LittleBirdCrosshairMath.tubeDot(tube);
            rocketDot(context.graphics, x, y, dot.column(), dot.row() - 1.5F, false);
        }
        circle(context.graphics, x, y, ROCKET_CIRCLE_RADIUS, HudStyle.TEXT);
    }

    /** Apache gunner: four arms on the barrel's projected aim, amber when pegged on the stop. */
    private void drawTurretSight(HudContext context) {
        ApacheAimRenderer.Reticle reticle = ApacheAimRenderer.reticle();
        if (!reticle.visible()) {
            return;
        }
        int x = (int) Math.round(reticle.x());
        int y = (int) Math.round(reticle.y());
        int colour = reticle.pegged() ? HudStyle.WARN : HudStyle.CYAN;
        HudCanvas.reticle(context.graphics, x, y, TURRET_GAP, TURRET_ARM, colour);
        if (reticle.pegged()) {
            context.graphics.fill(x - 1, y - 1, x + 2, y + 2, colour);
        }
    }

    /** Jet: a bracket at the centre once the seeker has locked, a filling bar until then. */
    private void drawJetLock(HudContext context, JetEntity jet) {
        int weapon = jet.getWeapon();
        boolean seeker = weapon == JetEntity.WEAPON_IR_MISSILE
                || weapon == JetEntity.WEAPON_AGM
                || weapon == JetEntity.WEAPON_RADAR_MISSILE;
        if (!seeker) {
            return;
        }
        int cx = context.centreX();
        int cy = context.centreY();
        if (jet.isLocked()) {
            lockBrackets(context.graphics, cx, cy);
            centred(context, Component.translatable("hud.battlefield.locked").getString(),
                    cy - 32, HudStyle.DANGER);
            return;
        }
        int acquire = JetEntity.lockAcquireTicksFor(weapon);
        double progress = acquire <= 0 ? 0.0D : jet.getLockProgress() / (double) acquire;
        if (jet.getLockTargetId() < 0 && progress <= 0.0D) {
            return;
        }
        centred(context, Component.translatable("hud.battlefield.locking").getString(),
                cy - 32, HudStyle.WARN);
        HudCanvas.bar(context.graphics, cx - 30, cy - 22, 60, HudStyle.BAR, progress,
                HudStyle.WARN);
    }

    private static void lockBrackets(GuiGraphics graphics, int cx, int cy) {
        int left = cx - LOCK_RADIUS;
        int right = cx + LOCK_RADIUS;
        int top = cy - LOCK_RADIUS;
        int bottom = cy + LOCK_RADIUS;
        int colour = HudStyle.DANGER;
        int len = LOCK_TICK;
        graphics.fill(left, top, left + len, top + 1, colour);
        graphics.fill(left, top, left + 1, top + len, colour);
        graphics.fill(right - len, top, right, top + 1, colour);
        graphics.fill(right - 1, top, right, top + len, colour);
        graphics.fill(left, bottom - 1, left + len, bottom, colour);
        graphics.fill(left, bottom - len, left + 1, bottom, colour);
        graphics.fill(right - len, bottom - 1, right, bottom, colour);
        graphics.fill(right - 1, bottom - len, right, bottom, colour);
    }

    private static void rocketBrackets(GuiGraphics graphics, int x, int y) {
        rocketBracket(graphics, x - ROCKET_BAR_OFFSET, y);
        rocketBracket(graphics, x + ROCKET_BAR_OFFSET, y);
    }

    private static void rocketBracket(GuiGraphics graphics, int barX, int y) {
        int half = ROCKET_BAR_HALF_HEIGHT;
        int gap = ROCKET_BAR_GAP;
        graphics.fill(barX, y - half, barX + ROCKET_BAR_WIDTH, y - gap, HudStyle.WARN);
        graphics.fill(barX, y + gap, barX + ROCKET_BAR_WIDTH, y + half, HudStyle.WARN);
    }

    /** One pod tube indicator dot; {@code row} may be fractional so both pods centre alike. */
    private static void rocketDot(GuiGraphics graphics, int x, int y, int column, float row,
                                  boolean leftSide) {
        int columnOffset = (column + 1) * ROCKET_DOT_COLUMN_SPACING;
        int dotX = leftSide
                ? x - ROCKET_BAR_OFFSET - columnOffset
                : x + ROCKET_BAR_OFFSET + columnOffset;
        int dotY = y + Math.round(row * ROCKET_DOT_ROW_SPACING);
        graphics.fill(dotX, dotY, dotX + ROCKET_DOT_SIZE, dotY + ROCKET_DOT_SIZE, HudStyle.WARN);
    }

    private static void circle(GuiGraphics graphics, int cx, int cy, int radius, int colour) {
        for (int step = 0; step < 24; step++) {
            double angle = Math.toRadians(step * 15.0D);
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(x, y, x + 1, y + 1, colour);
        }
    }

    private static void centred(HudContext context, String text, int y, int colour) {
        context.graphics.drawString(context.font, text,
                context.centreX() - context.font.width(text) / 2, y, colour, false);
    }

    // ------------------------------------------------------------------
    // Weapon panels (bottom right)
    // ------------------------------------------------------------------

    private void drawLittleBirdPanel(HudContext context, LittleBirdEntity helicopter) {
        boolean gun = helicopter.getWeapon() == LittleBirdEntity.WEAPON_GUN;
        int height = gun ? LITTLE_BIRD_GUN_PANEL_HEIGHT : LITTLE_BIRD_ROCKET_PANEL_HEIGHT;
        int x = context.rightPanelX(PANEL_WIDTH);
        int y = context.height - height - MARGIN;
        String weapon = Component.translatable(gun
                ? "hud.battlefield.weapon.gun" : "hud.battlefield.weapon.rockets").getString();
        String ammo = gun ? Integer.toString(helicopter.getGunAmmo())
                : helicopter.getRocketsRemaining() + "/" + helicopter.getRocketReserve();
        HudCanvas.panel(context.graphics, context.font, weapon, ammo, x, y, PANEL_WIDTH,
                height, HudStyle.ACCENT);
        int left = x + HudStyle.PAD;
        int right = x + PANEL_WIDTH - HudStyle.PAD;
        int rowY = y + HudStyle.HEADER + HudStyle.PAD;
        if (gun) {
            double heat = helicopter.getGunHeat() / (double) LittleBirdGunCycle.MAX_HEAT;
            HudCanvas.row(context.graphics, context.font, left, right, rowY, "HEAT",
                    percent(heat), heatColour(heat));
            rowY += HudStyle.ROW;
            rowY = barRow(context, x, rowY, heat, heatColour(heat));
        } else {
            boolean reloading = helicopter.getRocketReloadTicks() > 0;
            double load = reloading ? helicopter.getRocketReloadFraction()
                    : helicopter.getRocketsRemaining() / (double) RocketPodState.READY_CAPACITY;
            int colour = reloading ? HudStyle.WARN : HudStyle.OK;
            HudCanvas.row(context.graphics, context.font, left, right, rowY,
                    reloading
                            ? Component.translatable("hud.battlefield.reloading").getString()
                            : "READY",
                    percent(load), colour);
            rowY += HudStyle.ROW;
            rowY = barRow(context, x, rowY, load, colour);
            HudCanvas.row(context.graphics, context.font, left, right, rowY, "PODS",
                    helicopter.getRocketsLeft() + "/" + helicopter.getRocketsRight(),
                    HudStyle.TEXT);
            rowY += HudStyle.ROW;
        }
        boolean salvo = helicopter.isSalvoMode();
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "MODE",
                Component.translatable(salvo
                        ? "hud.battlefield.mode.salvo" : "hud.battlefield.mode.single").getString(),
                salvo ? HudStyle.WARN : HudStyle.TEXT);
        rowY += HudStyle.ROW;
        int rpm = Math.round(helicopter.getRenderRotorRpm() * 100.0F);
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "RPM", rpm + "%",
                rpm < 15 ? HudStyle.WARN : HudStyle.TEXT);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "HULL",
                Integer.toString(Math.round(helicopter.getHealth())),
                healthColour(helicopter.getHealth(), LittleBirdEntity.MAX_HEALTH));
    }

    /** Rear seat: both magazines, their transfer and resupply clocks, then rotor and hull. */
    private void drawApachePilotPanel(HudContext context, ApacheEntity apache) {
        int x = context.rightPanelX(PANEL_WIDTH);
        int y = context.height - APACHE_PILOT_PANEL_HEIGHT - MARGIN;
        boolean rockets = apache.getWeapon() == ApacheEntity.WEAPON_ROCKETS;
        String weapon = Component.translatable(rockets
                ? "hud.battlefield.weapon.apache_rockets"
                : "hud.battlefield.weapon.apache_missiles").getString();
        boolean reloading = rockets
                ? apache.getRocketReloadFraction() > 0.0F
                : apache.getMissileReloadFraction() > 0.0F;
        HudCanvas.panel(context.graphics, context.font, weapon,
                reloading
                        ? Component.translatable("hud.battlefield.reloading").getString()
                        : "READY",
                x, y, PANEL_WIDTH, APACHE_PILOT_PANEL_HEIGHT, HudStyle.ACCENT);
        int left = x + HudStyle.PAD;
        int right = x + PANEL_WIDTH - HudStyle.PAD;
        int rowY = y + HudStyle.HEADER + HudStyle.PAD;
        rowY = magazineRow(context, x, left, right, rowY, "RKT",
                apache.getReadyRockets(), apache.getReserveRockets(),
                apache.getRocketReloadFraction(), apache.getRocketResupplyFraction(),
                ApacheEntity.ROCKET_READY_CAPACITY, ApacheEntity.ROCKET_RESERVE_CAPACITY,
                rockets);
        rowY = magazineRow(context, x, left, right, rowY, "MSL",
                apache.getReadyMissiles(), apache.getReserveMissiles(),
                apache.getMissileReloadFraction(), apache.getMissileResupplyFraction(),
                ApacheEntity.MISSILE_READY_CAPACITY, ApacheEntity.MISSILE_RESERVE_CAPACITY,
                !rockets);
        int rpm = Math.round(apache.getRenderRotorRpm() * 100.0F);
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "RPM", rpm + "%",
                rpm < 15 ? HudStyle.WARN : HudStyle.TEXT);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "HULL",
                Integer.toString(Math.round(apache.getHealth())),
                healthColour(apache.getHealth(), ApacheEntity.MAX_HEALTH));
    }

    /**
     * One magazine: ready out of reserve, the reserve-to-ready transfer bar, then the thin
     * automatic resupply bar. An idle bar falls back to how full the magazine is, so it never
     * reads as dead.
     */
    private int magazineRow(HudContext context, int x, int left, int right, int y, String label,
                            int ready, int reserve, float reloadFraction, float resupplyFraction,
                            int readyCapacity, int reserveCapacity, boolean selected) {
        HudCanvas.row(context.graphics, context.font, left, right, y, label,
                ready + "/" + reserve, selected ? HudStyle.ACCENT : HudStyle.TEXT);
        y += HudStyle.ROW;
        boolean reloading = reloadFraction > 0.0F;
        double transfer = reloading
                ? reloadFraction : ready / (double) Math.max(1, readyCapacity);
        y = barRow(context, x, y, transfer, reloading ? HudStyle.CYAN : HudStyle.MUTED);
        boolean resupplying = resupplyFraction > 0.0F;
        double supply = resupplying
                ? resupplyFraction : reserve / (double) Math.max(1, reserveCapacity);
        return barRow(context, x, y, supply, resupplying ? HudStyle.TEXT : HudStyle.MUTED);
    }

    /** Front seat: chain gun rounds, turret bearing and barrel heat, then rotor and hull. */
    private void drawApacheGunnerPanel(HudContext context, ApacheEntity apache) {
        int x = context.rightPanelX(PANEL_WIDTH);
        int y = context.height - APACHE_GUNNER_PANEL_HEIGHT - MARGIN;
        HudCanvas.panel(context.graphics, context.font,
                Component.translatable("hud.battlefield.weapon.apache_chain_gun").getString(),
                "GUN", x, y, PANEL_WIDTH, APACHE_GUNNER_PANEL_HEIGHT, HudStyle.ACCENT);
        int left = x + HudStyle.PAD;
        int right = x + PANEL_WIDTH - HudStyle.PAD;
        int rowY = y + HudStyle.HEADER + HudStyle.PAD;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "AMMO",
                Integer.toString(apache.getChainAmmo()),
                apache.getChainAmmo() > 0 ? HudStyle.TEXT : HudStyle.DANGER);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "AZ",
                String.format(Locale.ROOT, "%.0f", apache.getTurretRelativeBearingDeg()),
                HudStyle.TEXT);
        rowY += HudStyle.ROW;
        double heat = apache.getChainGunHeatFraction();
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "HEAT",
                percent(heat), heatColour(heat));
        rowY += HudStyle.ROW;
        rowY = barRow(context, x, rowY, heat, heatColour(heat));
        int rpm = Math.round(apache.getRenderRotorRpm() * 100.0F);
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "RPM", rpm + "%",
                rpm < 15 ? HudStyle.WARN : HudStyle.TEXT);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "HULL",
                Integer.toString(Math.round(apache.getHealth())),
                healthColour(apache.getHealth(), ApacheEntity.MAX_HEALTH));
    }

    /** Jet: selected store, the two other counts, cannon heat, throttle, burner and hull. */
    private void drawJetPanel(HudContext context, JetEntity jet) {
        int x = context.rightPanelX(PANEL_WIDTH);
        int y = context.height - JET_PANEL_HEIGHT - MARGIN;
        int weapon = jet.getWeapon();
        HudCanvas.panel(context.graphics, context.font,
                Component.translatable(jetWeaponKey(weapon)).getString(),
                jet.isVtol() ? Component.translatable("hud.battlefield.vtol").getString() : null,
                x, y, PANEL_WIDTH, JET_PANEL_HEIGHT, HudStyle.ACCENT);
        int left = x + HudStyle.PAD;
        int right = x + PANEL_WIDTH - HudStyle.PAD;
        int rowY = y + HudStyle.HEADER + HudStyle.PAD;

        HudCanvas.row(context.graphics, context.font, left, right, rowY, "AMMO",
                Integer.toString(jetWeaponAmmo(jet, weapon)),
                jetWeaponAmmo(jet, weapon) > 0 ? HudStyle.TEXT : HudStyle.DANGER);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "RKT",
                Integer.toString(jet.getRocketReady()),
                jet.getRocketReady() > 0 ? HudStyle.TEXT : HudStyle.DANGER);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "MSL",
                Integer.toString(jet.getMissileAmmo()),
                jet.getMissileAmmo() > 0 ? HudStyle.TEXT : HudStyle.DANGER);
        rowY += HudStyle.ROW;
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "FLR",
                Integer.toString(jet.getFlares()),
                jet.getFlares() > 0 ? HudStyle.TEXT : HudStyle.DANGER);
        rowY += HudStyle.ROW;

        double heat = jet.getCannonHeat() / (double) Math.max(1, jet.cannonMaxHeat());
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "HEAT",
                percent(heat), heatColour(heat));
        rowY += HudStyle.ROW;
        rowY = barRow(context, x, rowY, heat, heatColour(heat));
        double throttle = jet.getRenderThrottle();
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "THR",
                percent(throttle), HudStyle.CYAN);
        rowY += HudStyle.ROW;
        rowY = barRow(context, x, rowY, throttle, HudStyle.CYAN);
        double burner = jet.getRenderAfterburnerCharge();
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "AB",
                percent(burner), HudStyle.WARN);
        rowY += HudStyle.ROW;
        rowY = barRow(context, x, rowY, burner, HudStyle.WARN);
        HudCanvas.row(context.graphics, context.font, left, right, rowY, "HULL",
                Integer.toString(Math.round(jet.getHealth())),
                healthColour(jet.getHealth(), JetEntity.MAX_HEALTH));
    }

    // ------------------------------------------------------------------
    // Alerts
    // ------------------------------------------------------------------

    private static List<Component> littleBirdAlerts(LittleBirdEntity helicopter) {
        List<Component> alerts = new ArrayList<>(2);
        if (helicopter.getGunState() == LittleBirdGunCycle.OVERHEATED) {
            alerts.add(Component.translatable("alert.battlefield.overheat"));
        }
        if (helicopter.getRenderRotorRpm() < 0.15F) {
            alerts.add(Component.translatable("alert.battlefield.engine_off"));
        } else if (helicopter.getRocketsRemaining() <= 0) {
            alerts.add(Component.translatable("alert.battlefield.no_rockets"));
        }
        addHullAlert(alerts, helicopter.getHealth(), LittleBirdEntity.MAX_HEALTH);
        return alerts;
    }

    private static List<Component> apacheAlerts(ApacheEntity apache, boolean pilot) {
        List<Component> alerts = new ArrayList<>(2);
        if (apache.getChainGunHeatFraction() >= 1.0F) {
            alerts.add(Component.translatable("alert.battlefield.overheat"));
        }
        if (apache.getRenderRotorRpm() < 0.15F) {
            alerts.add(Component.translatable("alert.battlefield.engine_off"));
        }
        if (pilot && apache.getReadyRockets() + apache.getReserveRockets() <= 0
                && apache.getReadyMissiles() + apache.getReserveMissiles() <= 0) {
            alerts.add(Component.translatable("alert.battlefield.no_ammo"));
        }
        addHullAlert(alerts, apache.getHealth(), ApacheEntity.MAX_HEALTH);
        return alerts;
    }

    private static List<Component> jetAlerts(JetEntity jet) {
        List<Component> alerts = new ArrayList<>(2);
        if (jet.getCannonState() == JetCannon.OVERHEATED) {
            alerts.add(Component.translatable("alert.battlefield.overheat"));
        }
        if (!jet.isEngineOn()) {
            alerts.add(Component.translatable("alert.battlefield.engine_off"));
        }
        if (jet.getCannonAmmo() <= 0 && jet.getRocketReady() <= 0
                && jet.getMissileAmmo() <= 0) {
            alerts.add(Component.translatable("alert.battlefield.no_ammo"));
        }
        addHullAlert(alerts, jet.getHealth(), JetEntity.MAX_HEALTH);
        return alerts;
    }

    private static void addHullAlert(List<Component> alerts, float health, float maxHealth) {
        if (LittleBirdHudLayout.lowHealth(health, maxHealth)) {
            alerts.add(Component.translatable("alert.battlefield.low_health"));
        }
    }

    // ------------------------------------------------------------------
    // Small helpers
    // ------------------------------------------------------------------

    /** One full width bar in the right hand panel; returns the y of the next row. */
    private static int barRow(HudContext context, int x, int y, double fraction, int colour) {
        HudCanvas.bar(context.graphics, x + HudStyle.PAD, y,
                PANEL_WIDTH - HudStyle.PAD * 2, HudStyle.BAR, fraction, colour);
        return y + HudStyle.BAR + 3;
    }

    private static String percent(double fraction) {
        double clamped = Double.isFinite(fraction)
                ? Math.max(0.0D, Math.min(1.0D, fraction)) : 0.0D;
        return Math.round(clamped * 100.0D) + "%";
    }

    private static int heatColour(double fraction) {
        if (fraction >= 0.9D) {
            return HudStyle.DANGER;
        }
        if (fraction >= 0.6D) {
            return HudStyle.WARN;
        }
        return HudStyle.OK;
    }

    private static int healthColour(float health, float maxHealth) {
        return LittleBirdHudLayout.lowHealth(health, maxHealth) ? HudStyle.DANGER : HudStyle.TEXT;
    }

    private static String signedDegrees(float degrees) {
        return String.format(Locale.ROOT, "%+d", Math.round(degrees));
    }

    private static String jetWeaponKey(int weapon) {
        return switch (weapon) {
            case JetEntity.WEAPON_ROCKETS -> "hud.battlefield.weapon.jet_rockets";
            case JetEntity.WEAPON_IR_MISSILE -> "hud.battlefield.weapon.jet_ir";
            case JetEntity.WEAPON_AGM -> "hud.battlefield.weapon.jet_agm";
            case JetEntity.WEAPON_RADAR_MISSILE -> "hud.battlefield.weapon.jet_radar";
            default -> "hud.battlefield.weapon.jet_cannon";
        };
    }

    private static int jetWeaponAmmo(JetEntity jet, int weapon) {
        return switch (weapon) {
            case JetEntity.WEAPON_ROCKETS -> jet.getRocketReady();
            case JetEntity.WEAPON_IR_MISSILE, JetEntity.WEAPON_AGM,
                    JetEntity.WEAPON_RADAR_MISSILE -> jet.getMissileAmmo();
            default -> jet.getCannonAmmo();
        };
    }
}
