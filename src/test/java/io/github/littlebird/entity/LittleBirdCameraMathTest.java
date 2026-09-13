package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.LittleBirdCameraMath.Offset;
import org.junit.jupiter.api.Test;

final class LittleBirdCameraMathTest {
    private static final double EPSILON = 1.0E-6D;

    @Test
    void yawZeroPutsTheLeftShoulderTowardsPlusX() {
        // Facing +Z (south), the aircraft's left is +X (east).
        Offset offset = LittleBirdCameraMath.shoulderOffset(0.0F, 1.6D, 0.6D);
        assertEquals(1.6D, offset.x(), EPSILON);
        assertEquals(0.6D, offset.y(), EPSILON);
        assertEquals(0.0D, offset.z(), EPSILON);
    }

    @Test
    void yawNinetyPutsTheLeftShoulderTowardsPlusZ() {
        Offset offset = LittleBirdCameraMath.shoulderOffset(90.0F, 2.0D, 0.0D);
        assertEquals(0.0D, offset.x(), 1.0E-6D);
        assertEquals(2.0D, offset.z(), 1.0E-6D);
    }

    @Test
    void offsetIsIndependentOfTheSideSign() {
        Offset positive = LittleBirdCameraMath.shoulderOffset(35.0F, 1.5D, 0.4D);
        Offset negative = LittleBirdCameraMath.shoulderOffset(35.0F, -1.5D, 0.4D);
        assertEquals(positive.x(), negative.x(), EPSILON);
        assertEquals(positive.z(), negative.z(), EPSILON);
    }

    @Test
    void cockpitSeatFollowsTheNose() {
        Offset level = LittleBirdCameraMath.cockpitSeatOffset(0.0F, 0.0F, 2.0D);
        assertEquals(0.0D, level.x(), EPSILON);
        assertEquals(0.0D, level.y(), EPSILON);
        assertEquals(2.0D, level.z(), EPSILON, "level flight looks straight ahead");

        Offset diving = LittleBirdCameraMath.cockpitSeatOffset(0.0F, 90.0F, 2.0D);
        assertEquals(0.0D, diving.x(), 1.0E-6D);
        assertEquals(-2.0D, diving.y(), 1.0E-6D, "a nose down dive drops the eye");
        assertEquals(0.0D, diving.z(), 1.0E-6D, "and carries it straight down");

        Offset yawed = LittleBirdCameraMath.cockpitSeatOffset(90.0F, 30.0F, 2.0D);
        assertEquals(-2.0D * Math.cos(Math.toRadians(30.0D)), yawed.x(), 1.0E-6D);
        assertEquals(-2.0D * Math.sin(Math.toRadians(30.0D)), yawed.y(), 1.0E-6D);
        assertEquals(0.0D, yawed.z(), 1.0E-6D);
    }

    @Test
    void shoulderViewOnlyAppliesWhilePilotingInThirdPerson() {
        assertTrue(LittleBirdCameraMath.applies(true, true, true, false, true));
        assertFalse(LittleBirdCameraMath.applies(true, true, true, true, true),
                "the cockpit view owns the camera");
        assertFalse(LittleBirdCameraMath.applies(true, true, false, false, true),
                "first person has no shoulder camera");
        assertFalse(LittleBirdCameraMath.applies(true, true, true, false, false),
                "the aim button must be held");
        assertFalse(LittleBirdCameraMath.applies(false, true, true, false, true),
                "the feature switch wins");
    }
}