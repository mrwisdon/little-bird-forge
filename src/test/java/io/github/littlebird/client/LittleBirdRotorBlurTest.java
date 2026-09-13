package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.LittleBirdGunCycle;
import org.junit.jupiter.api.Test;

final class LittleBirdRotorBlurTest {

    @Test
    void blurIsInvisibleBelowTheThreshold() {
        assertEquals(0.0F, LittleBirdModelMath.rotorBlurAlpha(0.0F), 1.0E-6F);
        assertEquals(0.0F, LittleBirdModelMath.rotorBlurAlpha(0.3F), 1.0E-6F);
        assertEquals(0.0F, LittleBirdModelMath.rotorBlurAlpha(Float.NaN), 1.0E-6F);
    }

    @Test
    void blurGrowsWithRpmAndStaysSubtle() {
        float mid = LittleBirdModelMath.rotorBlurAlpha(0.7F);
        float full = LittleBirdModelMath.rotorBlurAlpha(1.0F);
        assertTrue(mid > 0.0F && mid < full);
        assertEquals(LittleBirdModelMath.ROTOR_BLUR_MAX_ALPHA, full, 1.0E-6F);
        assertTrue(full <= 0.5F, "the disc must stay translucent");
    }

    @Test
    void idlingRotorsSpinSlowerThanLoadedOnes() {
        // A short age keeps the angle inside one turn: wrap360 would otherwise
        // make any proportional comparison meaningless.
        float idle = LittleBirdModelMath.rotorAngleDegrees(1.0F, 0.55F, 2.0F);
        float loaded = LittleBirdModelMath.rotorAngleDegrees(1.0F, 1.0F, 2.0F);
        assertTrue(idle < loaded, "the blades must react to the collective");
        assertEquals(loaded * 0.55F, idle, 1.0E-3F);
    }

    @Test
    void blurDiscIsFainterWhileIdling() {
        float idle = LittleBirdModelMath.rotorBlurAlpha(1.0F, 0.55F);
        float loaded = LittleBirdModelMath.rotorBlurAlpha(1.0F, 1.0F);
        assertTrue(idle < loaded);
        assertTrue(loaded <= LittleBirdModelMath.ROTOR_BLUR_MAX_ALPHA,
                "the disc stays subtler than before");
        assertEquals(0.0F, LittleBirdModelMath.rotorBlurAlpha(0.2F, 1.0F), 1.0E-6F);
    }

    @Test
    void radiansMatchTheDegreeConvention() {
        assertEquals(Math.toRadians(LittleBirdModelMath.rotorAngleDegrees(1.0F, 3.0F)),
                LittleBirdModelMath.rotorAngleRadians(1.0F, 3.0F), 1.0E-4D);
        assertEquals(Math.toRadians(LittleBirdModelMath.tailRotorAngleDegrees(1.0F, 3.0F)),
                LittleBirdModelMath.tailRotorAngleRadians(1.0F, 3.0F), 1.0E-4D);
    }
    @Test
    void gunMuzzlesGlowOnlyWhenHot() {
        float[] rgba = new float[4];
        LittleBirdModelMath.heatGlowColor(0, rgba);
        assertEquals(0.0F, rgba[3], 1.0E-6F, "a cold gun must not glow");
        LittleBirdModelMath.heatGlowColor(LittleBirdGunCycle.MAX_HEAT, rgba);
        assertEquals(1.0F, rgba[3], 1.0E-4F, "a red hot gun glows at full alpha");
        assertTrue(rgba[0] > rgba[1], "the glow is orange-red, not white");
        float[] warm = new float[4];
        LittleBirdModelMath.heatGlowColor(LittleBirdGunCycle.MAX_HEAT / 4, warm);
        assertTrue(warm[3] > 0.0F && warm[3] < rgba[3], "the glow fades in with heat");
    }
}
