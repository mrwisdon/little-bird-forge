package io.github.littlebird.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.seat.ApacheSeat;
import org.junit.jupiter.api.Test;

final class ApacheGeometryTest {

    @Test
    void theAirframeIsFortyPercentBiggerThanTheLittleBird() {
        assertEquals(0.91F, ApacheGeometry.SCALE, 1.0E-4F);
        assertEquals(LittleBirdGeometry.SCALE * 1.4F, ApacheGeometry.SCALE, 1.0E-4F,
                "the request was 40% bigger than the Little Bird");
        assertTrue(ApacheGeometry.aabbWidthBlocks() > LittleBirdGeometry.aabbWidthBlocks());
        assertTrue(ApacheGeometry.aabbHeightBlocks() > LittleBirdGeometry.aabbHeightBlocks());
    }

    @Test
    void theAirframeIsAsLongAsItsRotorDisc() {
        // The real AH-64 is 15.06 m long with a 14.63 m rotor, so the two are within a few
        // percent. The first Apache model was only 0.73 of its disc, which read as a fat bug.
        assertEquals(164.0F, ApacheGeometry.AIRFRAME_LENGTH_UNITS, 1.0E-4F);
        assertEquals(160.0F, ApacheGeometry.MAIN_ROTOR_RADIUS_UNITS * 2.0F, 1.0E-4F);
        float ratio = ApacheGeometry.airframeToRotorRatio();
        assertTrue(ratio > 0.95F && ratio < 1.10F, "airframe to rotor ratio: " + ratio);
        assertTrue(ApacheGeometry.airframeLengthBlocks() > 9.0F,
                "and the aircraft has to be long in blocks, not just in ratio: "
                        + ApacheGeometry.airframeLengthBlocks());
    }

    @Test
    void theTailIsLongEnoughToBalanceTheRotor() {
        float boom = ApacheGeometry.tailBoomLengthUnits();
        assertTrue(boom > 60.0F, "most of an AH-64 is tail boom: " + boom);
        assertEquals(ApacheGeometry.TAIL_TIP_UNITS,
                ApacheGeometry.tailFinFrontUnits() + ApacheGeometry.TAIL_FIN_LENGTH_UNITS,
                1.0E-4F, "the fin ends at the tail tip");
        assertTrue(ApacheGeometry.TAIL_TIP_UNITS > -ApacheGeometry.NOSE_TIP_UNITS * 1.2F,
                "the tail reaches further back than the nose reaches forward");
    }

    @Test
    void theChinTurretHangsUnderTheNoseAndReachesIt() {
        assertTrue(ApacheGeometry.TURRET_PIVOT_HEIGHT_UNITS < ApacheGeometry.BELLY_BOTTOM_UNITS,
                "the gun hangs below the belly line instead of inside the nose");
        float muzzle = ApacheGeometry.TURRET_PIVOT_FORWARD_UNITS
                + ApacheGeometry.TURRET_BARREL_UNITS;
        float nose = -ApacheGeometry.NOSE_TIP_UNITS;
        assertTrue(muzzle <= nose, "the muzzle must not poke past the nose tip");
        assertTrue(nose - muzzle <= 10.0F, "but it has to reach it: " + (nose - muzzle));
    }

    @Test
    void theModelRestsOnTheEntityFeet() {
        assertEquals(0.0F, ApacheGeometry.SKID_BOTTOM_UNITS, 1.0E-4F,
                "an entity position is the bottom of its AABB");
    }

    @Test
    void theCockpitIsTandem() {
        float pilotHeight = ApacheGeometry.seatHeightBlocks(ApacheSeat.PILOT);
        float gunnerHeight = ApacheGeometry.seatHeightBlocks(ApacheSeat.GUNNER);
        float pilotForward = ApacheGeometry.seatForwardBlocks(ApacheSeat.PILOT);
        float gunnerForward = ApacheGeometry.seatForwardBlocks(ApacheSeat.GUNNER);
        assertTrue(pilotHeight > gunnerHeight,
                "the pilot sits above the gunner: " + pilotHeight + " vs " + gunnerHeight);
        assertTrue(pilotForward < gunnerForward,
                "the pilot sits behind the gunner: " + pilotForward + " vs " + gunnerForward);
    }

    @Test
    void bothEyesStayInsideTheHitbox() {
        for (int seat = 0; seat < ApacheSeat.SEATS; seat++) {
            float eye = ApacheGeometry.seatHeightBlocks(seat) + 1.62F;
            assertTrue(eye < ApacheGeometry.aabbHeightBlocks(),
                    "seat " + seat + " eye at " + eye + " is above the hitbox at "
                            + ApacheGeometry.aabbHeightBlocks());
            assertTrue(eye < ApacheGeometry.toScaledBlocks(ApacheGeometry.ROOF_TOP_UNITS),
                    "and it has to stay under the roof");
        }
    }
}
