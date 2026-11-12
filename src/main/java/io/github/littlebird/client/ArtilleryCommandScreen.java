package io.github.littlebird.client;

import io.github.littlebird.LittleBirdConfig;
import io.github.littlebird.client.hud.HudCanvas;
import io.github.littlebird.client.hud.HudRadar;
import io.github.littlebird.client.hud.HudStyle;
import io.github.littlebird.entity.ArtilleryStrike;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules;
import io.github.littlebird.entity.weapon.ArtilleryStrikeRules.Offset;
import io.github.littlebird.entity.weapon.RocketStrikeGrid;
import io.github.littlebird.network.ArtilleryStrikeCallPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

/** North-up fire-mission board bound to the artillery vehicle that opened it. */
public final class ArtilleryCommandScreen extends Screen {
    private static final int REFRESH_TICKS = 4;
    private static final int TITLE_Y = 16;
    private static final int CROSSHAIR_ARM = 7;

    private final Entity vehicle;
    private final ArtilleryStrike strike;
    private long lastScanTick = Long.MIN_VALUE;

    ArtilleryCommandScreen(Entity vehicle, ArtilleryStrike strike) {
        super(Component.translatable(strike.isRocketGrid()
                ? "screen.battlefield.artillery.title.phl191"
                : "screen.battlefield.artillery.title.plz05"));
        this.vehicle = vehicle;
        this.strike = strike;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!stillDriving()) {
            onClose();
            return;
        }
        refreshContacts();

        g.fill(0, 0, width, height, 0xC8060907);
        Geometry geometry = geometry();
        g.drawCenteredString(font, getTitle(), width / 2, TITLE_Y, HudStyle.ACCENT);
        HudRadar.draw(g, font, geometry.x0(), geometry.y0(), geometry.size(), radarSpec());

        Offset cursor = geometry.offsetAt(mouseX, mouseY);
        boolean inRange = inRange(cursor);
        if (strike.isRocketGrid()) {
            drawRocketGrid(g, geometry, cursor, inRange ? HudStyle.WARN : HudStyle.DANGER);
        } else {
            int spreadRadius = (int) Math.round(
                    LittleBirdConfig.artilleryStrikeSpreadRadius() * geometry.scale());
            HudRadar.circle(g, mouseX, mouseY, spreadRadius, HudStyle.WARN);
            g.fill(mouseX - CROSSHAIR_ARM, mouseY, mouseX + CROSSHAIR_ARM + 1, mouseY + 1,
                    HudStyle.TEXT);
            g.fill(mouseX, mouseY - CROSSHAIR_ARM, mouseX + 1, mouseY + CROSSHAIR_ARM + 1,
                    HudStyle.TEXT);
        }

        g.drawCenteredString(font, statusLine(), width / 2, height - 42, statusColour());
        g.drawCenteredString(font, inRange
                        ? Component.translatable("screen.battlefield.artillery.hint")
                        : Component.translatable("screen.battlefield.artillery.out_of_range",
                                Math.round(strike.rangeBlocks())),
                width / 2, height - 26, inRange ? HudStyle.MUTED : HudStyle.DANGER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 || !stillDriving()) {
            onClose();
            return true;
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        Offset cursor = geometry().offsetAt(mouseX, mouseY);
        if (!inRange(cursor) || !strike.ready()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        // The server rechecks the caller, the range and the cooldown.
        ArtilleryStrikeCallPacket.send(vehicle.getX() + cursor.dx(), vehicle.getZ() + cursor.dz());
        onClose();
        return true;
    }

    private boolean stillDriving() {
        return minecraft != null && minecraft.level == vehicle.level()
                && ArtilleryCommand.isDriving(vehicle, minecraft.player);
    }

    private boolean inRange(Offset cursor) {
        return ArtilleryStrikeRules.inRange(cursor.dx(), cursor.dz(), strike.rangeBlocks());
    }

    private void drawRocketGrid(GuiGraphics g, Geometry geometry, Offset cursor, int colour) {
        RocketStrikeGrid grid = RocketStrikeGrid.facing(cursor.dx(), cursor.dz(), vehicle.getYRot());
        int last = RocketStrikeGrid.SIZE - 1;
        Offset farLeft = grid.pointOffset(0, 0);
        Offset farRight = grid.pointOffset(0, last);
        Offset nearLeft = grid.pointOffset(last, 0);
        Offset nearRight = grid.pointOffset(last, last);
        int rim = (colour & 0x00FFFFFF) | 0x55000000;
        int firstRim = (colour & 0x00FFFFFF) | 0x99000000;
        drawGridEdge(g, geometry, cursor, farLeft, farRight, firstRim);
        drawGridEdge(g, geometry, cursor, farLeft, nearLeft, rim);
        drawGridEdge(g, geometry, cursor, farRight, nearRight, rim);
        drawGridEdge(g, geometry, cursor, nearLeft, nearRight, rim);

        for (int row = 0; row < RocketStrikeGrid.SIZE; row++) {
            for (int column = 0; column < RocketStrikeGrid.SIZE; column++) {
                Offset point = grid.pointOffset(row, column);
                int x = geometry.pixelX(cursor.dx() + point.dx());
                int y = geometry.pixelY(cursor.dz() + point.dz());
                int markerColour = row == 0 || (row == last / 2 && column == last / 2)
                        ? HudStyle.TEXT : colour;
                g.fill(x, y, x + 1, y + 1, markerColour);
            }
        }
    }

    private static void drawGridEdge(GuiGraphics g, Geometry geometry, Offset cursor,
            Offset start, Offset end, int colour) {
        HudCanvas.line(g, geometry.pixelX(cursor.dx() + start.dx()),
                geometry.pixelY(cursor.dz() + start.dz()),
                geometry.pixelX(cursor.dx() + end.dx()), geometry.pixelY(cursor.dz() + end.dz()),
                colour);
    }

    private HudRadar.Spec radarSpec() {
        List<HudRadar.Contact> contacts = new ArrayList<>();
        if (strike.isRocketGrid()) {
            for (Phl191Recon.Dot dot : Phl191Recon.dots()) {
                contacts.add(new HudRadar.Contact(dot.x(), dot.z(), dot.vehicle()));
            }
            Phl191Recon.Target target = Phl191Recon.target();
            return new HudRadar.Spec(strike.rangeBlocks(), Phl191Recon.vehicleYaw(), contacts,
                    Phl191Recon.hasImpact(), Phl191Recon.impactX(), Phl191Recon.impactZ(),
                    target != null, target == null ? 0.0D : target.x(),
                    target == null ? 0.0D : target.z());
        }
        for (Plz05Recon.Dot dot : Plz05Recon.dots()) {
            contacts.add(new HudRadar.Contact(dot.x(), dot.z(), dot.vehicle()));
        }
        Plz05Recon.Target target = Plz05Recon.target();
        return new HudRadar.Spec(strike.rangeBlocks(), Plz05Recon.vehicleYaw(), contacts,
                Plz05Recon.hasImpact(), Plz05Recon.impactX(), Plz05Recon.impactZ(),
                target != null, target == null ? 0.0D : target.x(),
                target == null ? 0.0D : target.z());
    }

    private void refreshContacts() {
        long now = vehicle.level().getGameTime();
        if (lastScanTick == Long.MIN_VALUE || now - lastScanTick >= REFRESH_TICKS) {
            lastScanTick = now;
            if (strike.isRocketGrid()) {
                Phl191Recon.refreshNow();
            } else {
                Plz05Recon.refreshNow();
            }
        }
    }

    private Component statusLine() {
        if (strike.active()) {
            return Component.translatable("screen.battlefield.artillery.active",
                    strike.roundsFired(), strike.roundsTotal());
        }
        if (!strike.ready()) {
            return Component.translatable("screen.battlefield.artillery.cooldown",
                    (strike.cooldownTicks() + 19) / 20);
        }
        return Component.translatable("screen.battlefield.artillery.ready");
    }

    private int statusColour() {
        if (strike.active()) {
            return HudStyle.WARN;
        }
        return strike.ready() ? HudStyle.ACCENT : HudStyle.MUTED;
    }

    private Geometry geometry() {
        int available = Math.max(4, Math.min(width - 16, height - 96));
        // Leave room for the footprint when its centre is at the edge of the radar.
        double footprintRadius = strike.isRocketGrid()
                ? Math.sqrt(2.0D) * (RocketStrikeGrid.SIZE - 1) * RocketStrikeGrid.SPACING_BLOCKS / 2.0D
                : LittleBirdConfig.artilleryStrikeSpreadRadius();
        double scaleReserve = 1.0D + footprintRadius / strike.rangeBlocks();
        int size = Math.max(4, (int) (available / scaleReserve));
        return new Geometry((width - size) / 2, (height - size) / 2 - 2, size, strike.rangeBlocks());
    }

    /** Shared transform for drawing and designating points on the scope. */
    private record Geometry(int x0, int y0, int size, double rangeBlocks) {
        double scale() {
            return (size / 2 - 1) / rangeBlocks;
        }

        Offset offsetAt(double x, double y) {
            return new Offset((x - x0 - size / 2) / scale(),
                    (y - y0 - size / 2) / scale());
        }

        int pixelX(double dx) {
            return x0 + size / 2 + (int) Math.round(dx * scale());
        }

        int pixelY(double dz) {
            return y0 + size / 2 + (int) Math.round(dz * scale());
        }
    }
}
