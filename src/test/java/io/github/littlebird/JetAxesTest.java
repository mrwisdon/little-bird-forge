package io.github.littlebird;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.littlebird.entity.flight.JetAxes;
import org.junit.jupiter.api.Test;

/** The mouse and the A/D keys swap roles with the mouse-yaw switch and never share an axis. */
final class JetAxesTest {
    @Test
    void theBattlefieldPresetKeepsTheMouseOnRoll() {
        JetAxes axes = JetAxes.of(0.5F, -0.25F, false);
        assertEquals(0.5F, axes.roll());
        assertEquals(-0.25F, axes.yaw());
    }

    @Test
    void mouseYawMovesTheMouseOntoYaw() {
        JetAxes axes = JetAxes.of(0.5F, -0.25F, true);
        assertEquals(-0.25F, axes.roll());
        assertEquals(0.5F, axes.yaw());
    }
}
