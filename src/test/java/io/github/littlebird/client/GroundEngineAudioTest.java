package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pins the diesel loop curve: idle, full chat and the clamps in between. */
final class GroundEngineAudioTest {
    private static final float EPSILON = 1.0E-5F;

    @Test
    void aStandstillIsIdle() {
        assertEquals(0.0F, GroundEngineAudio.load(0.0D), EPSILON);
        assertEquals(0.32F, GroundEngineAudio.volume(0.0F), EPSILON);
        assertEquals(0.60F, GroundEngineAudio.pitch(0.0F), EPSILON);
    }

    @Test
    void topSpeedIsFullLoad() {
        assertEquals(1.0F, GroundEngineAudio.load(GroundEngineAudio.TOP_SPEED), EPSILON);
        assertEquals(0.90F, GroundEngineAudio.volume(1.0F), EPSILON);
        assertEquals(0.95F, GroundEngineAudio.pitch(1.0F), EPSILON);
    }

    @Test
    void loadIsClampedAtBothEnds() {
        assertEquals(0.0F, GroundEngineAudio.load(-2.0D), EPSILON);
        assertEquals(1.0F, GroundEngineAudio.load(9.0D), EPSILON);
        assertEquals(0.0F, GroundEngineAudio.load(Double.NaN), EPSILON);
    }

    @Test
    void volumeAndPitchRoseWithSpeed() {
        float previous = -1.0F;
        for (int step = 0; step <= 10; step++) {
            float load = step / 10.0F;
            float volume = GroundEngineAudio.volume(load);
            float pitch = GroundEngineAudio.pitch(load);
            assertTrue(volume > previous, "volume must rise at load " + load);
            assertTrue(pitch >= 0.60F - EPSILON && pitch <= 0.95F + EPSILON,
                    "pitch out of range at " + load);
            assertEquals(0.32F + 0.58F * load, volume, EPSILON);
            previous = volume;
        }
    }
}
