package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the bomb camera timing: one second of blast, then home. */
final class B2BombCamRulesTest {
    @Test
    void theExplosionIsHeldForExactlyOneSecond() {
        assertEquals(20, B2BombCamRules.HOLD_TICKS_AFTER_IMPACT);
        assertEquals(1.0D, B2BombCamRules.HOLD_TICKS_AFTER_IMPACT / 20.0D, 1.0E-9D);
        assertFalse(B2BombCamRules.shouldReturnHome(0));
        assertFalse(B2BombCamRules.shouldReturnHome(19));
        assertTrue(B2BombCamRules.shouldReturnHome(20));
        assertTrue(B2BombCamRules.shouldReturnHome(21));
    }

    @Test
    void theFollowGivesUpEventually() {
        assertFalse(B2BombCamRules.timedOut(0));
        assertFalse(B2BombCamRules.timedOut(399));
        assertTrue(B2BombCamRules.timedOut(400));
        assertTrue(B2BombCamRules.MAX_FOLLOW_TICKS > B2BombCamRules.HOLD_TICKS_AFTER_IMPACT);
    }

    @Test
    void theLensLooksDownFromAboveTheBomb() {
        assertEquals(72.0D, B2BombCamRules.cameraHeight(64.0D), 1.0E-9D);
        assertTrue(B2BombCamRules.CAMERA_HEIGHT_BLOCKS > 0.0D);
        assertTrue(B2BombCamRules.cameraPitchDeg() > 70.0D
                && B2BombCamRules.cameraPitchDeg() < 90.0D,
                "pitch=" + B2BombCamRules.cameraPitchDeg());
        assertTrue(B2BombCamRules.cameraFovDeg() > 20.0D && B2BombCamRules.cameraFovDeg() < 110.0D);
    }
}
