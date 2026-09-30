package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The corner rules: inside the screen, and never drawn through the hotbar. */
final class B2HudLayoutTest {
    @Test
    void thePanelAlwaysStaysOnScreen() {
        int[][] screens = {{1920, 1080}, {1280, 720}, {854, 480}, {640, 360}, {480, 270}, {320, 180}};
        for (int[] screen : screens) {
            B2HudLayout.Panel box = B2HudLayout.panelFor(screen[0], screen[1], 150, true);
            assertTrue(box.x() >= 0, "x=" + box.x() + " on " + screen[0] + "x" + screen[1]);
            assertTrue(box.y() >= 0, "y=" + box.y());
            assertTrue(box.right() <= screen[0], "right=" + box.right());
            assertTrue(box.bottom() <= screen[1], "bottom=" + box.bottom());
            assertTrue(box.width() > 0 && box.height() > 0);
        }
    }

    @Test
    void thePanelLiftsClearOfTheHotbarWhenTheyWouldShareTheRow() {
        // A narrow logical screen: the panel's left edge is inside the hotbar's span.
        B2HudLayout.Panel box = B2HudLayout.panelFor(500, 320, 240, false);
        assertTrue(box.x() < B2HudLayout.hotbarRightEdge(500), "test premise: they overlap");
        assertTrue(box.bottom() <= 320 - B2HudLayout.HOTBAR_HEIGHT - 4,
                "bottom=" + box.bottom() + " must sit above the hotbar");
        // A wide screen has room on the bottom row, so the panel stays low.
        B2HudLayout.Panel wide = B2HudLayout.panelFor(1920, 1080, 150, true);
        assertTrue(wide.x() >= B2HudLayout.hotbarRightEdge(1920));
        assertTrue(wide.bottom() > 1080 - B2HudLayout.HOTBAR_HEIGHT - 20);
    }

    @Test
    void smallScreensDropTheHintsAndKeepTheSameTopRows() {
        assertTrue(B2HudLayout.hintsFit(1920, 1080));
        assertTrue(B2HudLayout.hintsFit(640, 360), "640x360 still has room for two hint lines");
        assertFalse(B2HudLayout.hintsFit(480, 270), "480x270 does not");
        assertEquals(B2HudLayout.heightFor(true), B2HudLayout.heightFor(false) + B2HudLayout.ROW * 2);
        B2HudLayout.Panel compact = B2HudLayout.panelFor(480, 270, 150, false);
        assertTrue(compact.height() <= B2HudLayout.heightFor(false));
    }

    @Test
    void theWidthFollowsTheContent() {
        int narrow = B2HudLayout.panelFor(1920, 1080, 60, true).width();
        int wide = B2HudLayout.panelFor(1920, 1080, 300, true).width();
        assertEquals(B2HudLayout.MIN_WIDTH, narrow);
        assertEquals(300 + B2HudLayout.PAD * 2, wide);
        // A wider-than-screen request is clamped, never overflowing.
        B2HudLayout.Panel huge = B2HudLayout.panelFor(400, 300, 900, false);
        assertTrue(huge.right() <= 400, "right=" + huge.right());
    }
}
