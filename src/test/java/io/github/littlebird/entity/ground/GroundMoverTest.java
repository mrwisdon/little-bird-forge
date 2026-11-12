package io.github.littlebird.entity.ground;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class GroundMoverTest {
    @Test
    void forwardTravelUsesTheConfiguredHeightAtTheCurrentYaw() {
        assertEquals(1.5F, GroundMover.driveStepHeight(new Vec3(0.0D, -0.05D, 0.3D), 0.0F, 1.5D));
        assertEquals(0.75F, GroundMover.driveStepHeight(new Vec3(-0.3D, -0.05D, 0.0D), 90.0F, 0.75D));
    }

    @Test
    void reverseAndStationaryTravelDisableStepAssist() {
        assertEquals(0.0F, GroundMover.driveStepHeight(new Vec3(0.3D, -0.05D, 0.0D), 90.0F, 1.5D));
        assertEquals(0.0F, GroundMover.driveStepHeight(new Vec3(0.0D, -0.05D, 0.0D), 90.0F, 1.5D));
    }

    @Test
    void disabledAndInvalidHeightsDisableStepAssist() {
        Vec3 forward = new Vec3(0.0D, -0.05D, 0.3D);
        assertEquals(0.0F, GroundMover.driveStepHeight(forward, 0.0F, 0.0D));
        assertEquals(0.0F, GroundMover.driveStepHeight(forward, 0.0F, -1.0D));
        assertEquals(0.0F, GroundMover.driveStepHeight(forward, 0.0F, Double.NaN));
        assertEquals(0.0F, GroundMover.driveStepHeight(forward, 0.0F, Double.POSITIVE_INFINITY));
    }
}
