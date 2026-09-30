package io.github.littlebird.entity.flight;

/**
 * The two horizontal axes after the mouse and the A/D keys have been assigned.
 *
 * <p>Battlefield preset (mouse yaw off): the mouse rolls and A/D yaws. With mouse yaw on the two
 * swap, which is the scheme the helicopters already use. Pitch is never involved. Pure rules: no
 * Minecraft classes, so the mapping is unit tested.
 */
public record JetAxes(float roll, float yaw) {
    public static JetAxes of(float mouseAxis, float keyAxis, boolean mouseYaw) {
        return mouseYaw ? new JetAxes(keyAxis, mouseAxis) : new JetAxes(mouseAxis, keyAxis);
    }
}
