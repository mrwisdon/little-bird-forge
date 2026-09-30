package io.github.littlebird.entity.weapon;

/**
 * Numbers behind the bomb chase camera: how the client frames a stick on its way down and how long
 * the blast is held before the view snaps back to the aeroplane.
 *
 * <p>Pure rules: no Minecraft classes, so the timing can be pinned by a unit test.
 */
public final class B2BombCamRules {
    /** Ticks the impact is held after the bomb is gone: one second of explosion on screen. */
    public static final int HOLD_TICKS_AFTER_IMPACT = 20;
    /** Safety net: stop following even if the round never reports back. */
    public static final int MAX_FOLLOW_TICKS = 400;
    /** The lens rides this far above the bomb, looking down at it. */
    public static final double CAMERA_HEIGHT_BLOCKS = 8.0D;
    /** Nearly straight down, without the gimbal dead zone at exactly ninety. */
    public static final double CAMERA_PITCH_DEG = 82.0D;
    /** Wide enough to see the target area and the aeroplane leaving. */
    public static final double CAMERA_FOV_DEG = 72.0D;

    private B2BombCamRules() {
    }

    /** True once the explosion has had its second on screen. */
    public static boolean shouldReturnHome(int ticksSinceLost) {
        return ticksSinceLost >= HOLD_TICKS_AFTER_IMPACT;
    }

    /** True when the follow has run long enough to be considered stuck. */
    public static boolean timedOut(int ticksFollowing) {
        return ticksFollowing >= MAX_FOLLOW_TICKS;
    }

    /** Lens height for a bomb at {@code bombY}. */
    public static double cameraHeight(double bombY) {
        return bombY + CAMERA_HEIGHT_BLOCKS;
    }

    public static double cameraPitchDeg() {
        return CAMERA_PITCH_DEG;
    }

    public static double cameraFovDeg() {
        return CAMERA_FOV_DEG;
    }
}
