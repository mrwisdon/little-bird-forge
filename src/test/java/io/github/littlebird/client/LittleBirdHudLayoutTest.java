package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.client.LittleBirdHudLayout.Panels;
import io.github.littlebird.client.LittleBirdHudLayout.Rect;
import org.junit.jupiter.api.Test;

final class LittleBirdHudLayoutTest {
    private static final int[][] SCREENS = {{320, 240}, {854, 480}, {1920, 1080}, {2560, 1440}};

    @Test
    void headingTapeStaysCentredAtTheTop() {
        for (int[] screen : SCREENS) {
            Panels panels = LittleBirdHudLayout.compute(screen[0], screen[1], 2);
            Rect tape = panels.headingTape();
            assertEquals(screen[0] / 2, tape.x() + tape.width() / 2, "screen " + screen[0]);
            assertTrue(tape.y() >= 0);
            assertTrue(tape.right() <= screen[0], "tape must fit horizontally");
        }
    }

    @Test
    void weaponRowStaysInsideTheScreenAndBelowTheCrosshair() {
        for (int[] screen : SCREENS) {
            Panels panels = LittleBirdHudLayout.compute(screen[0], screen[1], 2);
            Rect heat = panels.heatBar();
            Rect grid = panels.rocketGrid();
            assertTrue(heat.x() >= 0, "heat bar must not clip on the left");
            assertTrue(grid.right() <= screen[0], "rocket grid must fit");
            assertTrue(heat.bottom() <= screen[1]);
            assertTrue(panels.bankIndicator().bottom() < heat.y(),
                    "bank indicator must stay above the weapon row");
        }
    }

    @Test
    void enemyHealthBarEmptiesFromTheRight() {
        int full = LittleBirdHudLayout.ENEMY_BAR_WIDTH;
        assertEquals(full, LittleBirdHudLayout.enemyHealthFillWidth(1.0F, full));
        assertEquals(full / 2, LittleBirdHudLayout.enemyHealthFillWidth(0.5F, full));
        assertEquals(0, LittleBirdHudLayout.enemyHealthFillWidth(0.0F, full));
        assertEquals(full, LittleBirdHudLayout.enemyHealthFillWidth(2.0F, full),
                "clamped high: a bar can never overfill");
        assertEquals(0, LittleBirdHudLayout.enemyHealthFillWidth(-1.0F, full), "clamped low");
        assertEquals(0, LittleBirdHudLayout.enemyHealthFillWidth(Float.NaN, full));
        assertEquals(0, LittleBirdHudLayout.enemyHealthFillWidth(1.0F, 0));
        assertTrue(LittleBirdHudLayout.ENEMY_BAR_OFFSET_PIXELS > 0,
                "the readout sits below the crosshair, not on it");
    }

    @Test
    void readoutsDoNotOverlapTheWeaponRow() {
        Panels panels = LittleBirdHudLayout.compute(1920, 1080, 2);
        assertFalse(panels.flightReadout().right() > panels.heatBar().x(),
                "flight readout must not run into the weapon row");
        assertTrue(panels.statusReadout().x() > panels.rocketGrid().right());
        assertTrue(panels.alertLine().y() < 1080 / 2);
    }

    @Test
    void rocketCellsAreOrderedLeftToRightDownThePods() {
        Panels panels = LittleBirdHudLayout.compute(1920, 1080, 2);
        Rect grid = panels.rocketGrid();
        Rect first = LittleBirdHudLayout.rocketCell(grid, 0, 0);
        Rect second = LittleBirdHudLayout.rocketCell(grid, 0, 1);
        Rect lower = LittleBirdHudLayout.rocketCell(grid, 1, 0);
        assertEquals(first.y(), second.y());
        assertTrue(second.x() > first.x());
        assertTrue(lower.y() > first.y());
        assertTrue(LittleBirdHudLayout.rocketCell(grid, 0, 6).right() <= grid.right());
    }

    @Test
    void spentCellsFollowTheRemainingRounds() {
        assertFalse(LittleBirdHudLayout.rocketCellSpent(0, 0, 7, 7));
        assertFalse(LittleBirdHudLayout.rocketCellSpent(0, 6, 7, 7));
        assertTrue(LittleBirdHudLayout.rocketCellSpent(0, 6, 6, 7));
        assertTrue(LittleBirdHudLayout.rocketCellSpent(1, 0, 7, 0));
        assertFalse(LittleBirdHudLayout.rocketCellSpent(1, 3, 0, 4));
    }

    @Test
    void heatFillIsClampedToTheBar() {
        assertEquals(0, LittleBirdHudLayout.heatFillWidth(0, 120));
        assertEquals(40, LittleBirdHudLayout.heatFillWidth(60, 120));
        assertEquals(LittleBirdHudLayout.HEAT_BAR_WIDTH,
                LittleBirdHudLayout.heatFillWidth(120, 120));
        assertEquals(LittleBirdHudLayout.HEAT_BAR_WIDTH,
                LittleBirdHudLayout.heatFillWidth(500, 120));
        assertEquals(0, LittleBirdHudLayout.heatFillWidth(10, 0));
    }

    @Test
    void headingShiftWrapsAroundNorth() {
        assertEquals(0, LittleBirdHudLayout.headingShift(0.0F));
        assertEquals(-180, LittleBirdHudLayout.headingShift(90.0F));
        assertEquals(180, LittleBirdHudLayout.headingShift(270.0F));
        assertEquals(2, LittleBirdHudLayout.headingShift(359.0F));
        // -1 degrees normalises to 359, so it must agree with the positive value.
        assertEquals(LittleBirdHudLayout.headingShift(359.0F), LittleBirdHudLayout.headingShift(-1.0F));
    }

    @Test
    void bankOffsetTracksRollAndClamps() {
        assertEquals(0, LittleBirdHudLayout.bankOffset(0.0F, 38.0F));
        assertEquals(LittleBirdHudLayout.BANK_HALF_WIDTH,
                LittleBirdHudLayout.bankOffset(38.0F, 38.0F));
        assertEquals(-LittleBirdHudLayout.BANK_HALF_WIDTH,
                LittleBirdHudLayout.bankOffset(-38.0F, 38.0F));
        assertEquals(LittleBirdHudLayout.BANK_HALF_WIDTH,
                LittleBirdHudLayout.bankOffset(120.0F, 38.0F));
        assertEquals(0, LittleBirdHudLayout.bankOffset(20.0F, 0.0F));
    }

    @Test
    void speedConversionAndArrowFollowTheMotion() {
        assertEquals(65, LittleBirdHudLayout.toKilometresPerHour(0.9D));
        assertEquals(0, LittleBirdHudLayout.toKilometresPerHour(0.0D));
        assertEquals(-1, LittleBirdHudLayout.verticalArrow(0.2D));
        assertEquals(1, LittleBirdHudLayout.verticalArrow(-0.2D));
        assertEquals(0, LittleBirdHudLayout.verticalArrow(0.0D));
    }

    @Test
    void lowHealthTripsAtThirtyPercent() {
        assertTrue(LittleBirdHudLayout.lowHealth(20.0F, 80.0F));
        assertFalse(LittleBirdHudLayout.lowHealth(24.0F, 80.0F));
        assertFalse(LittleBirdHudLayout.lowHealth(80.0F, 0.0F));
    }
}
