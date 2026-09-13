package io.github.littlebird.client;

/**
 * Third person camera numbers for the Apache. Pure rules - no Minecraft classes - so the
 * distance the lens is pushed out to can be tested against the airframe it has to clear.
 */
public final class ApacheCameraMath {
    /** What vanilla asks for in third person: four blocks behind the seat. */
    public static final double VANILLA_THIRD_PERSON_DISTANCE = 4.0D;
    public static final double MIN_THIRD_PERSON_DISTANCE = 4.0D;
    public static final double MAX_THIRD_PERSON_DISTANCE = 24.0D;

    private ApacheCameraMath() {
    }

    /** The configured distance, clamped to something a crew can still fly with. */
    public static double thirdPersonDistance(double configured) {
        if (Double.isNaN(configured)) {
            return MIN_THIRD_PERSON_DISTANCE;
        }
        return Math.max(MIN_THIRD_PERSON_DISTANCE,
                Math.min(MAX_THIRD_PERSON_DISTANCE, configured));
    }

    /**
     * What to hand the camera this frame. The vanilla request is only ever raised: a first
     * person pass (which asks for nothing) and any closer terrain clip are left alone.
     */
    public static double requestedDistance(double vanillaDistance, double configured) {
        if (vanillaDistance <= 0.0D) {
            return vanillaDistance;
        }
        return Math.max(vanillaDistance, thirdPersonDistance(configured));
    }

    /** Camera roll in degrees for an aircraft banked by the given angle. */
    public static float cameraRollDeg(float bankDegrees, double factor) {
        double clamped = Math.max(0.0D, Math.min(1.0D, factor));
        return (float) (bankDegrees * clamped);
    }
}
