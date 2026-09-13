package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class LittleBirdHudMathTest {

    @Test
    void headingWrapsAndPadsToThreeDigits() {
        assertEquals("000", LittleBirdHudMath.headingLabel(0.0F));
        assertEquals("090", LittleBirdHudMath.headingLabel(90.0F));
        assertEquals("359", LittleBirdHudMath.headingLabel(-1.0F));
        assertEquals("010", LittleBirdHudMath.headingLabel(370.0F));
    }

    @Test
    void ladderMovesTheHorizonWithTheNose() {
        assertEquals(0.0D, LittleBirdHudMath.ladderOffsetPixels(0.0F, 4.0D), 1.0E-6D);
        assertEquals(20.0D, LittleBirdHudMath.ladderOffsetPixels(5.0F, 4.0D), 1.0E-6D,
                "nose down moves the horizon down the screen");
        assertEquals(0.0D, LittleBirdHudMath.ladderOffsetPixels(-5.0F, -4.0D), 1.0E-6D);
    }

    @Test
    void ladderLinesLeaveTheWindowAsTheNosePitches() {
        assertTrue(LittleBirdHudMath.ladderLineVisible(0.0F, 0.0F, 20.0D));
        assertFalse(LittleBirdHudMath.ladderLineVisible(30.0F, 0.0F, 20.0D));
        assertTrue(LittleBirdHudMath.ladderLineVisible(30.0F, 25.0F, 20.0D));
    }

    @Test
    void bankScaleClampsAFullRollOver() {
        assertEquals(60.0D, LittleBirdHudMath.clampBankDegrees(120.0F), 1.0E-6D);
        assertEquals(-60.0D, LittleBirdHudMath.clampBankDegrees(-179.0F), 1.0E-6D);
        assertEquals(25.0D, LittleBirdHudMath.clampBankDegrees(25.0F), 1.0E-6D);
        assertEquals(0.0D, LittleBirdHudMath.clampBankDegrees(Float.NaN), 1.0E-6D);
    }

    @Test
    void healthPercentLabelReadsAsAPercentage() {
        assertEquals("100%", LittleBirdHudMath.healthPercentLabel(1.0F));
        assertEquals("87%", LittleBirdHudMath.healthPercentLabel(0.869F));
        assertEquals("50%", LittleBirdHudMath.healthPercentLabel(0.5F));
        assertEquals("0%", LittleBirdHudMath.healthPercentLabel(0.0F));
        assertEquals("0%", LittleBirdHudMath.healthPercentLabel(-0.5F), "clamped low");
        assertEquals("100%", LittleBirdHudMath.healthPercentLabel(4.0F), "clamped high");
        assertEquals("0%", LittleBirdHudMath.healthPercentLabel(Float.NaN));
    }

    @Test
    void bankTicksFollowTheRoll() {
        assertEquals(-30.0D, LittleBirdHudMath.bankTickDegrees(-30.0D, 0.0F), 1.0E-6D);
        assertEquals(-10.0D, LittleBirdHudMath.bankTickDegrees(-30.0D, 20.0F), 1.0E-6D);
        assertEquals(0, LittleBirdHudMath.clampToScreen(-5, 0, 100));
        assertEquals(100, LittleBirdHudMath.clampToScreen(500, 0, 100));
    }
}