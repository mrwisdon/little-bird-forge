package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

final class GroundSuspensionTest {
    private static final float[] AXLES = {48.0F, 16.0F, -16.0F, -48.0F};

    @Test
    void followsThePlaneUnderTheAxlesWithInterpolatedPitchRollAndLift() {
        GroundSuspension suspension = new GroundSuspension(AXLES, 16.0F, 0.2D);
        double pitch = Math.toRadians(25.0D);
        double roll = Math.toRadians(12.0D);
        GroundSuspension.HeightProbe incline = (forward, lateral) ->
                0.2D + forward * Math.tan(pitch) - lateral * Math.tan(roll) / Math.cos(pitch);

        suspension.update(incline);
        GroundSuspension.Pose first = suspension.pose(1.0F);
        assertEquals(first.pitchDegrees() / 2, suspension.pose(0.5F).pitchDegrees(), 1.0E-6);
        assertEquals(first.rollDegrees() / 2, suspension.pose(0.5F).rollDegrees(), 1.0E-6);
        assertEquals(first.liftBlocks() / 2, suspension.pose(0.5F).liftBlocks(), 1.0E-6);

        for (int tick = 0; tick < 40; tick++) {
            suspension.update(incline);
        }
        GroundSuspension.Pose settled = suspension.pose(1.0F);
        assertEquals(25.0D, settled.pitchDegrees(), 0.001D);
        assertEquals(12.0D, settled.rollDegrees(), 0.001D);
        assertEquals(0.2D, settled.liftBlocks(), 0.001D);
        for (int axle = 0; axle < AXLES.length; axle++) {
            assertEquals(0.0F, suspension.wheelOffsetUnits(axle, true, 1.0F), 0.001F);
            assertEquals(0.0F, suspension.wheelOffsetUnits(axle, false, 1.0F), 0.001F);
        }
    }

    @Test
    void aBumpCompressesTheWheelWithoutDeformingTheTrackedBelt() {
        GroundSuspension wheeled = new GroundSuspension(AXLES, 16.0F, 0.2D);
        GroundSuspension tracked = new GroundSuspension(AXLES, 16.0F, 0.0D);
        GroundSuspension.HeightProbe bump = (forward, lateral) ->
                forward == 1.0D && lateral < 0.0D ? 0.7D : 0.0D;

        for (int tick = 0; tick < 20; tick++) {
            wheeled.update(bump);
            tracked.update(bump);
        }
        float compressed = wheeled.wheelOffsetUnits(1, true, 1.0F);
        assertEquals(3.2F, compressed, 0.001F);
        assertTrue(wheeled.wheelOffsetUnits(1, false, 1.0F) < 0.0F);
        assertEquals(0.0F, tracked.wheelOffsetUnits(1, true, 1.0F));
    }

    @Test
    void missingOneWheelDoesNotPullTheHullTowardTheLedge() {
        GroundSuspension suspension = new GroundSuspension(AXLES, 16.0F, 0.2D);
        suspension.update((forward, lateral) -> forward == 3.0D && lateral < 0.0D
                ? Double.NaN : 0.0D);
        assertEquals(0.0F, suspension.pose(1.0F).pitchDegrees());
        assertEquals(0.0F, suspension.pose(1.0F).rollDegrees());
        assertTrue(suspension.wheelOffsetUnits(0, true, 1.0F) < 0.0F);
        assertEquals(0.0F, suspension.wheelOffsetUnits(0, false, 1.0F));
    }

    @Test
    void theMountMovesByTheSameTransformTheRendererUses() {
        GroundSuspension.Pose pose = new GroundSuspension.Pose(21.0F, -16.0F, -0.2F);
        float yaw = 68.0F;
        double forward = -3.0D;
        double height = 1.7D;
        Vec3 actual = GroundMover.suspensionMountOffset(pose, yaw, forward, height);
        Quaterniond rotation = new Quaterniond().rotateY(Math.toRadians(180.0D - yaw));
        Vector3d flat = rotation.transform(new Vector3d(0.0D, height, -forward));
        Vector3d tilted = rotation.rotateX(Math.toRadians(pose.pitchDegrees()))
                .rotateZ(-Math.toRadians(pose.rollDegrees()))
                .transform(new Vector3d(0.0D, height, -forward))
                .add(0.0D, pose.liftBlocks(), 0.0D);
        Vector3d offset = tilted.sub(flat);
        assertEquals(offset.x, actual.x, 1.0E-9D);
        assertEquals(offset.y, actual.y, 1.0E-9D);
        assertEquals(offset.z, actual.z, 1.0E-9D);
    }
}
