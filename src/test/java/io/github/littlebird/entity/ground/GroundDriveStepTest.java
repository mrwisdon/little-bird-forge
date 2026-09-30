package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The step ladder GroundMover walks when a hull is blocked by a lip. */
final class GroundDriveStepTest {
    @Test
    void stepRisesClimbInHalfBlockStepsAndStopAtTheLimit() {
        assertArrayEquals(new double[] {0.5D, 1.0D, 1.5D}, GroundDrive.stepRises(1.5D), 1.0E-9D);
        assertArrayEquals(new double[] {0.5D, 1.0D}, GroundDrive.stepRises(1.0D), 1.0E-9D);
    }

    @Test
    void aDisabledOrSillyStepHeightHasNoRungs() {
        assertEquals(0, GroundDrive.stepRises(0.0D).length);
        assertEquals(0, GroundDrive.stepRises(-1.0D).length);
        assertEquals(0, GroundDrive.stepRises(Double.NaN).length);
    }
}
