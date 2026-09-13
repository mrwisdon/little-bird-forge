package io.github.littlebird.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GunshipPlacementTest {

    @Test
    void theGroundDecidesTheHeightNotTheAim() {
        // Aiming at the ground used to bury the aircraft: at a fifteen degree downward look
        // from four blocks away the aim point is a block under the player's feet.
        assertEquals(64.0D, GunshipPlacement.baseY(true, 64.0D, 62.96D), 1.0E-9D,
                "a hit on the ground has to win over the aim height");
        assertEquals(64.0D, GunshipPlacement.baseY(true, 64.0D, 66.0D), 1.0E-9D,
                "and it wins when the player is aiming upwards as well");
        assertEquals(70.0D, GunshipPlacement.baseY(false, 0.0D, 70.0D), 1.0E-9D,
                "open air keeps the player's level");
    }

    @Test
    void theProbeReachesAStepUpAndADropInFront() {
        assertTrue(GunshipPlacement.PROBE_ABOVE >= 1.0D,
                "a one block step up in front has to be found");
        assertTrue(GunshipPlacement.PROBE_BELOW >= 4.0D,
                "and so does a drop, or the aircraft is placed floating");
        assertEquals(65.0D, GunshipPlacement.probeFromY(64.0D), 1.0E-9D);
        assertEquals(56.0D, GunshipPlacement.probeToY(64.0D), 1.0E-9D);
        assertTrue(GunshipPlacement.probeFromY(64.0D) > GunshipPlacement.probeToY(64.0D),
                "the probe points downwards");
    }
}
