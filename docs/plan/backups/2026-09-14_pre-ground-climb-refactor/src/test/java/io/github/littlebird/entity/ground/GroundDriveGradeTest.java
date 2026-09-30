package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The grade coupling: slower uphill, a little faster downhill, unholdable past the limit. */
final class GroundDriveGradeTest {
    private static final GroundDrive.Spec SPEC =
            new GroundDrive.Spec(0.5D, 0.05D, 0.1D, 2.0D, 0.5D, 35.0D);

    @Test
    void flatKeepsFullSpeedAndDownhillAddsALittle() {
        assertEquals(1.0D, GroundDrive.gradeSpeedFactor(0.0D, 35.0D), 1.0E-9D);
        assertTrue(GroundDrive.gradeSpeedFactor(35.0D, 35.0D) < 0.8D,
                "a climb up to the limit must cost some speed");
        assertTrue(GroundDrive.gradeSpeedFactor(-35.0D, 35.0D) > 1.0D,
                "gravity should help on the way down");
    }

    @Test
    void aSteadyClimbIsSlowerThanTheFlatRun() {
        GroundDrive.State flat = GroundDrive.State.still(0.0D);
        GroundDrive.State climb = GroundDrive.State.still(0.0D);
        GroundDrive.Step flatStep = null;
        GroundDrive.Step climbStep = null;
        for (int tick = 0; tick < 12; tick++) {
            flatStep = GroundDrive.advance(flat, 1.0D, 0.0D, SPEC, 0.0D);
            climbStep = GroundDrive.advance(climb, 1.0D, 0.0D, SPEC, 20.0D);
            flat = flatStep.state();
            climb = climbStep.state();
        }
        assertTrue(climbStep.state().speedBlocksPerTick()
                        < flatStep.state().speedBlocksPerTick(),
                "climb " + climbStep.state().speedBlocksPerTick()
                        + " vs flat " + flatStep.state().speedBlocksPerTick());
    }

    @Test
    void aGradePastTheLimitStillCrawlsInsteadOfStalling() {
        GroundDrive.State flat = GroundDrive.State.still(0.0D);
        GroundDrive.State steep = GroundDrive.State.still(0.0D);
        for (int tick = 0; tick < 40; tick++) {
            flat = GroundDrive.advance(flat, 1.0D, 0.0D, SPEC, 0.0D).state();
            steep = GroundDrive.advance(steep, 1.0D, 0.0D, SPEC, 50.0D).state();
        }
        assertTrue(steep.speedBlocksPerTick() > 0.0D,
                "a steep climb is slow, not stuck: " + steep.speedBlocksPerTick());
        assertTrue(steep.speedBlocksPerTick() < flat.speedBlocksPerTick());
    }

    @Test
    void theDefaultSpecStillCompilesWithoutAGrade() {
        GroundDrive.Spec legacy = new GroundDrive.Spec(0.4D, 0.04D, 0.08D, 2.0D, 0.4D);
        assertEquals(GroundDrive.DEFAULT_MAX_GRADE_DEGREES, legacy.maxGradeDegrees(), 1.0E-9D);
    }
}
