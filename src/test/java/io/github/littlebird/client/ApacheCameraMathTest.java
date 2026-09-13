package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.ApacheGeometry;
import io.github.littlebird.entity.seat.ApacheSeat;
import org.junit.jupiter.api.Test;

final class ApacheCameraMathTest {

    @Test
    void theConfiguredDistanceIsClamped() {
        assertEquals(ApacheCameraMath.MIN_THIRD_PERSON_DISTANCE,
                ApacheCameraMath.thirdPersonDistance(1.0D), 1.0E-6D);
        assertEquals(ApacheCameraMath.MAX_THIRD_PERSON_DISTANCE,
                ApacheCameraMath.thirdPersonDistance(100.0D), 1.0E-6D);
        assertEquals(9.0D, ApacheCameraMath.thirdPersonDistance(9.0D), 1.0E-6D);
        assertEquals(ApacheCameraMath.MIN_THIRD_PERSON_DISTANCE,
                ApacheCameraMath.thirdPersonDistance(Double.NaN), 1.0E-6D);
    }

    @Test
    void theVanillaRequestIsNeverShortened() {
        assertEquals(9.0D, ApacheCameraMath.requestedDistance(4.0D, 9.0D), 1.0E-6D);
        assertEquals(12.0D, ApacheCameraMath.requestedDistance(12.0D, 9.0D), 1.0E-6D,
                "a longer vanilla request is respected");
        assertEquals(0.0D, ApacheCameraMath.requestedDistance(0.0D, 9.0D), 1.0E-6D,
                "a first person pass asks for nothing and must not be stretched");
    }

    @Test
    void theDefaultDistanceClearsTheAirframe() {
        // Vanilla sits four blocks behind the pilot's seat. The tail tip is further back than
        // that, which is why the aircraft used to fill the screen with the lens in its tail.
        for (int seat = 0; seat < ApacheSeat.SEATS; seat++) {
            float reach = ApacheGeometry.tailReachBehindSeatBlocks(seat);
            assertTrue(reach > ApacheCameraMath.VANILLA_THIRD_PERSON_DISTANCE,
                    "the old camera really was inside the airframe: " + reach);
            assertTrue(ApacheCameraMath.thirdPersonDistance(9.0D) > reach + 1.0D,
                    "the default has to leave a block of air behind the tail: " + reach);
        }
    }

    @Test
    void cameraRollFollowsTheBankAndTheConfigFactor() {
        assertEquals(45.0F, ApacheCameraMath.cameraRollDeg(45.0F, 1.0D), 1.0E-4F);
        assertEquals(0.0F, ApacheCameraMath.cameraRollDeg(45.0F, 0.0D), 1.0E-4F);
        assertEquals(-30.0F, ApacheCameraMath.cameraRollDeg(-60.0F, 0.5D), 1.0E-4F);
    }
}
