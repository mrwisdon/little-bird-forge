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

    @Test
    void gentleGroundIsFreeAndTheCurveOnlyEverFalls() {
        // Tracks do not notice the first degrees of a slope.
        assertEquals(1.0D, GroundDrive.gradeSpeedFactor(0.0D, 50.0D), 1.0E-9D);
        assertEquals(1.0D, GroundDrive.gradeSpeedFactor(5.0D, 50.0D), 1.0E-9D);
        assertEquals(1.0D, GroundDrive.gradeSpeedFactor(50.0D * GroundDrive.FREE_GRADE_FRACTION,
                50.0D), 1.0E-9D);
        assertTrue(GroundDrive.gradeSpeedFactor(9.0D, 50.0D) < 1.0D,
                "past the free band a climb must start costing speed");
        double previous = 1.0D;
        for (double grade = 7.5D; grade <= 60.0D; grade += 0.5D) {
            double factor = GroundDrive.gradeSpeedFactor(grade, 50.0D);
            assertTrue(factor <= previous + 1.0E-9D,
                    "the curve must never rise: grade " + grade + " gave " + factor);
            previous = factor;
        }
    }

    @Test
    void pastTheLimitTheClimbKeepsGettingSlowerInsteadOfSaturating() {
        // The old curve clamped everything to the limit, so a 60 degree wall cost exactly what a
        // rated 50 degree climb cost: both read 0.70.
        double atLimit = GroundDrive.gradeSpeedFactor(50.0D, 50.0D);
        double wall = GroundDrive.gradeSpeedFactor(60.0D, 50.0D);
        assertEquals(GroundDrive.CLIMB_FLOOR, atLimit, 1.0E-9D);
        assertEquals(GroundDrive.STEEPEST_FLOOR, wall, 1.0E-9D);
        assertTrue(wall < atLimit, wall + " must be slower than " + atLimit);
        // Continuous at the limit: no cliff in the speed as the hull tips onto steeper ground.
        assertEquals(atLimit, GroundDrive.gradeSpeedFactor(50.0D - 1.0E-6D, 50.0D), 1.0E-6D);
        assertTrue(GroundDrive.STEEPEST_FLOOR > 0.0D,
                "steep ground crawls, it never stalls: that is the design promise");
    }

    @Test
    void aSteepClimbStillCrawlsOnEveryVehicle() {
        for (double max : new double[]{5.0D, 20.0D, 50.0D, 60.0D}) {
            for (double grade : new double[]{max, max + 5.0D, 60.0D, 90.0D, 180.0D}) {
                double factor = GroundDrive.gradeSpeedFactor(grade, max);
                assertTrue(factor > 0.0D, "max " + max + " at grade " + grade + " gave " + factor);
                assertTrue(factor <= 1.0D, "max " + max + " at grade " + grade + " gave " + factor);
            }
        }
    }
}
