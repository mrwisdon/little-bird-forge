package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The camera shock rules: stacking, decay, caps and a finite offset for every input. */
class ImpactShakeTest {
    private static final float EPSILON = 1.0E-6F;

    @BeforeEach
    void resetShake() {
        ImpactShake.reset();
    }

    @Test
    void addStacksAndClampsAtOne() {
        ImpactShake.add(0.4F);
        assertEquals(0.4F, ImpactShake.trauma(), EPSILON);
        ImpactShake.add(0.4F);
        assertEquals(0.8F, ImpactShake.trauma(), EPSILON);
        ImpactShake.add(5.0F);
        assertEquals(1.0F, ImpactShake.trauma(), EPSILON,
                "several hits at once must not push the shock past full trauma");
    }

    @Test
    void addIgnoresJunk() {
        ImpactShake.add(Float.NaN);
        ImpactShake.add(Float.POSITIVE_INFINITY);
        ImpactShake.add(Float.NEGATIVE_INFINITY);
        ImpactShake.add(-1.0F);
        ImpactShake.add(0.0F);
        assertEquals(0.0F, ImpactShake.trauma(), 0.0F,
                "a corrupt intensity must never start a shake");
    }

    @Test
    void tickDecaysToExactlyZero() {
        ImpactShake.add(1.0F);
        float previous = ImpactShake.trauma();
        for (int i = 0; i < 200; i++) {
            ImpactShake.tick();
            assertTrue(ImpactShake.trauma() <= previous, "ticking may only bleed trauma away");
            previous = ImpactShake.trauma();
        }
        assertEquals(0.0F, ImpactShake.trauma(), 0.0F, "spent trauma must settle on exactly zero");
        assertEquals(0.0F, ImpactShake.yawOffsetDeg(0.5F), 0.0F);
        assertEquals(0.0F, ImpactShake.pitchOffsetDeg(0.5F), 0.0F);
    }

    @Test
    void offsetsRespectTheCapsAndStayFinite() {
        ImpactShake.add(1.0F);
        float[] partialTicks = {0.0F, 0.25F, 1.0F, 7.5F, -3.0F, Float.NaN,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (int tick = 0; tick < 60; tick++) {
            for (float partialTick : partialTicks) {
                float yaw = ImpactShake.yawOffsetDeg(partialTick);
                float pitch = ImpactShake.pitchOffsetDeg(partialTick);
                assertTrue(Float.isFinite(yaw) && Float.isFinite(pitch),
                        "offsets must stay finite for partialTick " + partialTick);
                assertTrue(Math.abs(yaw) <= ImpactShake.MAX_YAW_DEG + EPSILON, "yaw " + yaw);
                assertTrue(Math.abs(pitch) <= ImpactShake.MAX_PITCH_DEG + EPSILON, "pitch " + pitch);
            }
            ImpactShake.tick();
        }
    }

    @Test
    void partialTickNeverProducesNaN() {
        ImpactShake.add(0.55F);
        float[] partialTicks = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -12.0F,
                99.0F};
        for (float partialTick : partialTicks) {
            assertFalse(Float.isNaN(ImpactShake.yawOffsetDeg(partialTick)));
            assertFalse(Float.isNaN(ImpactShake.pitchOffsetDeg(partialTick)));
            assertFalse(Float.isNaN(ImpactShake.wobble(3L, 0.0F, partialTick)));
        }
    }

    @Test
    void resetClearsEverything() {
        ImpactShake.add(1.0F);
        ImpactShake.tick();
        ImpactShake.reset();
        assertEquals(0.0F, ImpactShake.trauma(), 0.0F);
        assertEquals(0.0F, ImpactShake.yawOffsetDeg(0.0F), 0.0F);
        assertEquals(0.0F, ImpactShake.pitchOffsetDeg(1.0F), 0.0F);
    }

    @Test
    void shapingIsPureAndBounded() {
        for (int tick = 0; tick < 32; tick++) {
            for (float trauma = 0.0F; trauma <= 1.0F; trauma += 0.125F) {
                for (float partialTick = 0.0F; partialTick <= 1.0F; partialTick += 0.5F) {
                    float yaw = ImpactShake.shapedOffsetDeg(
                            ImpactShake.MAX_YAW_DEG, trauma, tick, 0.0F, partialTick);
                    float pitch = ImpactShake.shapedOffsetDeg(
                            ImpactShake.MAX_PITCH_DEG, trauma, tick, 1.7F, partialTick);
                    assertTrue(Math.abs(yaw) <= ImpactShake.MAX_YAW_DEG + EPSILON, "yaw " + yaw);
                    assertTrue(Math.abs(pitch) <= ImpactShake.MAX_PITCH_DEG + EPSILON, "pitch " + pitch);
                }
            }
        }
        // Pure: identical inputs give an identical output and leave the stored state alone.
        float first = ImpactShake.shapedOffsetDeg(1.0F, 0.5F, 11L, 0.0F, 0.5F);
        float second = ImpactShake.shapedOffsetDeg(1.0F, 0.5F, 11L, 0.0F, 0.5F);
        assertEquals(first, second, 0.0F);
        assertEquals(0.0F, ImpactShake.trauma(), 0.0F, "shaping must not touch the stored shock");
    }

    @Test
    void fullTraumaActuallyDeflectsTheCamera() {
        ImpactShake.add(1.0F);
        float peak = 0.0F;
        for (int tick = 0; tick < 40; tick++) {
            for (float partialTick = 0.0F; partialTick < 1.0F; partialTick += 0.1F) {
                peak = Math.max(peak, Math.abs(ImpactShake.yawOffsetDeg(partialTick)));
            }
            ImpactShake.tick();
        }
        assertTrue(peak > 0.0F, "a full-strength impact must visibly move the camera");
        assertTrue(peak <= ImpactShake.MAX_YAW_DEG + EPSILON);
    }

    @Test
    void wobbleNeverLeavesUnitRange() {
        for (int tick = 0; tick < 500; tick++) {
            for (float partialTick = 0.0F; partialTick <= 1.0F; partialTick += 0.25F) {
                float value = ImpactShake.wobble(tick, 0.0F, partialTick);
                assertTrue(Math.abs(value) <= 1.0F + 1.0E-5F, "wobble " + value);
            }
        }
    }
}
