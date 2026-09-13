package io.github.littlebird.entity.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ApacheFlightProfileTest {
    private static final FlightConfig APACHE = ApacheFlightProfile.config();
    private static final FlightConfig LITTLE_BIRD = FlightConfig.defaults();

    @Test
    void topSpeedIsNinetyKilometresAnHour() {
        assertEquals(90, ApacheFlightProfile.topSpeedKmh());
        assertEquals(1.25D, APACHE.maxForwardSpeed, 1.0E-9D);
    }

    @Test
    void everyRateIsLowerThanTheLittleBird() {
        assertTrue(APACHE.maxYawRateDegPerTick < LITTLE_BIRD.maxYawRateDegPerTick,
                "yaw " + APACHE.maxYawRateDegPerTick + " vs " + LITTLE_BIRD.maxYawRateDegPerTick);
        assertTrue(APACHE.pitchRateDegPerTick < LITTLE_BIRD.pitchRateDegPerTick);
        assertTrue(APACHE.rollRateDegPerTick < LITTLE_BIRD.rollRateDegPerTick);
        assertTrue(APACHE.forwardAccel < LITTLE_BIRD.forwardAccel);
        assertTrue(APACHE.bankedTurnDegPerTick < LITTLE_BIRD.bankedTurnDegPerTick);
        assertTrue(APACHE.rollSpinUpTicks > LITTLE_BIRD.rollSpinUpTicks,
                "a heavier rotor takes longer to spin the roll rate up");
        assertTrue(APACHE.verticalDrag > LITTLE_BIRD.verticalDrag);
    }

    @Test
    void theEnvelopeIsStillUnlimited() {
        assertEquals(90.0D, APACHE.pitchMaxDeg, 1.0E-9D,
                "the nose still reaches straight up and straight down");
        assertTrue(APACHE.rollRateDegPerTick > 0.0D, "roll is still a rate command");
        assertEquals(0.0D, APACHE.rollLevelRateDegPerTick, 1.0E-9D,
                "and the bank still holds when the key is released");
    }
}
