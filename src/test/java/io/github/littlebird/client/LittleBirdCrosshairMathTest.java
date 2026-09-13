package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class LittleBirdCrosshairMathTest {
    private static final double EPSILON = 1.0E-9D;

    @Test
    void coldBarrelIsTighterThanAHotOne() {
        double cold = LittleBirdCrosshairMath.spreadRadiusBlocks(32.0D, 0.35D);
        double hot = LittleBirdCrosshairMath.spreadRadiusBlocks(32.0D, 1.10D);
        assertTrue(hot > cold);
        assertEquals(0.1955D, cold, 1.0E-3D, "tan(0.35 deg) * 32 blocks");
    }

    @Test
    void spreadRadiusGrowsWithDistance() {
        double near = LittleBirdCrosshairMath.spreadRadiusBlocks(16.0D, 1.0D);
        double far = LittleBirdCrosshairMath.spreadRadiusBlocks(64.0D, 1.0D);
        assertEquals(near * 4.0D, far, 1.0E-6D);
        assertEquals(0.0D, LittleBirdCrosshairMath.spreadRadiusBlocks(-5.0D, 1.0D), EPSILON);
        assertEquals(0.0D, LittleBirdCrosshairMath.spreadRadiusBlocks(32.0D, -1.0D), EPSILON);
    }

    @Test
    void armGapStaysInItsBand() {
        double gap = LittleBirdCrosshairMath.armGapPixels(100.0D, 100.0D, 100.5D, 100.0D);
        assertEquals(LittleBirdCrosshairMath.MIN_ARM_GAP_PIXELS, gap, EPSILON);
        assertEquals(10.0D, LittleBirdCrosshairMath.armGapPixels(0.0D, 0.0D, 10.0D, 0.0D),
                1.0E-6D);
        assertEquals(50.0D,
                LittleBirdCrosshairMath.armGapPixels(100.0D, 100.0D, 130.0D, 140.0D),
                1.0E-6D, "the gap tracks the dispersion cone, however wide it projects");
        assertEquals(LittleBirdCrosshairMath.MIN_ARM_GAP_PIXELS,
                LittleBirdCrosshairMath.armGapPixels(0.0D, 0.0D, Double.NaN, 0.0D), EPSILON);
    }

    @Test
    void reticleFollowsTheSelectedWeapon() {
        assertTrue(LittleBirdCrosshairMath.showsGunReticle(true, 0, true));
        assertFalse(LittleBirdCrosshairMath.showsGunReticle(true, 1, true));
        assertTrue(LittleBirdCrosshairMath.showsRocketReticle(true, 1, true));
        assertFalse(LittleBirdCrosshairMath.showsRocketReticle(true, 0, true));
        assertTrue(LittleBirdCrosshairMath.showsGunReticle(false, 1, true),
                "with weapon following off both reticles can show");
        assertFalse(LittleBirdCrosshairMath.showsGunReticle(false, 0, false),
                "the per-reticle switch still wins");
    }
    @Test
    void sevenTubesFillSevenDistinctCells() {
        java.util.Set<String> cells = new java.util.HashSet<>();
        for (int tube = 0; tube < LittleBirdCrosshairMath.TUBES_PER_POD; tube++) {
            LittleBirdCrosshairMath.DotPosition dot = LittleBirdCrosshairMath.tubeDot(tube);
            assertTrue(dot.column() >= 0 && dot.column() < LittleBirdCrosshairMath.DOT_COLUMNS);
            assertTrue(dot.row() >= 0 && dot.row() < LittleBirdCrosshairMath.DOT_ROWS);
            cells.add(dot.column() + ":" + dot.row());
        }
        assertEquals(LittleBirdCrosshairMath.TUBES_PER_POD, cells.size(),
                "every tube needs its own dot");
    }

    @Test
    void innerColumnSkipsItsTopCellLikeTheReference() {
        java.util.Set<String> cells = new java.util.HashSet<>();
        for (int tube = 0; tube < LittleBirdCrosshairMath.TUBES_PER_POD; tube++) {
            LittleBirdCrosshairMath.DotPosition dot = LittleBirdCrosshairMath.tubeDot(tube);
            cells.add(dot.column() + ":" + dot.row());
        }
        assertFalse(cells.contains("1:0"), "the reference leaves the inner top cell empty");
        assertTrue(cells.contains("0:0"), "the outer column starts at the top");
        assertTrue(cells.contains("0:3"), "the outer column holds four dots");
        assertTrue(cells.contains("1:3"), "the inner column ends at the bottom");
        // Which tube owns which cell is covered by dotsEmptyLeftToRightAndTopToBottom.
    }

    @Test
    void outOfRangeTubesAreRejected() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> LittleBirdCrosshairMath.tubeDot(-1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> LittleBirdCrosshairMath.tubeDot(7));
    }
    @Test
    void dotsEmptyAcrossEachRowBeforeMovingDown() {
        // Firing order is tube 0 first, so tube 6 owns the first dot to disappear.
        assertEquals(new LittleBirdCrosshairMath.DotPosition(0, 0),
                LittleBirdCrosshairMath.tubeDot(6), "top row, outer dot first");
        assertEquals(new LittleBirdCrosshairMath.DotPosition(0, 1),
                LittleBirdCrosshairMath.tubeDot(5), "then the next row, outer dot");
        assertEquals(new LittleBirdCrosshairMath.DotPosition(1, 1),
                LittleBirdCrosshairMath.tubeDot(4), "then that row's inner dot");
        assertEquals(new LittleBirdCrosshairMath.DotPosition(0, 2),
                LittleBirdCrosshairMath.tubeDot(3), "next row starts again on the left");
        assertEquals(new LittleBirdCrosshairMath.DotPosition(1, 2),
                LittleBirdCrosshairMath.tubeDot(2));
        assertEquals(new LittleBirdCrosshairMath.DotPosition(0, 3),
                LittleBirdCrosshairMath.tubeDot(1));
        assertEquals(new LittleBirdCrosshairMath.DotPosition(1, 3),
                LittleBirdCrosshairMath.tubeDot(0), "last dot: bottom row, inner");
    }

    @Test
    void sevenTubesStillFillSevenDistinctCells() {
        java.util.Set<String> cells = new java.util.HashSet<>();
        for (int tube = 0; tube < LittleBirdCrosshairMath.TUBES_PER_POD; tube++) {
            LittleBirdCrosshairMath.DotPosition dot = LittleBirdCrosshairMath.tubeDot(tube);
            cells.add(dot.column() + ":" + dot.row());
        }
        assertEquals(LittleBirdCrosshairMath.TUBES_PER_POD, cells.size());
        assertFalse(cells.contains("1:0"), "the inner top cell stays empty");
    }
    @Test
    void reticlesArePulledInsideTheFrameInsteadOfJumpingToTheCentre() {
        LittleBirdCrosshairMath.ScreenPoint inside =
                LittleBirdCrosshairMath.clampToFrame(100.0D, 100.0D, 200, 200);
        assertEquals(100.0D, inside.x(), EPSILON);
        assertEquals(100.0D, inside.y(), EPSILON);

        double margin = 200.0D * LittleBirdCrosshairMath.SCREEN_MARGIN_FRACTION;
        LittleBirdCrosshairMath.ScreenPoint far =
                LittleBirdCrosshairMath.clampToFrame(9000.0D, -9000.0D, 200, 200);
        assertEquals(200.0D - margin, far.x(), 1.0E-9D, "pegs at the right margin");
        assertEquals(margin, far.y(), 1.0E-9D, "pegs at the top margin");

        // Barely outside and far outside must land in exactly the same place: that is
        // what stops the drawn crosshair oscillating against the edge of the screen.
        assertEquals(far.x(), LittleBirdCrosshairMath.clampToFrame(
                201.0D, 100.0D, 200, 200).x(), 1.0E-9D);
        assertEquals(far.y(), LittleBirdCrosshairMath.clampToFrame(
                100.0D, -1.0D, 200, 200).y(), 1.0E-9D);

        assertEquals(margin, LittleBirdCrosshairMath.clampToFrame(
                Double.NaN, 100.0D, 200, 200).x(), 1.0E-9D, "garbage stays on screen");
        assertTrue(LittleBirdCrosshairMath.behindCamera(false));
        assertFalse(LittleBirdCrosshairMath.behindCamera(true));
    }

    @Test
    void theCentreCrossOpensUpWithHeat() {
        double cold = LittleBirdCrosshairMath.crosshairGapPixels(0.35D, 32.0D, 70.0D, 600);
        double hot = LittleBirdCrosshairMath.crosshairGapPixels(1.10D, 32.0D, 70.0D, 600);
        assertTrue(hot > cold, "a hot barrel must open the arms: " + cold + " -> " + hot);
        assertEquals(LittleBirdCrosshairMath.MIN_ARM_GAP_PIXELS,
                LittleBirdCrosshairMath.crosshairGapPixels(0.0D, 32.0D, 70.0D, 600), EPSILON);
        assertEquals(LittleBirdCrosshairMath.MIN_ARM_GAP_PIXELS,
                LittleBirdCrosshairMath.crosshairGapPixels(1.0D, 0.0D, 70.0D, 600), EPSILON);
        assertEquals(LittleBirdCrosshairMath.MIN_ARM_GAP_PIXELS,
                LittleBirdCrosshairMath.crosshairGapPixels(Double.NaN, 32.0D, 70.0D, 600),
                EPSILON);
        assertTrue(LittleBirdCrosshairMath.crosshairGapPixels(45.0D, 32.0D, 70.0D, 600)
                        <= LittleBirdCrosshairMath.MAX_CROSSHAIR_GAP_PIXELS,
                "however hot the barrel gets the cross stays readable");
    }

    @Test
    void smoothingApproachesWithoutOvershooting() {
        double value = 0.0D;
        for (int frame = 0; frame < 20; frame++) {
            value = LittleBirdCrosshairMath.smooth(value, 100.0D,
                    LittleBirdCrosshairMath.SMOOTHING_FACTOR);
        }
        assertEquals(100.0D, value, 0.01D);
        assertEquals(50.0D, LittleBirdCrosshairMath.smooth(0.0D, 100.0D, 0.5D), 1.0E-6D);
        assertEquals(0.0D, LittleBirdCrosshairMath.smooth(0.0D, 100.0D, -1.0D), 1.0E-6D);
        assertEquals(100.0D, LittleBirdCrosshairMath.smooth(0.0D, 100.0D, 5.0D), 1.0E-6D);
    }
}
