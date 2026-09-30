package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The whole climb pipeline, run as a pure simulation: the four-corner contact fit feeds the
 * attitude smoother, which feeds the drive grade. A 50 degree plane is the design target, so
 * the model must keep moving up it, settle onto it, and never move the pose faster than its
 * rate limit. This is the numeric half of the 50 degree acceptance; the in-game half is the
 * driver's own hand-off check.
 */
final class GroundClimbSimulationTest {
    private static final double HALF_LENGTH = 3.5D;
    private static final double HALF_WIDTH = 1.75D;
    /** A 50 degree climb costs top speed but must never reach zero. */
    private static final double MAX_RATE = 4.0D;

    private record Climb(double distance, double speed, double pitch, double worstPitchStep) {
    }

    private static Climb climb(double gradeDegrees, GroundDrive.Spec spec, int maxTicks) {
        double slopeTan = Math.tan(Math.toRadians(gradeDegrees));
        GroundDrive.State state = GroundDrive.State.still(0.0D);
        HullPose pose = new HullPose(60.0D, 35.0D, MAX_RATE, 0.75D, 0.35D);
        double distance = 0.0D;
        double previousPitch = 0.0D;
        double worstPitchStep = 0.0D;
        for (int tick = 0; tick < maxTicks; tick++) {
            double front = (distance + HALF_LENGTH) * slopeTan;
            double back = (distance - HALF_LENGTH) * slopeTan;
            GroundContact.Sample sample = GroundContact.fit(front, front, back, back,
                    HALF_LENGTH, HALF_WIDTH);
            pose.update(sample);
            double driveGrade = sample.valid() ? pose.pitchDegrees() : 0.0D;
            GroundDrive.Step step = GroundDrive.advance(state, 1.0D, 0.0D, spec, driveGrade);
            state = step.state();
            distance += Math.sqrt(step.dx() * step.dx() + step.dz() * step.dz());
            worstPitchStep = Math.max(worstPitchStep, Math.abs(pose.pitchDegrees() - previousPitch));
            previousPitch = pose.pitchDegrees();
            if (distance > 30.0D) {
                break;
            }
        }
        return new Climb(distance, state.speedBlocksPerTick(), pose.pitchDegrees(), worstPitchStep);
    }

    private static GroundDrive.Spec tank() {
        return new GroundDrive.Spec(0.45D, 0.05D, 0.05D, 2.4D, 0.5D, 50.0D);
    }

    @Test
    void theModelKeepsClimbingAtFiftyDegrees() {
        Climb climb = climb(50.0D, tank(), 400);
        assertTrue(climb.speed() > 0.0D, "a 50 degree climb must never stall: speed " + climb.speed());
        assertEquals(50.0D, climb.pitch(), 1.0D, "the hull must settle onto the 50 degree plane");
        assertTrue(climb.worstPitchStep() <= MAX_RATE + 1.0E-9D,
                "the pose must stay inside its rate limit: " + climb.worstPitchStep());
    }

    @Test
    void aFiftyDegreeClimbIsSlowerThanAThirtyDegreeOne() {
        Climb steep = climb(50.0D, tank(), 400);
        Climb shallow = climb(30.0D, tank(), 400);
        assertTrue(steep.speed() < shallow.speed(),
                "50 deg " + steep.speed() + " vs 30 deg " + shallow.speed());
    }

    @Test
    void aFiftyDegreeClimbActuallyMakesProgress() {
        Climb climb = climb(50.0D, tank(), 400);
        assertTrue(climb.distance() > 30.0D, "the model must reach the top: " + climb.distance());
    }
}
