package io.github.littlebird.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class LittleBirdTracerTimelineTest {
    private static final double LENGTH = 28.0D;

    @Test
    void bulletTravelsAtTheConfiguredSpeed() {
        assertEquals(2.0D, LittleBirdTracerTimeline.travelTicks(LENGTH), 1.0E-6D);
        assertEquals(14.0D, LittleBirdTracerTimeline.frontDistance(1.0D, LENGTH), 1.0E-6D);
        assertEquals(LENGTH, LittleBirdTracerTimeline.frontDistance(3.0D, LENGTH), 1.0E-6D,
                "the head never overshoots the impact point");
    }

    @Test
    void trailStaysBehindTheHead() {
        double front = LittleBirdTracerTimeline.frontDistance(1.0D, LENGTH);
        double tail = LittleBirdTracerTimeline.tailDistance(front);
        assertEquals(front - LittleBirdTracerTimeline.TRAIL_LENGTH, tail, 1.0E-6D);
        assertTrue(tail >= 0.0D);
    }

    @Test
    void bulletFadesAfterArrivalAndThenExpires() {
        assertTrue(LittleBirdTracerTimeline.hasArrived(2.5D, LENGTH));
        assertFalse(LittleBirdTracerTimeline.isExpired(2.1D, LENGTH));
        assertTrue(LittleBirdTracerTimeline.isExpired(2.2D, LENGTH));
        assertEquals(1.0D, LittleBirdTracerTimeline.arrivalFade(2.0D, LENGTH), 1.0E-6D);
        assertEquals(0.0D, LittleBirdTracerTimeline.arrivalFade(2.2D, LENGTH), 1.0E-6D);
    }

    @Test
    void muzzleFlashIsBrief() {
        assertEquals(1.0D, LittleBirdTracerTimeline.muzzleFlashFade(0.0D), 1.0E-6D);
        assertEquals(0.0D, LittleBirdTracerTimeline.muzzleFlashFade(0.2D), 1.0E-6D);
    }

    @Test
    void zeroLengthShotsStillExpire() {
        assertEquals(0.0D, LittleBirdTracerTimeline.travelTicks(0.0D), 1.0E-6D);
        assertTrue(LittleBirdTracerTimeline.isExpired(0.3D, 0.0D));
        assertTrue(LittleBirdTracerTimeline.isStaleWithoutRender(25.0D));
    }
}