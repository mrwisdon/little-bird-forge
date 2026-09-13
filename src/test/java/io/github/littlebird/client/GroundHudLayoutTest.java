package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Ground HUD panel placement and readouts. */
class GroundHudLayoutTest {
    @Test
    void panelsSitInOppositeBottomCorners() {
        GroundHudLayout.Panels panels = GroundHudLayout.compute(854, 480, 1);
        GroundHudLayout.Rect health = panels.health();
        GroundHudLayout.Rect weapons = panels.weapons();
        assertTrue(health.x() < weapons.x(), "health left, weapons right");
        assertEquals(854 - GroundHudLayout.EDGE_MARGIN - weapons.width(), weapons.x());
        assertEquals(480 - GroundHudLayout.EDGE_MARGIN - health.height(), health.y());
        assertTrue(weapons.y() + weapons.height() <= 480 - GroundHudLayout.EDGE_MARGIN + 1);
    }

    @Test
    void scaleOnlyMovesTheMarginSoTextAlwaysFits() {
        // The font does not scale with the HUD, so the boxes must not shrink either.
        GroundHudLayout.Panels plain = GroundHudLayout.compute(854, 480, 1);
        GroundHudLayout.Panels doubled = GroundHudLayout.compute(854, 480, 2);
        assertEquals(plain.weapons().width(), doubled.weapons().width());
        assertEquals(plain.rowHeight(), doubled.rowHeight());
        assertEquals(plain.health().width(), doubled.health().width());
        assertTrue(doubled.weapons().x() < plain.weapons().x(), "a wider margin pulls it inward");
        assertTrue(doubled.health().x() > plain.health().x());
    }

    @Test
    void absurdScaleNeverCollapsesAPanel() {
        GroundHudLayout.Panels panels = GroundHudLayout.compute(320, 200, 0);
        assertEquals(GroundHudLayout.HEALTH_WIDTH, panels.health().width());
        assertEquals(GroundHudLayout.SLOT_HEIGHT, panels.rowHeight());
        assertTrue(panels.weapons().height() >= GroundHudLayout.WEAPON_ROWS);
        assertTrue(panels.weapons().x() >= 0);
    }

    @Test
    void rowPositionsStackDownThePanel() {
        GroundHudLayout.Panels panels = GroundHudLayout.compute(854, 480, 1);
        assertEquals(panels.weapons().y(), GroundHudLayout.rowY(panels, 0));
        assertEquals(panels.weapons().y() + panels.rowHeight(), GroundHudLayout.rowY(panels, 1));
    }

    @Test
    void theBarGrowsWithTheWeaponCount() {
        assertEquals(GroundHudLayout.SLOT_SELECTED_WIDTH, GroundHudLayout.barWidth(1));
        assertEquals(GroundHudLayout.SLOT_SELECTED_WIDTH + GroundHudLayout.SLOT_WIDTH
                + GroundHudLayout.SLOT_GAP, GroundHudLayout.barWidth(2));
        assertTrue(GroundHudLayout.barWidth(3) > GroundHudLayout.barWidth(2));
        assertEquals(GroundHudLayout.barWidth(3),
                GroundHudLayout.compute(854, 480, 1, 3).weapons().width());
    }

    @Test
    void slotsStepRightAndTheSelectedOneIsWidest() {
        GroundHudLayout.Panels panels = GroundHudLayout.compute(854, 480, 1, 3);
        int first = GroundHudLayout.slotX(panels, 0);
        int second = GroundHudLayout.slotX(panels, 1);
        int third = GroundHudLayout.slotX(panels, 2);
        assertTrue(second > first);
        assertTrue(third > second);
        assertEquals(first + GroundHudLayout.SLOT_SELECTED_WIDTH + GroundHudLayout.SLOT_GAP, second);
        assertTrue(GroundHudLayout.slotWidth(0) > GroundHudLayout.slotWidth(1));
        assertEquals(GroundHudLayout.SLOT_WIDTH, GroundHudLayout.slotWidth(1));
        int right = GroundHudLayout.slotX(panels, 2) + GroundHudLayout.slotWidth(2);
        assertEquals(panels.weapons().x() + panels.weapons().width(), right);
    }

    @Test
    void readoutSitsAboveTheBarAndInsideTheScreen() {
        GroundHudLayout.Panels panels = GroundHudLayout.compute(854, 480, 1, 3);
        GroundHudLayout.Rect readout = GroundHudLayout.readout(854, 480, 1);
        assertTrue(readout.y() + readout.height() <= panels.weapons().y(),
                "the readout must not overlap the bar");
        assertEquals(panels.weapons().x(), readout.x(), "left edges flush");
        assertEquals(panels.weapons().width(), readout.width(), "same width as the bar");
        assertEquals(panels.weapons().x() + panels.weapons().width(),
                readout.x() + readout.width(), "both right aligned");
        GroundHudLayout.Rect small = GroundHudLayout.readout(320, 200, 1);
        assertTrue(small.x() >= GroundHudLayout.EDGE_MARGIN);
        assertTrue(small.y() >= GroundHudLayout.EDGE_MARGIN);
    }

    @Test
    void aTwoWeaponHullGetsAShorterBarAndReadout() {
        GroundHudLayout.Panels tank = GroundHudLayout.compute(854, 480, 1, 3);
        GroundHudLayout.Panels ifv = GroundHudLayout.compute(854, 480, 1, 2);
        assertTrue(ifv.weapons().width() < tank.weapons().width(), "two slots are narrower");
        assertEquals(ifv.weapons().width(), GroundHudLayout.readout(854, 480, 1, 2).width());
        assertEquals(ifv.weapons().x(), GroundHudLayout.readout(854, 480, 1, 2).x());
        assertTrue(GroundHudLayout.readout(854, 480, 1, 2).y()
                        + GroundHudLayout.READOUT_HEIGHT <= ifv.weapons().y(),
                "the readout still clears the shorter bar");
    }

    @Test
    void panelsStepUpWhereTheHotbarWouldBeCovered() {
        // GUI 427x240 (an 854x480 window at scale 2): the bar clips the hotbar's right corner.
        GroundHudLayout.Panels narrow = GroundHudLayout.compute(427, 240, 2, 3);
        int hotbarTop = 240 - GroundHudLayout.HOTBAR_HEIGHT;
        assertTrue(narrow.weapons().y() + narrow.weapons().height() <= hotbarTop,
                "the weapon bar is clear of the hotbar");
        // A wide GUI keeps both panels in their corners, untouched.
        GroundHudLayout.Panels wide = GroundHudLayout.compute(640, 360, 2, 3);
        int hotbarLeft = 640 / 2 - GroundHudLayout.HOTBAR_WIDTH / 2;
        assertTrue(wide.weapons().x() > hotbarLeft + GroundHudLayout.HOTBAR_WIDTH);
        assertTrue(wide.health().x() + wide.health().width() < hotbarLeft);
    }

    @Test
    void theLiftIsExactlyTheOverlap() {
        assertEquals(0, GroundHudLayout.hotbarLift(427, 240, 12, 202, 96, 26),
                "the health panel is clear of the hotbar");
        assertEquals(10, GroundHudLayout.hotbarLift(427, 240, 295, 197, 120, 31),
                "the bar shares ten pixels with the hotbar");
    }

    @Test
    void fillWidthClamps() {
        assertEquals(50, GroundHudLayout.fillWidth(0.5F, 100));
        assertEquals(0, GroundHudLayout.fillWidth(-1.0F, 100));
        assertEquals(100, GroundHudLayout.fillWidth(3.0F, 100));
        assertEquals(0, GroundHudLayout.fillWidth(Float.NaN, 100));
    }

    @Test
    void cooldownRoundsUpToWholeSeconds() {
        assertEquals(0, GroundHudLayout.cooldownSeconds(0));
        assertEquals(1, GroundHudLayout.cooldownSeconds(1), "a single tick still reads as 1 s");
        assertEquals(1, GroundHudLayout.cooldownSeconds(20));
        assertEquals(18, GroundHudLayout.cooldownSeconds(343));
    }

    @Test
    void healthWarningOnlyWhenHurt() {
        assertFalse(GroundHudLayout.healthWarning(1.0F));
        assertFalse(GroundHudLayout.healthWarning(0.5F));
        assertTrue(GroundHudLayout.healthWarning(0.35F));
        assertTrue(GroundHudLayout.healthWarning(0.0F));
        assertFalse(GroundHudLayout.healthWarning(Float.NaN));
    }
}
