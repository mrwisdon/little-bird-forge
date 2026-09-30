package io.github.littlebird.client;

import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import java.util.ArrayList;
import java.util.List;

/**
 * Geometry and ballistic rules behind the tank's optical gun sight, kept free of Minecraft classes
 * so the tube opening, the mil scale and the drop ladder are pinned by unit tests instead of being
 * eyeballed in game.
 *
 * <p>Every value is derived from the viewport size and the shell's flight model alone: nothing here
 * integrates time, smooths a previous frame or draws a random number, which is what makes the sight
 * read as a rigid optic parked on the barrel instead of a HUD overlay that drifts.
 */
public final class GunSightMath {
    /** Zoom the config ships with, and the reference multiplier the unit tests use. */
    public static final double DEFAULT_FOV_MULTIPLIER = 0.55D;
    /** Strongest magnification the config allows. */
    public static final double MIN_FOV_MULTIPLIER = 0.20D;
    /** Widest setting the config allows, that is the plain unmagnified view. */
    public static final double MAX_FOV_MULTIPLIER = 1.50D;
    /** Tube opening as a fraction of the viewport's short side at the widest setting. */
    public static final double WIDEST_OPENING_FRACTION = 0.72D;
    /** Tube opening as a fraction of the viewport's short side at the strongest magnification. */
    public static final double NARROWEST_OPENING_FRACTION = 0.52D;
    /**
     * Dark band always left between the opening and the screen edge. The hull condition and weapon
     * panels live in the corners, so the tube must never grow past them: this is that guard.
     */
    public static final int MIN_MASK_BAND_PIXELS = 6;
    /** Hard cap on the ladder, so a caller asking for one mark per block cannot flood the HUD. */
    public static final int MAX_LADDER_MARKS = 64;

    private GunSightMath() {
    }

    /** An axis aligned GUI rectangle; width and height are never negative. */
    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }

        public boolean empty() {
            return width <= 0 || height <= 0;
        }
    }

    /**
     * One frame's sight picture: the square opening the optic looks through plus the four dark bands
     * filling the rest of the viewport. Together they tile the whole screen exactly, so a rider never
     * sees a seam between the tube and the world.
     */
    public record Tube(Rect opening, Rect top, Rect bottom, Rect left, Rect right) {
    }

    /** One ladder entry: the range it stands for, that range's drop in mils, and whether it is a hundred. */
    public record Mark(double rangeBlocks, double mils, boolean hundred) {
    }

    /**
     * Normalises the configured zoom. A non-finite or non-positive value means there is no sight at
     * all, so it falls back to the plain 1.0 view rather than producing a meaningless tube; anything
     * outside the config's own range is clamped to its ends so the opening can never invert.
     */
    public static double clampMultiplier(double fovMultiplier) {
        if (!Double.isFinite(fovMultiplier) || fovMultiplier <= 0.0D) {
            return 1.0D;
        }
        return Math.max(MIN_FOV_MULTIPLIER, Math.min(MAX_FOV_MULTIPLIER, fovMultiplier));
    }

    /**
     * Fraction of the viewport's short side the tube opening takes up. A smaller multiplier is a
     * stronger zoom and shows a larger opening; 1.0 and anything wider get the widest opening, the
     * strongest zoom gets the narrowest.
     */
    public static double openingFraction(double fovMultiplier) {
        double zoom = clampMultiplier(fovMultiplier);
        double strength = (1.0D - zoom) / (1.0D - MIN_FOV_MULTIPLIER);
        strength = Math.max(0.0D, Math.min(1.0D, strength));
        return WIDEST_OPENING_FRACTION
                + (NARROWEST_OPENING_FRACTION - WIDEST_OPENING_FRACTION) * strength;
    }

    /**
     * Builds the tube for one frame. The opening is the largest square that fits the viewport's short
     * side at {@link #openingFraction}, minus the guaranteed dark band, and it is centred, so the
     * sight can never push a HUD panel off the screen nor leave a hole at the frame's edge.
     *
     * @return the opening and the four bands around it; all empty for a viewport with no area
     */
    public static Tube tube(int width, int height, double fovMultiplier) {
        Rect none = new Rect(0, 0, 0, 0);
        if (width <= 0 || height <= 0) {
            return new Tube(none, none, none, none, none);
        }
        int shortSide = Math.min(width, height);
        int maxOpening = shortSide - 2 * MIN_MASK_BAND_PIXELS;
        if (maxOpening < 2) {
            // A window too small to hold a tube: the whole frame is the opening and nothing is masked.
            return new Tube(new Rect(0, 0, width, height), none, none, none, none);
        }
        int side = (int) Math.round(shortSide * openingFraction(fovMultiplier));
        side = Math.max(2, Math.min(side, maxOpening));
        int left = (width - side) / 2;
        int top = (height - side) / 2;
        return new Tube(
                new Rect(left, top, side, side),
                new Rect(0, 0, width, top),
                new Rect(0, top + side, width, height - top - side),
                new Rect(0, top, left, side),
                new Rect(left + side, top, width - left - side, side));
    }

    /**
     * Pixels covered by one angular mil of the view, the scale the drop ladder is drawn on.
     *
     * <p>Derivation: one mil is a thousandth of a radian and a full turn is 360 degrees, so a
     * vertical field of {@code verticalFovDeg} degrees spans
     * {@code verticalFovDeg * (pi / 180) * 1000} mils. Dividing the viewport height by that count
     * gives {@code height * 180 / (verticalFovDeg * pi * 1000)} pixels per mil; at 1080 pixels and a
     * 70 degree field that is 1080 / 1221.73 = 0.884 pixels.
     *
     * @return pixels per mil, or 0 when the height or the field of view is unusable
     */
    public static double pixelsPerMil(int height, double verticalFovDeg) {
        if (height <= 0 || !Double.isFinite(verticalFovDeg) || verticalFovDeg <= 0.0D) {
            return 0.0D;
        }
        double mils = verticalFovDeg * (Math.PI / 180.0D) * 1000.0D;
        return height / mils;
    }

    /**
     * As {@link #pixelsPerMil(int, double)} but for a camera whose field of view the sight zoom has
     * already scaled. The interior camera renders at {@code baseFovDeg * fovMultiplier}, so reading
     * the ladder scale off the unscaled option would stretch every mark by one over the multiplier.
     */
    public static double pixelsPerMil(int height, double baseFovDeg, double fovMultiplier) {
        return pixelsPerMil(height, baseFovDeg * clampMultiplier(fovMultiplier));
    }

    /**
     * Drop of the 120mm main gun at a given range, in mils. Kept as the tank sight's entry point, and
     * delegated to the general form with the dart's own ballistics so the 120mm ladder stays bit for
     * bit what it was before the 25mm sight existed.
     *
     * @return drop in mils, or 0 for a range that is not positive and finite
     */
    public static double dropMils(double rangeBlocks) {
        return dropMils(rangeBlocks, MainGunShellBallistics.MUZZLE_SPEED,
                MainGunShellBallistics.GRAVITY);
    }

    /**
     * Drop of a shell at a given range, in mils, from that shell's own flight model.
     *
     * <p>{@code advance} pulls gravity first and moves second, so after n ticks of level flight a
     * round has fallen {@code gravity / 2 * n * (n + 1)} blocks; a range is covered at {@code speed}
     * blocks per tick, so {@code n = range / speed}. One mil at that range subtends
     * {@code range / 1000} blocks, hence {@code mils = 1000 * drop / range}, and at 500 blocks the
     * 120mm dart falls 101 blocks, which is 202 mils.
     *
     * <p>The speed matters as much as the gravity: the same 100 block shot puts the 120mm dart 42
     * mils low, the 25mm autocannon 29.4 mils low at its shipped 6 blocks per tick, and 255 mils low
     * at the 2 blocks per tick a server can still configure. A ladder copied between two guns is
     * therefore wrong by whole tick marks, which is why this takes the shell's numbers rather than a
     * shared constant.
     *
     * @return drop in mils, or 0 for a range, speed or gravity that is not positive and finite
     */
    public static double dropMils(double rangeBlocks, double speedBlocksPerTick,
                                  double gravityPerTick) {
        if (!Double.isFinite(rangeBlocks) || rangeBlocks <= 0.0D
                || !Double.isFinite(speedBlocksPerTick) || speedBlocksPerTick <= 0.0D
                || !Double.isFinite(gravityPerTick) || gravityPerTick <= 0.0D) {
            return 0.0D;
        }
        double ticks = rangeBlocks / speedBlocksPerTick;
        double drop = gravityPerTick * 0.5D * ticks * (ticks + 1.0D);
        return 1000.0D * drop / rangeBlocks;
    }

    /**
     * The 120mm drop ladder's tick table, kept as the tank sight's entry point and delegated to the
     * general form, so the drawn 120mm ladder is unchanged.
     *
     * @return the marks in increasing range, or an empty list for a step or range that is not
     *         positive and finite
     */
    public static List<Mark> ladderMarks(double stepBlocks, double maxRangeBlocks) {
        return ladderMarks(stepBlocks, maxRangeBlocks, MainGunShellBallistics.MUZZLE_SPEED,
                MainGunShellBallistics.GRAVITY);
    }

    /**
     * A shell's drop ladder: marks start at one step and stop at or below {@code maxRangeBlocks},
     * each carrying the drop that shell's own ballistics give it, and the list is capped at
     * {@link #MAX_LADDER_MARKS}. Shared by the renderer and the tests so the marks drawn for a gun
     * cannot disagree with the gun.
     *
     * @return the marks in increasing range, or an empty list for a step, range, speed or gravity
     *         that is not positive and finite
     */
    public static List<Mark> ladderMarks(double stepBlocks, double maxRangeBlocks,
                                         double speedBlocksPerTick, double gravityPerTick) {
        List<Mark> marks = new ArrayList<>();
        if (!Double.isFinite(stepBlocks) || stepBlocks <= 0.0D
                || !Double.isFinite(maxRangeBlocks) || maxRangeBlocks <= 0.0D
                || !Double.isFinite(speedBlocksPerTick) || speedBlocksPerTick <= 0.0D
                || !Double.isFinite(gravityPerTick) || gravityPerTick <= 0.0D) {
            return marks;
        }
        int count = (int) Math.floor(maxRangeBlocks / stepBlocks + 1.0E-9D);
        count = Math.max(0, Math.min(count, MAX_LADDER_MARKS));
        for (int index = 1; index <= count; index++) {
            double range = index * stepBlocks;
            marks.add(new Mark(range, dropMils(range, speedBlocksPerTick, gravityPerTick),
                    wholeHundred(range)));
        }
        return marks;
    }

    /** True when a range sits on a whole hundred, which is the mark that carries a label. */
    public static boolean wholeHundred(double rangeBlocks) {
        if (!Double.isFinite(rangeBlocks) || rangeBlocks <= 0.0D) {
            return false;
        }
        double hundreds = rangeBlocks / 100.0D;
        return Math.abs(hundreds - Math.round(hundreds)) < 1.0E-6D;
    }

    /**
     * How far below the aim point a ladder mark sits, in whole pixels. Rounded rather than truncated
     * so a mark cannot alternate between two rows as the viewport changes.
     *
     * @return the offset in pixels, or 0 for any input that is not a positive, finite pair
     */
    public static int markOffsetPixels(double mils, double pixelsPerMil) {
        if (!Double.isFinite(mils) || mils <= 0.0D
                || !Double.isFinite(pixelsPerMil) || pixelsPerMil <= 0.0D) {
            return 0;
        }
        double pixels = mils * pixelsPerMil;
        if (!Double.isFinite(pixels) || pixels < 1.0D) {
            return 0;
        }
        return (int) Math.round(pixels);
    }

    /** True when a GUI point falls inside an opening; a null or empty opening contains nothing. */
    public static boolean insideOpening(Rect opening, int x, int y) {
        return opening != null && !opening.empty()
                && x >= opening.x() && x < opening.right()
                && y >= opening.y() && y < opening.bottom();
    }
}
