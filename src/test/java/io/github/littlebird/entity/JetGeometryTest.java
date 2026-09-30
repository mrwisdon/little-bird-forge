package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class JetGeometryTest {
    @Test
    void airframeRunsFromNoseToTail() {
        assertTrue(JetGeometry.NOSE_TIP_UNITS < 0.0F);
        assertTrue(JetGeometry.TAIL_TIP_UNITS > 0.0F);
        assertEquals(176.0F, JetGeometry.AIRFRAME_LENGTH_UNITS, 1.0E-4F);
        assertEquals(11.0F, JetGeometry.airframeLengthBlocks(), 1.0E-4F);
    }

    @Test
    void theWingIsLongerThanTheFuselageIsWide() {
        assertTrue(JetGeometry.wingSpanBlocks() > JetGeometry.aabbWidthBlocks(),
                "span " + JetGeometry.wingSpanBlocks());
        // Real F-35 span/length is about 0.68; keep the model in that family.
        double ratio = JetGeometry.wingSpanBlocks() / JetGeometry.airframeLengthBlocks();
        assertTrue(ratio > 0.55D && ratio < 0.80D, "ratio " + ratio);
    }

    @Test
    void theSeatSitsInsideTheAirframe() {
        assertTrue(JetGeometry.seatForwardBlocks() > 0.0F,
                "the cockpit sits ahead of the centre of gravity");
        assertTrue(JetGeometry.seatForwardBlocks() < JetGeometry.noseReachAheadBlocks(),
                "and still behind the nose");
        assertTrue(JetGeometry.seatHeightBlocks() > 0.0F);
        assertTrue(JetGeometry.seatHeightBlocks() < JetGeometry.toScaledBlocks(
                JetGeometry.ROOF_TOP_UNITS));
    }

    @Test
    void theCannonMuzzleClearsTheNose() {
        assertTrue(JetGeometry.cannonMuzzleForwardBlocks() > 0.0F);
        assertTrue(JetGeometry.cannonMuzzleForwardBlocks()
                < JetGeometry.airframeLengthBlocks());
        assertTrue(JetGeometry.cannonMuzzleHeightBlocks() > 0.0F);
    }

    @Test
    void theHardpointsHangInsideTheWingSpan() {
        assertTrue(JetGeometry.hardpointLateralBlocks() > 0.0F);
        assertTrue(JetGeometry.hardpointLateralBlocks()
                < JetGeometry.wingSpanBlocks() / 2.0F);
        assertTrue(JetGeometry.hardpointForwardBlocks() > 0.0F);
    }

    @Test
    void theTailReachesPastTheVanillaChaseCamera() {
        // Vanilla asks for four blocks behind the rider; the tail must reach further or the
        // camera clips into the fuselage.
        assertTrue(JetGeometry.tailReachBehindSeatBlocks() > 4.0F,
                "tail reach " + JetGeometry.tailReachBehindSeatBlocks());
        assertTrue(JetGeometry.tailReachBehindSeatBlocks()
                < JetGeometry.airframeLengthBlocks());
    }

    @Test
    void hitboxAndRooflineAreSane() {
        assertTrue(JetGeometry.aabbWidthBlocks() > 0.0F);
        assertTrue(JetGeometry.aabbHeightBlocks() > 0.0F);
        assertTrue(JetGeometry.ROOF_TOP_UNITS > JetGeometry.BELLY_BOTTOM_UNITS);
    }
}
