package io.github.littlebird.client;

import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Draws the tank's optical gun sight: the rigid tube, the mil drop ladder and the aiming marks.
 *
 * <p>Everything is a whole pixel {@code fill} plus {@code drawString} - no textures, no gradients,
 * no anti-aliased shapes - because hard edges are what make the sight read as a machined optic
 * instead of a soft overlay, and because a couple of dozen fills cost nothing next to the world
 * render. Nothing is allocated per frame either: the ladder table is cached per shell and only
 * rebuilt when the gun's own ballistics change.
 */
public final class GunSightDraw {
    /** Tube mask: near black, so the world outside the optic stops competing with the ladder. */
    public static final int MASK_COLOUR = 0xE80A0C10;
    /** The tube's inner rim, the thin bright edge that draws the opening's hard boundary. */
    public static final int RIM_COLOUR = 0xFF9FB6C4;
    /** Crosshair wire and target box. */
    public static final int RETICLE_COLOUR = 0xFFD8F2E6;
    /** Ladder ticks and their labels, one step dimmer than the wire. */
    public static final int LADDER_COLOUR = 0xFFB6D6C8;
    /** The armour piercing aim point at the centre of the cross. */
    public static final int CENTRE_COLOUR = 0xFFFFE08A;

    /** Fire control colour: the predicted impact, deliberately not the reticle's green. */
    public static final int IMPACT_COLOUR = 0xFFFFC24A;

    private static final int TANK_GRATICULE_COLOUR = 0xFFB6CDC3;
    private static final int TANK_BORE_COLOUR = 0xFFE1EAF0;
    private static final int TANK_LINE_SHADOW = 0xB00C1011;
    private static final int TANK_CENTRE_GAP = 5;
    private static final int TANK_WIRE_LENGTH = 12;
    private static final int TANK_TICK_STEP_MILS = 10;
    private static final int TANK_TICK_LIMIT_MILS = 60;

    /** The rim is a single pixel, the same weight as every other line in the HUD. */
    private static final int RIM_THICKNESS = 1;
    /** Half width of the gap the wires leave around the aim point. */
    private static final int CENTRE_GAP_PIXELS = 7;
    /** Side of the solid aim point. */
    /** Keep the impact marker this far inside the tube opening. */
    private static final int MARKER_MARGIN = 6;

    private static final int CENTRE_DOT_SIZE = 3;
    /** Half side of the target box that frames the aim point. */
    private static final int TARGET_BOX_HALF = 12;
    /** Half length of a ladder tick that carries a label. */
    private static final int TICK_HALF_LABELLED = 10;
    /** Half length of an unlabelled ladder tick. */
    private static final int TICK_HALF_PLAIN = 5;
    /** Range step of the ladder in blocks; every second step sits on a hundred and is labelled. */
    private static final double LADDER_STEP_BLOCKS = 50.0D;
    /** Furthest ladder mark drawn; marks past the rim are clipped by the opening. */
    private static final double LADDER_MAX_BLOCKS = 600.0D;

    /**
     * Last ladder built and the ballistics it was built for. A crew rides one gun at a time, so the
     * table is derived once per weapon change and reused after that: the per frame cost stays one
     * loop over twelve {@link GunSightMath.Mark} entries, with nothing allocated while the sight is up.
     */
    private static double cachedSpeed = Double.NaN;
    private static double cachedGravity = Double.NaN;
    private static List<GunSightMath.Mark> cachedLadder = List.of();

    private GunSightDraw() {
    }

    /**
     * The whole sight picture for one frame, drawn over the world: the tube mask and its rim first,
     * then the ladder and the aiming marks inside the opening.
     *
     * @param baseFovDeg    the player's configured vertical field of view, before the sight zoom
     * @param fovMultiplier the zoom the interior camera applies, read from the config
     * @return the tube that was drawn, so the caller can place its readouts in the dark bands
     */
    public static GunSightMath.Tube sight(GuiGraphics graphics, Font font, int width, int height,
                                          double baseFovDeg, double fovMultiplier,
                                          int centreX, int centreY) {
        return sight(graphics, font, width, height, baseFovDeg, fovMultiplier, centreX, centreY,
                MainGunShellBallistics.MUZZLE_SPEED, MainGunShellBallistics.GRAVITY);
    }

    /**
     * As above, but with the ladder calibrated to the shell this sight is laid for: the same range
     * sits on a different mil mark for a 25mm round than for the 120mm dart, so the optic has to be
     * told which gun it belongs to.
     *
     * @param shellSpeedBlocksPerTick the shell's muzzle speed, in blocks per tick
     * @param gravityPerTick          the shell's gravity, in blocks per tick squared
     */
    public static GunSightMath.Tube sight(GuiGraphics graphics, Font font, int width, int height,
                                          double baseFovDeg, double fovMultiplier,
                                          int centreX, int centreY,
                                          double shellSpeedBlocksPerTick, double gravityPerTick) {
        GunSightMath.Tube tube = GunSightMath.tube(width, height, fovMultiplier);
        drawTube(graphics, tube);
        GunSightMath.Rect opening = tube.opening();
        if (opening.empty() || opening.width() < 3 * CENTRE_GAP_PIXELS) {
            return tube;
        }
        // The wires are pinned inside the opening, so the aim point can never drag them onto the mask
        // and the ladder always hangs off the gun line rather than off the middle of the frame.
        int x = clamp(centreX, opening.x() + CENTRE_GAP_PIXELS,
                opening.right() - CENTRE_GAP_PIXELS - 1);
        int y = clamp(centreY, opening.y() + CENTRE_GAP_PIXELS,
                opening.bottom() - CENTRE_GAP_PIXELS - 1);
        double pixelsPerMil = GunSightMath.pixelsPerMil(height, baseFovDeg, fovMultiplier);
        drawLadder(graphics, font, opening, x, y, pixelsPerMil,
                ladder(shellSpeedBlocksPerTick, gravityPerTick));
        drawWires(graphics, opening, x, y);
        drawCentreDot(graphics, x, y);
        drawTargetBox(graphics, opening, x, y);
        return tube;
    }

    /** Draws the MBT's optical line of sight and angular ruler inside the lens. */
    public static GunSightMath.Tube tankSight(GuiGraphics graphics, int width, int height,
            double fovMultiplier, double focalLengthPixels) {
        GunSightMath.Tube tube = GunSightMath.tube(width, height, fovMultiplier);
        drawTube(graphics, tube);
        GunSightMath.Rect opening = tube.opening();
        int x = width / 2;
        int y = height / 2;
        if (!insideWithMargin(opening, x, y, TANK_WIRE_LENGTH + 1)) {
            return tube;
        }

        etchedLine(graphics, x - TANK_WIRE_LENGTH, y, x - TANK_CENTRE_GAP, y + 1,
                TANK_GRATICULE_COLOUR);
        etchedLine(graphics, x + TANK_CENTRE_GAP + 1, y, x + TANK_WIRE_LENGTH + 1, y + 1,
                TANK_GRATICULE_COLOUR);
        etchedLine(graphics, x, y - TANK_WIRE_LENGTH, x + 1, y - TANK_CENTRE_GAP,
                TANK_GRATICULE_COLOUR);
        etchedLine(graphics, x, y + TANK_CENTRE_GAP + 1, x + 1, y + TANK_WIRE_LENGTH + 1,
                TANK_GRATICULE_COLOUR);

        if (Double.isFinite(focalLengthPixels) && focalLengthPixels > 0.0D) {
            drawTankTicks(graphics, opening, x, y, focalLengthPixels);
        }
        return tube;
    }

    /** Places a pale chevron with its tip on the projected gun line. */
    public static void tankBore(GuiGraphics graphics, GunSightMath.Tube tube,
            AimProjection.Screen screen) {
        if (!projectedInside(tube, screen, 5)) {
            return;
        }
        int x = (int) Math.round(screen.x());
        int y = (int) Math.round(screen.y());
        for (int row = 0; row <= 3; row++) {
            etchedLine(graphics, x - row, y + row, x - row + 1, y + row + 1, TANK_BORE_COLOUR);
            if (row > 0) {
                etchedLine(graphics, x + row, y + row, x + row + 1, y + row + 1, TANK_BORE_COLOUR);
            }
        }
    }

    /** Draws the impact at its projected position if the whole marker fits inside the lens. */
    public static boolean tankImpact(GuiGraphics graphics, GunSightMath.Tube tube,
            AimProjection.Screen screen, boolean hitEntity) {
        if (!projectedInside(tube, screen, 7)) {
            return false;
        }
        int x = (int) Math.round(screen.x());
        int y = (int) Math.round(screen.y());
        if (hitEntity) {
            for (int side = -5; side <= 5; side += 10) {
                int left = Math.min(x + side, x + side / 2);
                int right = Math.max(x + side, x + side / 2) + 1;
                etchedLine(graphics, x + side, y - 5, x + side + 1, y + 6, IMPACT_COLOUR);
                etchedLine(graphics, left, y - 5, right, y - 4, IMPACT_COLOUR);
                etchedLine(graphics, left, y + 5, right, y + 6, IMPACT_COLOUR);
            }
        } else {
            etchedLine(graphics, x - 5, y, x - 1, y + 1, IMPACT_COLOUR);
            etchedLine(graphics, x + 2, y, x + 6, y + 1, IMPACT_COLOUR);
            etchedLine(graphics, x, y - 5, x + 1, y - 1, IMPACT_COLOUR);
            etchedLine(graphics, x, y + 2, x + 1, y + 6, IMPACT_COLOUR);
        }
        return true;
    }

    private static void drawTankTicks(GuiGraphics graphics, GunSightMath.Rect opening,
            int x, int y, double focalLengthPixels) {
        int previousOffset = TANK_CENTRE_GAP - 1;
        int limit = Math.min(Math.min(x - opening.x(), opening.right() - 1 - x),
                opening.bottom() - 1 - y) - 2;
        for (int mils = TANK_TICK_STEP_MILS; mils <= TANK_TICK_LIMIT_MILS;
                mils += TANK_TICK_STEP_MILS) {
            double pixels = Math.tan(mils / 1000.0D) * focalLengthPixels;
            if (pixels > limit) {
                break;
            }
            int offset = (int) Math.round(pixels);
            if (offset - previousOffset < 3) {
                continue;
            }
            int half = mils % (2 * TANK_TICK_STEP_MILS) == 0 ? 3 : 1;
            etchedLine(graphics, x - offset, y - half, x - offset + 1, y + half + 1,
                    TANK_GRATICULE_COLOUR);
            etchedLine(graphics, x + offset, y - half, x + offset + 1, y + half + 1,
                    TANK_GRATICULE_COLOUR);
            etchedLine(graphics, x - half, y + offset, x + half + 1, y + offset + 1,
                    TANK_GRATICULE_COLOUR);
            previousOffset = offset;
        }
    }

    private static boolean projectedInside(GunSightMath.Tube tube, AimProjection.Screen screen,
            int margin) {
        return tube != null && screen != null && screen.visible()
                && insideWithMargin(tube.opening(), screen.x(), screen.y(), margin);
    }

    private static boolean insideWithMargin(GunSightMath.Rect opening, double x, double y,
            int margin) {
        return x >= opening.x() + margin && x <= opening.right() - 1 - margin
                && y >= opening.y() + margin && y <= opening.bottom() - 1 - margin;
    }

    private static void etchedLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colour) {
        fillBox(graphics, x1 + 1, y1 + 1, x2 + 1, y2 + 1, TANK_LINE_SHADOW);
        fillBox(graphics, x1, y1, x2, y2, colour);
    }

    /** Four dark bands plus the rim: the whole tube, and nothing outside the viewport. */
    private static void drawTube(GuiGraphics graphics, GunSightMath.Tube tube) {
        fillBox(graphics, tube.top(), MASK_COLOUR);
        fillBox(graphics, tube.bottom(), MASK_COLOUR);
        fillBox(graphics, tube.left(), MASK_COLOUR);
        fillBox(graphics, tube.right(), MASK_COLOUR);
        GunSightMath.Rect opening = tube.opening();
        if (opening.empty()) {
            return;
        }
        // The rim is drawn on the mask side of the opening, so it reads as the tube's inner edge and
        // can never eat into the sight picture itself.
        fillBox(graphics, opening.x(), opening.y() - RIM_THICKNESS,
                opening.right(), opening.y(), RIM_COLOUR);
        fillBox(graphics, opening.x(), opening.bottom(),
                opening.right(), opening.bottom() + RIM_THICKNESS, RIM_COLOUR);
        fillBox(graphics, opening.x() - RIM_THICKNESS, opening.y(),
                opening.x(), opening.bottom(), RIM_COLOUR);
        fillBox(graphics, opening.right(), opening.y(),
                opening.right() + RIM_THICKNESS, opening.bottom(), RIM_COLOUR);
    }

    /**
     * The crosshair: a full height vertical wire and a full width horizontal one, both broken by the
     * centre gap so the mark never hides the target it is laid on.
     */
    private static void drawWires(GuiGraphics graphics, GunSightMath.Rect opening, int x, int y) {
        fillBox(graphics, x, opening.y(), x + 1, y - CENTRE_GAP_PIXELS, RETICLE_COLOUR);
        fillBox(graphics, x, y + CENTRE_GAP_PIXELS, x + 1, opening.bottom(), RETICLE_COLOUR);
        fillBox(graphics, opening.x(), y, x - CENTRE_GAP_PIXELS, y + 1, RETICLE_COLOUR);
        fillBox(graphics, x + CENTRE_GAP_PIXELS, y, opening.right(), y + 1, RETICLE_COLOUR);
    }

    /** Solid 3x3 aim point: the armour piercing round's point of aim. */
    private static void drawCentreDot(GuiGraphics graphics, int x, int y) {
        int half = CENTRE_DOT_SIZE / 2;
        fillBox(graphics, x - half, y - half, x - half + CENTRE_DOT_SIZE,
                y - half + CENTRE_DOT_SIZE, CENTRE_COLOUR);
    }

    /** Square bracket around the aim point; skipped when the opening cannot hold it whole. */
    private static void drawTargetBox(GuiGraphics graphics, GunSightMath.Rect opening, int x, int y) {
        int left = x - TARGET_BOX_HALF;
        int right = x + TARGET_BOX_HALF;
        int top = y - TARGET_BOX_HALF;
        int bottom = y + TARGET_BOX_HALF;
        if (left < opening.x() || right > opening.right()
                || top < opening.y() || bottom > opening.bottom()) {
            return;
        }
        fillBox(graphics, left, top, right, top + 1, RETICLE_COLOUR);
        fillBox(graphics, left, bottom, right, bottom + 1, RETICLE_COLOUR);
        fillBox(graphics, left, top, left + 1, bottom + 1, RETICLE_COLOUR);
        fillBox(graphics, right, top, right + 1, bottom + 1, RETICLE_COLOUR);
    }

    /**
     * The drop ladder: one tick per step below the aim point, every hundred carrying its range in
     * plain digits. Marks reach only as far as the rim, because the wire they hang on ends there.
     */
    private static void drawLadder(GuiGraphics graphics, Font font, GunSightMath.Rect opening,
                                   int x, int y, double pixelsPerMil,
                                   List<GunSightMath.Mark> ladder) {
        if (pixelsPerMil <= 0.0D) {
            return;
        }
        for (GunSightMath.Mark mark : ladder) {
            int markY = y + GunSightMath.markOffsetPixels(mark.mils(), pixelsPerMil);
            if (markY <= y || markY >= opening.bottom()) {
                // The ladder only grows downwards, so once a mark is past the rim every later one is
                // further past it too: stop instead of drawing off the optic.
                break;
            }
            int half = mark.hundred() ? TICK_HALF_LABELLED : TICK_HALF_PLAIN;
            fillBox(graphics, x - half, markY, x + half + 1, markY + 1, LADDER_COLOUR);
            if (mark.hundred()) {
                String label = Integer.toString((int) Math.round(mark.rangeBlocks()));
                graphics.drawString(font, label, x + half + 4, markY - 4, LADDER_COLOUR, false);
            }
        }
    }

    /** The ladder table for one shell, rebuilt only when that shell's own ballistics change. */
    private static List<GunSightMath.Mark> ladder(double speedBlocksPerTick, double gravityPerTick) {
        if (speedBlocksPerTick != cachedSpeed || gravityPerTick != cachedGravity) {
            cachedLadder = GunSightMath.ladderMarks(LADDER_STEP_BLOCKS, LADDER_MAX_BLOCKS,
                    speedBlocksPerTick, gravityPerTick);
            cachedSpeed = speedBlocksPerTick;
            cachedGravity = gravityPerTick;
        }
        return cachedLadder;
    }

    /** Fills a rectangle, normalising the corners and skipping anything empty. */
    private static void fillBox(GuiGraphics graphics, int x1, int y1, int x2, int y2, int colour) {
        if (x1 == x2 || y1 == y2) {
            return;
        }
        graphics.fill(Math.min(x1, x2), Math.min(y1, y2), Math.max(x1, x2), Math.max(y1, y2), colour);
    }

    /** Fills a {@link GunSightMath.Rect}; empty rectangles are skipped. */
    private static void fillBox(GuiGraphics graphics, GunSightMath.Rect rect, int colour) {
        if (rect == null || rect.empty()) {
            return;
        }
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), colour);
    }

    private static int clamp(int value, int low, int high) {
        return Math.max(low, Math.min(high, value));
    }

    /**
     * The fire control marker: where the round in the barrel would land, drawn relative to the aim
     * point in mils so it rides the sight's own zoom.
     *
     * <p>A bracket rather than a dot, because it has to stay readable against a tank hull, and a
     * dotted leader back up to the aim point so a long holdover still reads as one sight picture
     * instead of two unrelated marks. The marker is clamped inside the tube: a shot that lands off
     * the edge of the optic parks on the edge rather than drawing over the mask.
     */
    public static void fireControl(GuiGraphics graphics, Font font, GunSightMath.Tube tube,
            int centreX, int centreY, double pixelsPerMil, double rightMils, double downMils,
            String label) {
        GunSightMath.Rect opening = tube == null ? null : tube.opening();
        boolean framed = opening != null && !opening.empty();
        double x = centreX + rightMils * pixelsPerMil;
        double y = centreY + downMils * pixelsPerMil;
        if (framed) {
            x = Math.max(opening.x() + MARKER_MARGIN, Math.min(opening.right() - MARKER_MARGIN, x));
            y = Math.max(opening.y() + MARKER_MARGIN, Math.min(opening.bottom() - MARKER_MARGIN, y));
        }
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            return;
        }
        int mx = (int) Math.round(x);
        int my = (int) Math.round(y);
        for (int dot = centreY + CENTRE_GAP_PIXELS + 6; dot < my - 4; dot += 6) {
            fillBox(graphics, mx, dot, mx + 1, dot + 2, IMPACT_COLOUR);
        }
        fillBox(graphics, mx - 6, my, mx - 2, my + 1, IMPACT_COLOUR);
        fillBox(graphics, mx + 3, my, mx + 7, my + 1, IMPACT_COLOUR);
        fillBox(graphics, mx, my - 6, mx + 1, my - 2, IMPACT_COLOUR);
        fillBox(graphics, mx, my + 3, mx + 1, my + 7, IMPACT_COLOUR);
        fillBox(graphics, mx - 1, my - 1, mx + 2, my + 2, IMPACT_COLOUR);
        if (font == null || label == null || label.isEmpty()) {
            return;
        }
        int labelY = my + 10;
        if (framed) {
            labelY = Math.min(opening.bottom() - 10, Math.max(opening.y() + 2, labelY));
        }
        graphics.drawString(font, label, mx - font.width(label) / 2, labelY, IMPACT_COLOUR, false);
    }
}
