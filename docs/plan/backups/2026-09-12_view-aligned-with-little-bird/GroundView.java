package io.github.littlebird.client;

/**
 * A ground vehicle that offers a turret interior camera next to its exterior one.
 *
 * <p>The flag is a client-side presentation choice, exactly like the aircraft cockpit flag: the
 * server never needs it, so it is not synched. It only moves where the local player's camera sits,
 * so it cannot affect the hull's physics, its hitbox or anyone else's view.
 */
public interface GroundView {
    /** True while this hull is being viewed through its turret interior camera. */
    boolean isInteriorView();

    /** Client only: switches between the exterior and turret interior cameras. */
    void setInteriorView(boolean value);
}
