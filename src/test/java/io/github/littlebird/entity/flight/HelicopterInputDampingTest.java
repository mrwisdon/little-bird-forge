package io.github.littlebird.entity.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class HelicopterInputDampingTest {

    private static FlightConfig config(double yaw, double pitch, double roll, double collective) {
        return FlightConfig.builder()
                .dampingYaw(yaw).dampingPitch(pitch)
                .dampingRoll(roll).dampingCollective(collective)
                .build();
    }

    private static HelicopterInput input(float yaw, float pitch, float roll, float collective) {
        return new HelicopterInput(yaw, pitch, roll, collective, false);
    }

    @Test
    void factorOneIsPassThrough() {
        FlightConfig none = config(1.0D, 1.0D, 1.0D, 1.0D);
        HelicopterInputDamping.State s = HelicopterInputDamping.step(
                HelicopterInputDamping.State.initial(0.0F), input(90.0F, 0.5F, -0.5F, 1.0F), none);
        assertEquals(90.0F, s.yawDeg(), 1.0E-3F);
        assertEquals(0.5F, s.pitch(), 1.0E-4F);
        assertEquals(-0.5F, s.roll(), 1.0E-4F);
        assertEquals(1.0F, s.collective(), 1.0E-4F);
    }

    @Test
    void dampingRampsInsteadOfSnapping() {
        FlightConfig damped = config(0.25D, 0.25D, 0.25D, 0.25D);
        HelicopterInputDamping.State s = HelicopterInputDamping.State.initial(0.0F);
        s = HelicopterInputDamping.step(s, input(0.0F, 0.0F, 1.0F, 1.0F), damped);
        assertEquals(0.25F, s.roll(), 1.0E-4F, "one tick applies a quarter of the command");
        assertTrue(s.roll() < 1.0F, "a digital key must not snap to full deflection");
        // A quarter per tick needs roughly 30 ticks to land inside a percent; the
        // point of the test is that it converges smoothly, not that it is instant.
        for (int i = 0; i < 30; i++) {
            s = HelicopterInputDamping.step(s, input(0.0F, 0.0F, 1.0F, 1.0F), damped);
        }
        assertEquals(1.0F, s.roll(), 5.0E-3F, "damping still converges");
        assertTrue(s.roll() <= 1.0F, "and never overshoots");
    }

    @Test
    void dampingIsMonotonicWhileTheCommandIsHeld() {
        FlightConfig damped = config(0.2D, 0.2D, 0.2D, 0.2D);
        HelicopterInputDamping.State s = HelicopterInputDamping.State.initial(0.0F);
        float previous = 0.0F;
        for (int i = 0; i < 15; i++) {
            s = HelicopterInputDamping.step(s, input(0.0F, -1.0F, 0.0F, 0.0F), damped);
            // The command is negative, so a correct filter moves monotonically down.
            assertTrue(s.pitch() <= previous, "pitch must move toward the command");
            previous = s.pitch();
        }
        assertTrue(previous < 0.0F, "negative pitch stays negative");
    }

    @Test
    void yawDampingTakesTheShortestTurn() {
        FlightConfig damped = config(0.5D, 1.0D, 1.0D, 1.0D);
        HelicopterInputDamping.State s = HelicopterInputDamping.State.initial(179.0F);
        s = HelicopterInputDamping.step(s, input(-179.0F, 0.0F, 0.0F, 0.0F), damped);
        assertTrue(Math.abs(HelicopterInputDamping.apply(
                input(-179.0F, 0.0F, 0.0F, 0.0F), s).yawDeg()) > 179.0F,
                "179 -> -179 must move two degrees forward, not 358 backwards");
    }

    @Test
    void nonFiniteValuesAreNeutralised() {
        FlightConfig damped = config(0.2D, 0.2D, 0.2D, 0.2D);
        HelicopterInputDamping.State s = HelicopterInputDamping.step(
                HelicopterInputDamping.State.initial(0.0F),
                input(Float.NaN, Float.NaN, Float.NaN, Float.NaN), damped);
        assertTrue(Float.isFinite(s.yawDeg()) && Float.isFinite(s.pitch())
                && Float.isFinite(s.roll()) && Float.isFinite(s.collective()));
    }

    @Test
    void releasedRollSnapsToZeroSoTheBankCanBeHeld() {
        FlightConfig damped = config(1.0D, 1.0D, 0.25D, 1.0D);
        HelicopterInputDamping.State s = HelicopterInputDamping.State.initial(0.0F);
        for (int i = 0; i < 4; i++) {
            s = HelicopterInputDamping.step(s, input(0.0F, 0.0F, 1.0F, 0.0F), damped);
        }
        assertTrue(s.roll() > 0.5F, "the roll command still ramps in, roll=" + s.roll());
        s = HelicopterInputDamping.step(s, input(0.0F, 0.0F, 0.0F, 0.0F), damped);
        assertEquals(0.0F, s.roll(), 1.0E-6F,
                "roll is a rate axis: it must stop the moment the key comes up");
    }

    @Test
    void rotateYawTurnsTheHeldHeadingWithTheAirframe() {
        HelicopterInputDamping.State s =
                new HelicopterInputDamping.State(170.0F, 0.1F, 0.2F, 0.3F);
        HelicopterInputDamping.State turned = HelicopterInputDamping.rotateYaw(s, 30.0F);
        assertEquals(-160.0F, turned.yawDeg(), 1.0E-3F, "the wrap is kept");
        assertEquals(0.1F, turned.pitch(), 1.0E-6F);
        assertEquals(0.2F, turned.roll(), 1.0E-6F);
        assertSame(s, HelicopterInputDamping.rotateYaw(s, 0.0F), "a zero turn is a no-op");
        assertNull(HelicopterInputDamping.rotateYaw(null, 30.0F));
    }

    @Test
    void applyKeepsGroundContact() {
        HelicopterInputDamping.State s = HelicopterInputDamping.State.initial(0.0F);
        HelicopterInput grounded = new HelicopterInput(10.0F, 0.0F, 0.0F, 0.0F, true);
        assertTrue(HelicopterInputDamping.apply(grounded, s).onGround());
    }
}
