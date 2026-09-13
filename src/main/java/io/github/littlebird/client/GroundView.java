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

    /**
     * The turret aim this client should draw for the given frame, in absolute world yaw. While the
     * turret interior view is up on this machine, the turret follows the local driver's own look one
     * for one, so the driver's own interpolated view yaw is both fresher and smoother than the
     * synched copy: the synched value only changes on the 20 Hz server tick, which reads as a step in
     * the lens at any higher frame rate. Every other viewer (and the exterior view) keeps the synched
     * value, so other players' hulls and the third person camera are unchanged.
     */
    float renderTurretYawDeg(float partialTick);

    /** As {@link #renderTurretYawDeg} for the barrel's elevation, already clamped to its stops. */
    float renderTurretPitchDeg(float partialTick);
}
