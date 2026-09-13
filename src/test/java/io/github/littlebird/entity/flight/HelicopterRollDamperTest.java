package io.github.littlebird.entity.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HelicopterRollDamperTest {
    private static final FlightConfig CONFIG = FlightConfig.defaults();

    @Test
    void theRollEasesInInsteadOfSnappingOver() {
        float axis = HelicopterRollDamper.step(0.0F, 1.0F, CONFIG);
        assertTrue(axis > 0.0F && axis < 0.5F,
                "the first tick must not be full deflection, axis=" + axis);
        // A tenth of the axis per tick at the default ten tick spin-up.
        assertEquals(0.1F, axis, 1.0E-4F);
    }

    @Test
    void theRollReachesFullRateInTheConfiguredTime() {
        float axis = 0.0F;
        for (int i = 0; i < 10; i++) {
            axis = HelicopterRollDamper.step(axis, 1.0F, CONFIG);
        }
        assertEquals(1.0F, axis, 1.0E-4F, "ten ticks of spin-up must reach full rate");
    }

    @Test
    void theAxisIsMonotonicWhileTheKeyIsHeld() {
        float previous = 0.0F;
        for (int i = 0; i < 12; i++) {
            float axis = HelicopterRollDamper.step(previous, 1.0F, CONFIG);
            assertTrue(axis >= previous, "the rate must build up, not wobble");
            assertTrue(axis <= 1.0F, "and never overshoot the command");
            previous = axis;
        }
        assertEquals(1.0F, previous, 1.0E-4F);
    }

    @Test
    void releasingDropsTheRateImmediatelySoTheBankHolds() {
        float axis = HelicopterRollDamper.step(0.0F, 1.0F, CONFIG);
        axis = HelicopterRollDamper.step(axis, 1.0F, CONFIG);
        assertEquals(0.0F, HelicopterRollDamper.step(axis, 0.0F, CONFIG), 1.0E-6F,
                "an undamped release is what keeps the bank angle exact");
    }

    @Test
    void theSpinUpIsConfigurable() {
        FlightConfig quick = FlightConfig.builder().rollSpinUpTicks(2.0D).build();
        assertEquals(0.5F, HelicopterRollDamper.step(0.0F, 1.0F, quick), 1.0E-4F);
        FlightConfig broken = FlightConfig.builder().rollSpinUpTicks(0.0D).build();
        assertEquals(1.0F, HelicopterRollDamper.stepPerTick(broken), 1.0E-6D,
                "a nonsensical spin-up falls back to no damping");
    }

    @Test
    void nonFiniteValuesAreNeutralised() {
        assertTrue(Float.isFinite(HelicopterRollDamper.step(Float.NaN, Float.NaN, CONFIG)));
        assertEquals(0.0F, HelicopterRollDamper.step(0.5F, Float.NaN, CONFIG), 1.0E-6F);
    }
}
