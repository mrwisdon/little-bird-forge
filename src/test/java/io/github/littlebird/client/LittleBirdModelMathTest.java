package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the model rotation conventions. These signs are easy to get wrong and
 * are expensive to notice in game, so they are asserted against explicit
 * rotation math here.
 */
final class LittleBirdModelMathTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void modelYawPointsTheNoseAlongTheEntityFacing() {
        assertEquals(180.0F, LittleBirdModelMath.modelYawDegrees(0.0F), 1.0E-4F);
        assertEquals(90.0F, LittleBirdModelMath.modelYawDegrees(90.0F), 1.0E-4F);
        // Rotating the model's -Z nose by (180 - yaw) must reproduce Minecraft's
        // forward direction for that yaw: (-sin(yaw), 0, cos(yaw)).
        for (float yaw = -180.0F; yaw <= 180.0F; yaw += 45.0F) {
            double phi = Math.toRadians(LittleBirdModelMath.modelYawDegrees(yaw));
            double noseX = -Math.sin(phi);
            double noseZ = -Math.cos(phi);
            assertEquals(-Math.sin(Math.toRadians(yaw)), noseX, 1.0E-6D, "yaw " + yaw);
            assertEquals(Math.cos(Math.toRadians(yaw)), noseZ, 1.0E-6D, "yaw " + yaw);
        }
    }

    @Test
    void noseDownPitchTiltsTheNoseDown() {
        assertEquals(-32.0F, LittleBirdModelMath.modelPitchDegrees(32.0F), 1.0E-4F);
        assertEquals(0.0F, LittleBirdModelMath.modelPitchDegrees(0.0F), 1.0E-4F);
        double theta = Math.toRadians(LittleBirdModelMath.modelPitchDegrees(32.0F));
        // Rotating the -Z nose about +X by theta moves it to (0, sin(theta), -cos(theta)).
        double noseY = Math.sin(theta);
        assertTrue(noseY < 0.0D, "positive nose-down attitude must move the nose down");
        assertEquals(-Math.sin(Math.toRadians(32.0D)), noseY, EPSILON);
    }

    @Test
    void rightBankDropsTheRightWing() {
        assertEquals(-38.0F, LittleBirdModelMath.modelRollDegrees(38.0F), 1.0E-4F);
        double phi = Math.toRadians(LittleBirdModelMath.modelRollDegrees(38.0F));
        // The model's right wing sits at +X; a +Z rotation moves it to (cos, sin, 0).
        double rightWingY = Math.sin(phi);
        assertTrue(rightWingY < 0.0D, "a right bank must lower the +X wing");
    }

    @Test
    void rotorSpinScalesWithRpmAndWraps() {
        assertEquals(0.0F, LittleBirdModelMath.rotorAngleDegrees(0.0F, 120.0F), 1.0E-4F);
        assertEquals(LittleBirdModelMath.MAX_ROTOR_DEGREES_PER_TICK,
                LittleBirdModelMath.rotorAngleDegrees(1.0F, 1.0F), 1.0E-4F);
        assertEquals(0.0F, LittleBirdModelMath.rotorAngleDegrees(1.0F, 10.0F), 1.0E-4F);
        assertEquals(180.0F, LittleBirdModelMath.rotorAngleDegrees(0.5F, 10.0F), 1.0E-4F);
        assertEquals(0.0F, LittleBirdModelMath.rotorAngleDegrees(Float.NaN, 10.0F), 1.0E-4F);
        assertEquals(36.0F, LittleBirdModelMath.rotorAngleDegrees(4.0F, 1.0F), 1.0E-4F,
                "RPM above one is clamped");
    }

    @Test
    void tailRotorSpinsFasterThanTheMainRotor() {
        float main = LittleBirdModelMath.rotorAngleDegrees(1.0F, 1.0F);
        assertEquals(main * LittleBirdModelMath.TAIL_ROTOR_MULTIPLIER,
                LittleBirdModelMath.tailRotorAngleDegrees(1.0F, 1.0F), 1.0E-3F);
    }
}
