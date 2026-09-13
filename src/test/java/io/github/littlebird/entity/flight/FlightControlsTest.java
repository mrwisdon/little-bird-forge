package io.github.littlebird.entity.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class FlightControlsTest {

    @Test
    void collectiveAndRollAxesMapFromTheBits() {
        assertEquals(0.0F, FlightControls.collectiveAxis(0));
        assertEquals(1.0F, FlightControls.collectiveAxis(FlightControls.CLIMB));
        assertEquals(-1.0F, FlightControls.collectiveAxis(FlightControls.DESCEND));
        assertEquals(0.0F, FlightControls.collectiveAxis(
                FlightControls.CLIMB | FlightControls.DESCEND), "both keys cancel out");

        assertEquals(0.0F, FlightControls.rollAxis(0));
        assertEquals(1.0F, FlightControls.rollAxis(FlightControls.ROLL_RIGHT));
        assertEquals(-1.0F, FlightControls.rollAxis(FlightControls.ROLL_LEFT));
        assertEquals(0.0F, FlightControls.rollAxis(
                FlightControls.ROLL_LEFT | FlightControls.ROLL_RIGHT));
    }

    @Test
    void theTwoAxesDoNotInterfere() {
        int both = FlightControls.CLIMB | FlightControls.ROLL_LEFT;
        assertEquals(1.0F, FlightControls.collectiveAxis(both));
        assertEquals(-1.0F, FlightControls.rollAxis(both));
    }
}
