package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.MainGunShellBallistics;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tank gun sight geometry: the tube opening, the mil scale and the drop ladder. */
class GunSightMathTest {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    @Test
    void tubeTilesTheViewportWithoutLeavingTheScreen() {
        GunSightMath.Tube tube = GunSightMath.tube(WIDTH, HEIGHT, 0.55D);
        int covered = area(tube.opening()) + area(tube.top()) + area(tube.bottom())
                + area(tube.left()) + area(tube.right());
        assertEquals(WIDTH * HEIGHT, covered, "the bands and the opening tile the frame exactly");
        for (GunSightMath.Rect rect : new GunSightMath.Rect[] {
                tube.opening(), tube.top(), tube.bottom(), tube.left(), tube.right()}) {
            assertTrue(rect.width() >= 0 && rect.height() >= 0, "no negative rectangle");
            assertTrue(rect.x() >= 0 && rect.y() >= 0, "no rectangle starts off screen");
            assertTrue(rect.right() <= WIDTH && rect.bottom() <= HEIGHT, "no rectangle runs off screen");
        }
        assertEquals(WIDTH / 2.0D, tube.opening().x() + tube.opening().width() / 2.0D, 0.5D);
        assertEquals(HEIGHT / 2.0D, tube.opening().y() + tube.opening().height() / 2.0D, 0.5D);
    }

    @Test
    void strongerZoomOpensTheTubeWider() {
        int strongest = GunSightMath.tube(WIDTH, HEIGHT, 0.20D).opening().width();
        int shipped = GunSightMath.tube(WIDTH, HEIGHT, 0.55D).opening().width();
        int plain = GunSightMath.tube(WIDTH, HEIGHT, 1.00D).opening().width();
        assertEquals(562, strongest);
        assertEquals(656, shipped);
        assertEquals(778, plain);
        assertTrue(strongest < shipped && shipped < plain, "smaller multiplier means a wider opening");
        assertTrue(plain <= Math.min(WIDTH, HEIGHT) - 2 * GunSightMath.MIN_MASK_BAND_PIXELS,
                "the opening always keeps the HUD its band");
    }

    @Test
    void tubeSurvivesDegenerateViewportsAndBadMultipliers() {
        GunSightMath.Tube none = GunSightMath.tube(0, HEIGHT, 0.55D);
        assertTrue(none.opening().empty() && none.top().empty() && none.left().empty());
        GunSightMath.Tube tiny = GunSightMath.tube(4, 4, 0.55D);
        assertEquals(16, area(tiny.opening()), "a window too small for a tube masks nothing");
        GunSightMath.Tube nan = GunSightMath.tube(WIDTH, HEIGHT, Double.NaN);
        assertEquals(GunSightMath.tube(WIDTH, HEIGHT, 1.0D).opening().width(), nan.opening().width());
        assertEquals(1.0D, GunSightMath.clampMultiplier(Double.NaN), 1.0E-9D);
        assertEquals(1.0D, GunSightMath.clampMultiplier(0.0D), 1.0E-9D);
        assertEquals(1.0D, GunSightMath.clampMultiplier(Double.POSITIVE_INFINITY), 1.0E-9D);
        assertEquals(0.20D, GunSightMath.clampMultiplier(0.01D), 1.0E-9D);
        assertEquals(1.50D, GunSightMath.clampMultiplier(9.0D), 1.0E-9D);
        assertEquals(GunSightMath.WIDEST_OPENING_FRACTION,
                GunSightMath.openingFraction(Double.NaN), 1.0E-9D);
    }

    @Test
    void pixelsPerMilFollowsTheMilDefinition() {
        // 180/pi degrees is exactly 1000 mils, so 1000 pixels are one pixel per mil.
        double oneMilField = 180.0D / Math.PI;
        assertEquals(1.0D, GunSightMath.pixelsPerMil(1000, oneMilField), 1.0E-9D);
        // 90 degrees is 1570.7963 mils, which one pixel per mil turns into 1570.7963 pixels.
        double ninetyDegreesInMils = 90.0D * (Math.PI / 180.0D) * 1000.0D;
        assertEquals(1570.7963267948966D, ninetyDegreesInMils, 1.0E-9D);
        assertEquals(ninetyDegreesInMils, 1571.0D / GunSightMath.pixelsPerMil(1571, 90.0D), 1.0E-9D);
        // 1080 pixels over a 70 degree field is 1221.73 mils, so 0.884 pixels per mil.
        assertEquals(0.883992D, GunSightMath.pixelsPerMil(1080, 70.0D), 1.0E-6D);
        assertTrue(GunSightMath.pixelsPerMil(1080, 35.0D) > GunSightMath.pixelsPerMil(1080, 70.0D),
                "a narrower field magnifies each mil");
        assertEquals(0.0D, GunSightMath.pixelsPerMil(0, 70.0D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.pixelsPerMil(1080, -1.0D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.pixelsPerMil(1080, Double.NaN), 1.0E-9D);
    }

    @Test
    void pixelsPerMilUsesTheSightZoom() {
        assertEquals(GunSightMath.pixelsPerMil(1080, 38.5D),
                GunSightMath.pixelsPerMil(1080, 70.0D, 0.55D), 1.0E-9D);
        assertEquals(GunSightMath.pixelsPerMil(1080, 70.0D),
                GunSightMath.pixelsPerMil(1080, 70.0D, 1.0D), 1.0E-9D);
        assertEquals(GunSightMath.pixelsPerMil(1080, 70.0D, 1.0D),
                GunSightMath.pixelsPerMil(1080, 70.0D, Double.NaN), 1.0E-9D);
    }

    @Test
    void dropMilsMatchesTheShellFlightModel() {
        // Integrate the shell's own step rule independently: 100 blocks at 5 blocks per tick is 20
        // ticks, and the round has fallen 4.2 blocks by then.
        double velocity = 0.0D;
        double drop = 0.0D;
        for (int tick = 0; tick < 20; tick++) {
            velocity -= MainGunShellBallistics.GRAVITY;
            drop += velocity;
        }
        assertEquals(4.2D, -drop, 1.0E-9D);
        assertEquals(42.0D, GunSightMath.dropMils(100.0D), 1.0E-9D);
        assertEquals(1000.0D * (-drop) / 100.0D, GunSightMath.dropMils(100.0D), 1.0E-9D);
        // The ladder's closed form for the shipped muzzle speed and gravity: mils = 0.4 * range + 2.
        assertEquals(22.0D, GunSightMath.dropMils(50.0D), 1.0E-9D);
        assertEquals(202.0D, GunSightMath.dropMils(500.0D), 1.0E-9D);
        assertEquals(402.0D, GunSightMath.dropMils(1000.0D), 1.0E-9D);
        assertTrue(GunSightMath.dropMils(200.0D) > GunSightMath.dropMils(100.0D),
                "a longer shot drops more mils");
    }

    @Test
    void dropMilsIsSafeForBadRanges() {
        assertEquals(0.0D, GunSightMath.dropMils(0.0D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(-12.0D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(Double.NaN), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(Double.POSITIVE_INFINITY), 1.0E-9D);
    }

    @Test
    void ladderMarksCoverTheRangesAndFlagTheHundreds() {
        List<GunSightMath.Mark> marks = GunSightMath.ladderMarks(50.0D, 600.0D);
        assertEquals(12, marks.size());
        assertEquals(50.0D, marks.get(0).rangeBlocks(), 1.0E-9D);
        assertEquals(22.0D, marks.get(0).mils(), 1.0E-9D);
        assertFalse(marks.get(0).hundred(), "50 metres is a plain tick");
        assertEquals(600.0D, marks.get(11).rangeBlocks(), 1.0E-9D);
        for (GunSightMath.Mark mark : marks) {
            boolean expected = mark.rangeBlocks() % 100.0D == 0.0D;
            assertEquals(expected, mark.hundred(), "hundred flag at " + mark.rangeBlocks());
        }
        GunSightMath.Mark hundred = marks.get(1);
        assertEquals(100.0D, hundred.rangeBlocks(), 1.0E-9D);
        assertEquals(42.0D, hundred.mils(), 1.0E-9D);
        assertTrue(hundred.hundred());
        for (int index = 1; index < marks.size(); index++) {
            assertTrue(marks.get(index).mils() > marks.get(index - 1).mils(), "mils grow with range");
        }
    }

    @Test
    void ladderMarksAreSafeAndBounded() {
        assertTrue(GunSightMath.ladderMarks(0.0D, 600.0D).isEmpty());
        assertTrue(GunSightMath.ladderMarks(-50.0D, 600.0D).isEmpty());
        assertTrue(GunSightMath.ladderMarks(50.0D, 0.0D).isEmpty());
        assertTrue(GunSightMath.ladderMarks(50.0D, -1.0D).isEmpty());
        assertTrue(GunSightMath.ladderMarks(Double.NaN, 600.0D).isEmpty());
        assertTrue(GunSightMath.ladderMarks(50.0D, Double.POSITIVE_INFINITY).isEmpty());
        assertTrue(GunSightMath.ladderMarks(Double.POSITIVE_INFINITY, 600.0D).isEmpty());
        assertEquals(GunSightMath.MAX_LADDER_MARKS,
                GunSightMath.ladderMarks(1.0D, 1.0E9D).size(), "an absurd step cannot flood the HUD");
    }

    @Test
    void markOffsetRoundsAndRejectsBadInput() {
        assertEquals(67, GunSightMath.markOffsetPixels(42.0D, 1.6D));
        assertEquals(0, GunSightMath.markOffsetPixels(42.0D, 0.0D));
        assertEquals(0, GunSightMath.markOffsetPixels(0.0D, 1.6D));
        assertEquals(0, GunSightMath.markOffsetPixels(-42.0D, 1.6D));
        assertEquals(0, GunSightMath.markOffsetPixels(Double.NaN, 1.6D));
        assertEquals(0, GunSightMath.markOffsetPixels(42.0D, Double.NaN));
        assertEquals(0, GunSightMath.markOffsetPixels(Double.POSITIVE_INFINITY, 1.6D));
        assertEquals(0, GunSightMath.markOffsetPixels(0.1D, 1.0D), "sub pixel marks are dropped");
    }

    @Test
    void insideOpeningIsHalfOpen() {
        GunSightMath.Rect opening = new GunSightMath.Rect(10, 20, 100, 50);
        assertTrue(GunSightMath.insideOpening(opening, 10, 20));
        assertTrue(GunSightMath.insideOpening(opening, 109, 69));
        assertFalse(GunSightMath.insideOpening(opening, 110, 69));
        assertFalse(GunSightMath.insideOpening(opening, 109, 70));
        assertFalse(GunSightMath.insideOpening(opening, 9, 20));
        assertFalse(GunSightMath.insideOpening(null, 10, 20));
        assertFalse(GunSightMath.insideOpening(new GunSightMath.Rect(0, 0, 0, 0), 0, 0));
    }

    @Test
    void autocannonDropUsesTheAutocannonSpeed() {
        // 25mm at the shipped 6 blocks per tick: 100 blocks takes 16.67 ticks and falls 2.94 blocks,
        // which is 29.4 mils - not the 42 the 120mm dart needs for the same range.
        double ticks = 100.0D / 6.0D;
        double drop = 0.02D * 0.5D * ticks * (ticks + 1.0D);
        assertEquals(2.944444444444444D, drop, 1.0E-9D);
        assertEquals(29.444444444444443D, GunSightMath.dropMils(100.0D, 6.0D, 0.02D), 1.0E-9D);
        assertEquals(1000.0D * drop / 100.0D, GunSightMath.dropMils(100.0D, 6.0D, 0.02D), 1.0E-9D);
        assertEquals(29.4D, GunSightMath.dropMils(100.0D, 6.0D, 0.02D), 0.05D);
        // The slow setting a server can still pick: 2 blocks per tick is 50 ticks and 25.5 blocks of
        // drop at the same range, so the ladder has to follow the configured speed.
        assertEquals(255.0D, GunSightMath.dropMils(100.0D, 2.0D, 0.02D), 1.0E-9D);
        assertEquals(168.33333333333334D, GunSightMath.dropMils(600.0D, 6.0D, 0.02D), 1.0E-9D);
        assertTrue(GunSightMath.dropMils(100.0D, 6.0D, 0.02D) < GunSightMath.dropMils(100.0D),
                "the faster 25mm round needs fewer mils at 100 blocks than the 120mm dart");
    }

    @Test
    void dropMilsRejectsBadBallistics() {
        assertEquals(0.0D, GunSightMath.dropMils(100.0D, 0.0D, 0.02D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(100.0D, -6.0D, 0.02D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(100.0D, Double.NaN, 0.02D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(100.0D, 6.0D, 0.0D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(100.0D, 6.0D, -0.02D), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(100.0D, 6.0D, Double.POSITIVE_INFINITY), 1.0E-9D);
        assertEquals(0.0D, GunSightMath.dropMils(Double.POSITIVE_INFINITY, 6.0D, 0.02D), 1.0E-9D);
    }

    @Test
    void ladderMarksFollowTheirOwnBallistics() {
        List<GunSightMath.Mark> autocannon = GunSightMath.ladderMarks(50.0D, 600.0D, 6.0D, 0.02D);
        assertEquals(12, autocannon.size());
        assertEquals(15.555555555555557D, autocannon.get(0).mils(), 1.0E-9D);
        assertEquals(29.444444444444443D, autocannon.get(1).mils(), 1.0E-9D);
        assertEquals(168.33333333333334D, autocannon.get(11).mils(), 1.0E-9D);
        assertTrue(autocannon.get(1).hundred(), "100 blocks is still a labelled hundred");
        assertTrue(autocannon.get(0).mils() < GunSightMath.ladderMarks(50.0D, 600.0D).get(0).mils(),
                "the two guns need different marks for the same range");
        assertTrue(GunSightMath.ladderMarks(50.0D, 600.0D, 0.0D, 0.02D).isEmpty(),
                "a sight with no ballistics draws no ladder");
    }

    @Test
    void heavyGunDelegationStaysIdentical() {
        assertEquals(GunSightMath.dropMils(100.0D, 5.0D, 0.02D), GunSightMath.dropMils(100.0D),
                1.0E-12D);
        List<GunSightMath.Mark> shipped = GunSightMath.ladderMarks(50.0D, 600.0D);
        List<GunSightMath.Mark> explicit =
                GunSightMath.ladderMarks(50.0D, 600.0D, MainGunShellBallistics.MUZZLE_SPEED,
                        MainGunShellBallistics.GRAVITY);
        assertEquals(shipped.size(), explicit.size());
        for (int index = 0; index < shipped.size(); index++) {
            assertEquals(shipped.get(index).rangeBlocks(), explicit.get(index).rangeBlocks(),
                    1.0E-12D);
            assertEquals(shipped.get(index).mils(), explicit.get(index).mils(), 1.0E-12D);
            assertEquals(shipped.get(index).hundred(), explicit.get(index).hundred());
        }
    }

    private static int area(GunSightMath.Rect rect) {
        return rect.width() * rect.height();
    }
}
