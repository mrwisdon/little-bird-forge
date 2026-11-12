package io.github.littlebird.client.hud;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.LittleBirdKeyMappings;
import io.github.littlebird.client.Phl191Aim;
import io.github.littlebird.client.Phl191Recon;
import io.github.littlebird.client.Plz05Aim;
import io.github.littlebird.client.Plz05Recon;
import io.github.littlebird.client.RocketCam;
import io.github.littlebird.entity.ArtilleryStrike;
import io.github.littlebird.entity.Phl191Entity;
import io.github.littlebird.entity.Plz05Entity;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules.Phase;
import io.github.littlebird.entity.weapon.Phl191Ballistics;
import io.github.littlebird.entity.weapon.Plz05Ballistics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * PHL-191 and PLZ-05 driver HUD, rebuilt on the new canvas.
 *
 * <p>Reading order is the one the crew needs: can I shoot (status), how much is left (ammunition),
 * where is the gun pointing (laying angle), where will it land (range and the world marker), and
 * only then the scope. The two launchers share this file; the numbers and strings differ.
 */
public final class HudArtillery implements HudView {
    private static final int PANEL_WIDTH = 158;
    private static final int PANEL_HEIGHT = 74;

    @Override
    public boolean appliesTo(HudContext context) {
        return context.minecraft.player.getVehicle() instanceof Plz05Entity
                || context.minecraft.player.getVehicle() instanceof Phl191Entity;
    }

    @Override
    public void render(HudContext context) {
        var vehicle = context.minecraft.player.getVehicle();
        if (vehicle instanceof Plz05Entity howitzer) {
            if (!howitzer.isDriver(howitzer.seatOf(context.minecraft.player))) {
                return;
            }
            Plz05Aim.Reticle reticle = Plz05Aim.reticle();
            Plz05Aim.Solution solution = Plz05Aim.solution();
            GunData data = new GunData();
            data.title = Component.translatable("hud.battlefield.plz05.title").getString();
            data.ammo = Component.translatable("hud.battlefield.plz05.tubes",
                    howitzer.getTubes(), Plz05Ballistics.SHELLS).getString();
            data.ammoLeft = howitzer.getTubes();
            data.elevation = howitzer.getElevationDeg();
            data.reloadTicks = howitzer.getReloadTicks();
            data.reloadTotal = Plz05Ballistics.RELOAD_TICKS;
            data.qteActive = Plz05Ballistics.qteActive(howitzer.getReloadTicks(),
                    howitzer.isQteDone());
            data.qtePhase = Plz05Ballistics.qtePhase(howitzer.getReloadTicks());
            data.qteInWindow = Plz05Ballistics.qteInWindow(howitzer.getReloadTicks());
            data.qteCycle = Plz05Ballistics.QTE_TICKS;
            data.qteWindowStart = Plz05Ballistics.QTE_WINDOW_START;
            data.qteWindowEnd = Plz05Ballistics.QTE_WINDOW_END;
            data.solutionValid = solution.valid();
            data.range = solution.valid() ? solution.rangeBlocks() : 0.0D;
            data.flightTicks = solution.valid() ? solution.flightTicks() : 0.0D;
            data.reticleVisible = reticle.visible();
            data.reticleX = (int) reticle.x();
            data.reticleY = (int) reticle.y();
            data.radarOpen = Plz05Recon.isActive();
            data.yaw = Plz05Recon.vehicleYaw();
            data.radius = howitzer.strike().rangeBlocks();
            data.hasImpact = Plz05Recon.hasImpact();
            data.impactX = Plz05Recon.impactX();
            data.impactZ = Plz05Recon.impactZ();
            Plz05Recon.Target target = Plz05Recon.target();
            data.hasTarget = target != null;
            data.targetX = target == null ? 0.0D : target.x();
            data.targetZ = target == null ? 0.0D : target.z();
            for (Plz05Recon.Dot dot : Plz05Recon.dots()) {
                data.contacts.add(new HudRadar.Contact(dot.x(), dot.z(), dot.vehicle()));
            }
            data.hint = Component.translatable("hud.battlefield.plz05.hint").getString();
            readStrike(data, howitzer.strike(), howitzer.getX(), howitzer.getZ());
            data.followHint = Component.translatable("hud.battlefield.plz05.follow").getString();
            draw(context, data);
        } else if (vehicle instanceof Phl191Entity launcher) {
            if (!launcher.isDriver(launcher.seatOf(context.minecraft.player))) {
                return;
            }
            Phl191Aim.Reticle reticle = Phl191Aim.reticle();
            Phl191Aim.Solution solution = Phl191Aim.solution();
            GunData data = new GunData();
            data.title = Component.translatable("hud.battlefield.phl191.title").getString();
            data.ammo = Component.translatable("hud.battlefield.phl191.tubes",
                    launcher.getTubes(), Phl191Ballistics.TUBES).getString();
            data.ammoLeft = launcher.getTubes();
            data.elevation = launcher.getElevationDeg();
            data.reloadTicks = launcher.getReloadTicks();
            data.reloadTotal = Phl191Ballistics.RELOAD_TICKS;
            data.qteActive = Phl191Ballistics.qteActive(launcher.getReloadTicks(),
                    launcher.isQteDone());
            data.qtePhase = Phl191Ballistics.qtePhase(launcher.getReloadTicks());
            data.qteInWindow = Phl191Ballistics.qteInWindow(launcher.getReloadTicks());
            data.qteCycle = Phl191Ballistics.QTE_TICKS;
            data.qteWindowStart = Phl191Ballistics.QTE_WINDOW_START;
            data.qteWindowEnd = Phl191Ballistics.QTE_WINDOW_END;
            data.solutionValid = solution.valid();
            data.range = solution.valid() ? solution.rangeBlocks() : 0.0D;
            data.flightTicks = solution.valid() ? solution.flightTicks() : 0.0D;
            data.reticleVisible = reticle.visible();
            data.reticleX = (int) reticle.x();
            data.reticleY = (int) reticle.y();
            data.radarOpen = Phl191Recon.isActive();
            data.yaw = Phl191Recon.vehicleYaw();
            data.radius = launcher.strike().rangeBlocks();
            data.hasImpact = Phl191Recon.hasImpact();
            data.impactX = Phl191Recon.impactX();
            data.impactZ = Phl191Recon.impactZ();
            Phl191Recon.Target target = Phl191Recon.target();
            data.hasTarget = target != null;
            data.targetX = target == null ? 0.0D : target.x();
            data.targetZ = target == null ? 0.0D : target.z();
            for (Phl191Recon.Dot dot : Phl191Recon.dots()) {
                data.contacts.add(new HudRadar.Contact(dot.x(), dot.z(), dot.vehicle()));
            }
            data.hint = Component.translatable("hud.battlefield.phl191.hint").getString();
            readStrike(data, launcher.strike(), launcher.getX(), launcher.getZ());
            data.followHint = Component.translatable("hud.battlefield.phl191.follow").getString();
            draw(context, data);
        }
    }

    private static void readStrike(GunData data, ArtilleryStrike strike, double x, double z) {
        Component key = LittleBirdKeyMappings.ARTILLERY_STRIKE.getTranslatedKeyMessage();
        if (LittleBirdConfig.artilleryStrikeEnabled()) {
            data.hint += "   |   "
                    + Component.translatable("hud.battlefield.artillery.strike_hint", key).getString();
        }

        data.hasStrike = strike.active();
        if (data.hasStrike) {
            data.strikeX = strike.targetX() - x;
            data.strikeZ = strike.targetZ() - z;
            int total = ArtilleryStrikeRules.totalTicks(
                    LittleBirdConfig.artilleryStrikeWarmupSeconds(), strike.durationSeconds());
            int remaining = Math.max(0, total - strike.elapsedTicks());
            data.strikeText = Component.translatable("hud.battlefield.artillery.strike",
                    String.format(Locale.ROOT, "%.1f", remaining / 20.0D),
                    strike.roundsFired(), strike.roundsTotal()).getString();
            data.strikeColour = strike.phase() == Phase.WARMUP ? HudStyle.WARN : HudStyle.DANGER;
        } else if (strike.ready()) {
            data.strikeText = Component.translatable("hud.battlefield.artillery.strike_ready",
                    key).getString();
            data.strikeColour = HudStyle.OK;
        } else if (strike.cooldownTicks() > 0) {
            data.strikeText = Component.translatable("hud.battlefield.artillery.strike_cooldown",
                    (strike.cooldownTicks() + 19) / 20).getString();
        }
    }

    /** Everything one launcher frame needs, already read from its entity. */
    private static final class GunData {
        String title = "";
        String ammo = "";
        String hint = "";
        String followHint = "";
        int ammoLeft;
        double elevation;
        int reloadTicks;
        int reloadTotal;
        boolean qteActive;
        int qtePhase;
        boolean qteInWindow;
        int qteCycle;
        int qteWindowStart;
        int qteWindowEnd;
        boolean solutionValid;
        double range;
        double flightTicks;
        boolean reticleVisible;
        int reticleX;
        int reticleY;
        boolean radarOpen;
        float yaw;
        double radius;
        boolean hasImpact;
        double impactX;
        double impactZ;
        boolean hasTarget;
        double targetX;
        double targetZ;
        boolean hasStrike;
        double strikeX;
        double strikeZ;
        String strikeText = "";
        int strikeColour = HudStyle.MUTED;
        final List<HudRadar.Contact> contacts = new ArrayList<>();
    }

    private void draw(HudContext context, GunData data) {
        GuiGraphics g = context.graphics;
        Font font = context.font;
        boolean reloading = data.ammoLeft <= 0 && data.reloadTicks > 0;

        drawMarker(context, data);
        HudCanvas.reticle(g, context.centreX(), context.centreY(), 7, 6, HudStyle.TEXT);

        int left = data.radarOpen ? context.rightPanelX(PANEL_WIDTH + 8) : context.leftPanelX();
        int top = context.height / 2 - 48;
        HudCanvas.panel(g, font, data.title, reloading ? "RELOAD" : "READY",
                left, top, PANEL_WIDTH, PANEL_HEIGHT,
                reloading ? HudStyle.WARN : HudStyle.ACCENT);
        int right = left + PANEL_WIDTH - HudStyle.PAD;
        int y = top + 15;

        HudCanvas.line(g, font, left + 8, y, data.ammo,
                data.ammoLeft > 0 ? HudStyle.TEXT : HudStyle.MUTED);
        y += HudStyle.ROW;
        HudCanvas.row(g, font, left + 8, right, y, "ELEV",
                String.format(java.util.Locale.ROOT, "%.1f", data.elevation), HudStyle.TEXT);
        y += HudStyle.ROW;
        if (data.solutionValid) {
            HudCanvas.row(g, font, left + 8, right, y, "RANGE",
                    Math.round(data.range) + " m", HudStyle.ACCENT);
        }
        y += HudStyle.ROW;

        if (reloading) {
            int barWidth = PANEL_WIDTH - 16;
            int barY = top + PANEL_HEIGHT - 16;
            HudCanvas.bar(g, left + 8, barY, barWidth, 4,
                    1.0D - data.reloadTicks / (double) data.reloadTotal,
                    reloading ? HudStyle.WARN : HudStyle.ACCENT);
            if (data.qteActive) {
                int x0 = left + 8 + (int) Math.round(barWidth * data.qteWindowStart
                        / (double) data.qteCycle);
                int x1 = left + 8 + (int) Math.round(barWidth * data.qteWindowEnd
                        / (double) data.qteCycle);
                g.fill(x0, barY - 2, x1, barY + 6,
                        data.qteInWindow ? HudStyle.OK : 0x66E8B464);
                int needle = left + 8 + (int) Math.round(barWidth * data.qtePhase
                        / (double) data.qteCycle);
                g.fill(needle, barY - 3, needle + 1, barY + 7, 0xFFFFFFFF);
            }
            HudCanvas.line(g, font, left + 8, top + PANEL_HEIGHT - 26,
                    Component.translatable(data.qteInWindow
                            ? "hud.battlefield.plz05.qte_now" : "hud.battlefield.plz05.qte")
                            .getString(),
                    data.qteInWindow ? HudStyle.OK : HudStyle.MUTED);
        }

        if (!data.strikeText.isEmpty()) {
            HudCanvas.line(g, font, left, top + PANEL_HEIGHT + 3, data.strikeText,
                    data.strikeColour);
        }

        if (data.radarOpen) {
            drawRadar(context, data);
        }

        String hint = data.hint;
        if (RocketCam.isActive()) {
            hint = hint + "   |   " + data.followHint;
        }
        HudCanvas.hint(g, font, context.width, context.height, hint);
    }

    /** World-space impact marker: brackets, centre dot, range and time of flight. */
    private void drawMarker(HudContext context, GunData data) {
        if (!data.reticleVisible || !data.solutionValid) {
            return;
        }
        GuiGraphics g = context.graphics;
        int x = data.reticleX;
        int y = data.reticleY;
        int arm = 6;
        int c = HudStyle.WARN;
        g.fill(x - arm, y - arm, x - arm + 4, y - arm + 1, c);
        g.fill(x - arm, y - arm, x - arm + 1, y - arm + 4, c);
        g.fill(x + arm - 4, y - arm, x + arm, y - arm + 1, c);
        g.fill(x + arm - 1, y - arm, x + arm, y - arm + 4, c);
        g.fill(x - arm, y + arm - 1, x - arm + 4, y + arm, c);
        g.fill(x - arm, y + arm - 4, x - arm + 1, y + arm, c);
        g.fill(x + arm - 4, y + arm - 1, x + arm, y + arm, c);
        g.fill(x + arm - 1, y + arm - 4, x + arm, y + arm, c);
        g.fill(x - 1, y - 1, x + 1, y + 1, c);
        String label = Math.round(data.range) + "m  "
                + String.format(java.util.Locale.ROOT, "%.1fs", data.flightTicks / 20.0D);
        g.drawString(context.font, label, x - context.font.width(label) / 2, y + arm + 3, c,
                true);
    }

    private void drawRadar(HudContext context, GunData data) {
        int size = Math.min(190, Math.max(120, context.height - 110));
        int x0 = 10;
        int y0 = context.height - 56 - size;
        HudRadar.Spec spec = new HudRadar.Spec(data.radius, data.yaw, data.contacts,
                data.hasImpact, data.impactX, data.impactZ,
                data.hasTarget, data.targetX, data.targetZ,
                data.hasStrike, data.strikeX, data.strikeZ);
        HudRadar.draw(context.graphics, context.font, x0, y0, size, spec);
        HudCanvas.line(context.graphics, context.font, x0, y0 - 10,
                Component.translatable("hud.battlefield.plz05.radar",
                        Math.round(data.radius)).getString(), HudStyle.ACCENT);
    }
}
