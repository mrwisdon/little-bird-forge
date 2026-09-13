package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * These are the invariants behind the invisible-aircraft bug, so they are
 * asserted rather than trusted.
 */
final class LittleBirdGeometryTest {

    @Test
    void modelRestsOnTheEntityFeet() {
        // An entity position is the bottom of its AABB: the lowest model geometry
        // must be exactly there, or the aircraft renders sunk into the ground.
        assertEquals(0.0F, LittleBirdGeometry.SKID_BOTTOM_UNITS, 1.0E-4F);
    }

    @Test
    void airframeFitsInsideItsOwnHitbox() {
        assertTrue(LittleBirdGeometry.airframeHeightBlocks()
                        <= LittleBirdGeometry.aabbHeightBlocks(),
                "airframe is taller than the hitbox: " + LittleBirdGeometry.airframeHeightBlocks()
                        + " vs " + LittleBirdGeometry.aabbHeightBlocks());
        assertTrue(LittleBirdGeometry.toScaledBlocks(
                        LittleBirdGeometry.CABIN_HALF_WIDTH_UNITS * 2.0F)
                        <= LittleBirdGeometry.aabbWidthBlocks(),
                "cabin is wider than the hitbox");
    }

    @Test
    void rotorSitsAboveTheCabin() {
        assertTrue(LittleBirdGeometry.ROTOR_HUB_UNITS > LittleBirdGeometry.ROOF_TOP_UNITS);
        assertTrue(LittleBirdGeometry.ROOF_TOP_UNITS > LittleBirdGeometry.CABIN_TOP_UNITS);
        assertTrue(LittleBirdGeometry.CABIN_TOP_UNITS > LittleBirdGeometry.CANOPY_BOTTOM_UNITS);
        assertTrue(LittleBirdGeometry.CANOPY_BOTTOM_UNITS > LittleBirdGeometry.CABIN_BOTTOM_UNITS);
        assertTrue(LittleBirdGeometry.CABIN_BOTTOM_UNITS > LittleBirdGeometry.SKID_TUBE_HEIGHT_UNITS);
    }

    @Test
    void scaledRotorIsSixAndAHalfBlocks() {
        assertEquals(6.5F, LittleBirdGeometry.rotorDiameterBlocks(), 1.0E-3F);
        assertTrue(LittleBirdGeometry.SCALE < 1.0F, "the aircraft was shrunk on request");
        assertTrue(LittleBirdGeometry.SCALE > 0.3F, "scale must stay usable");
    }

    @Test
    void riderEyeSitsBelowTheRotorDisc() {
        float eye = LittleBirdGeometry.riderOffsetBlocks() + 1.62F;
        assertTrue(eye < LittleBirdGeometry.rotorHubBlocks(),
                "eye height " + eye + " would be inside the rotor disc at "
                        + LittleBirdGeometry.rotorHubBlocks());
    }

    @Test
    void cockpitEyeLooksOverTheNoseDeck() {
        float eye = LittleBirdGeometry.cockpitRiderOffsetBlocks() + 1.62F;
        assertEquals(LittleBirdGeometry.cockpitEyeBlocks(), eye, 1.0E-3F);
        assertTrue(eye - LittleBirdGeometry.noseDeckTopBlocks() >= 0.3F,
                "the nose deck would sit on the eye line and black out the view: eye "
                        + eye + " deck " + LittleBirdGeometry.noseDeckTopBlocks());
        assertTrue(eye < LittleBirdGeometry.rotorHubBlocks(),
                "the eye must stay under the rotor disc");
        assertTrue(LittleBirdGeometry.cockpitRiderOffsetBlocks() < 0.0F,
                "at this build scale the camera has to drop into the airframe");
        assertTrue(LittleBirdGeometry.cockpitSeatForwardBlocks()
                        > LittleBirdGeometry.windscreenFrontBlocks(),
                "the cockpit camera must sit ahead of the windscreen frame or the"
                        + " frame blocks the middle of the screen: "
                        + LittleBirdGeometry.cockpitSeatForwardBlocks() + " vs "
                        + LittleBirdGeometry.windscreenFrontBlocks());
        // The default seat stays above ground so the server never buries the pilot.
        assertTrue(LittleBirdGeometry.riderOffsetBlocks() >= 0.0F);
    }

    @Test
    void theCockpitEyeLiftIsAppliedOnTopOfTheSeat() {
        assertEquals(LittleBirdGeometry.cockpitRiderOffsetBlocks() + 0.4F,
                LittleBirdGeometry.cockpitRiderOffsetBlocks(0.4F), 1.0E-5F);
        assertTrue(LittleBirdGeometry.cockpitEyeBlocks() + 0.4F
                        < LittleBirdGeometry.rotorHubBlocks(),
                "the default lift must keep the eye under the rotor disc");
    }

    @Test
    void weaponHardpointsMatchTheScaledModel() {
        assertEquals(LittleBirdGeometry.toScaledBlocks(LittleBirdGeometry.GUN_X_UNITS),
                LittleBirdGeometry.gunLateralBlocks(), 1.0E-5F);
        assertTrue(LittleBirdGeometry.gunForwardBlocks() > 0.7F,
                "the under-wing barrels still stick out ahead of the pylon");
        assertTrue(LittleBirdGeometry.podLateralBlocks() > LittleBirdGeometry.gunLateralBlocks());
        assertTrue(LittleBirdGeometry.podHeightBlocks() < LittleBirdGeometry.gunHeightBlocks());
    }

    @Test
    void unitConversionsRoundTrip() {
        assertEquals(1.0F, LittleBirdGeometry.toBlocks(16.0F), 1.0E-5F);
        assertEquals(32.0F, LittleBirdGeometry.toUnits(2.0F), 1.0E-5F);
        assertEquals(LittleBirdGeometry.SCALE, LittleBirdGeometry.toScaledBlocks(16.0F), 1.0E-5F);
    }
}
