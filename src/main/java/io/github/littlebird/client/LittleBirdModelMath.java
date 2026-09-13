package io.github.littlebird.client;

import io.github.littlebird.entity.weapon.LittleBirdGunCycle;

/**
 * Rotation conventions for the Little Bird model, kept free of Minecraft
 * classes so they can be unit-tested.
 *
 * The model is built with its nose along -Z, which is what Minecraft's own
 * living-entity models use. A -Z model needs a yaw of (180 - entityYaw) to
 * point along the entity's facing direction, a negative X rotation to pitch the
 * nose down, and a negative Z rotation to drop the right wing.
 */
public final class LittleBirdModelMath {
    public static final float MAX_ROTOR_DEGREES_PER_TICK = 36.0F;
    public static final float TAIL_ROTOR_MULTIPLIER = 2.5F;
    /** Above this RPM the rotor disc gets a translucent blur pass. */
    public static final float ROTOR_BLUR_RPM = 0.45F;
    public static final float ROTOR_BLUR_MAX_ALPHA = 0.20F;
    /** Rotor spin factor used when no throttle information is available. */
    public static final float FULL_SPIN = 1.0F;
    private static final float DEGREES_PER_RADIAN = 57.295776F;

    private LittleBirdModelMath() {
    }

    public static float modelYawDegrees(float entityYawDeg) {
        return 180.0F - entityYawDeg;
    }

    /** noseDownDeg is positive when the nose points down (entity xRot sign). */
    public static float modelPitchDegrees(float noseDownDeg) {
        return -noseDownDeg;
    }

    /** rollDeg is positive when the aircraft banks right. */
    public static float modelRollDegrees(float rollDeg) {
        return -rollDeg;
    }

    /** Main rotor spin in degrees, wrapped to one turn. */
    public static float rotorAngleDegrees(float rotorRpm, float ageInTicks) {
        return rotorAngleDegrees(rotorRpm, FULL_SPIN, ageInTicks);
    }

    /**
     * spinFactor comes from the vertical throttle, so the blades visibly speed up
     * when the helicopter is climbing and idle slowly in the hover.
     */
    public static float rotorAngleDegrees(float rotorRpm, float spinFactor, float ageInTicks) {
        float spin = ageInTicks * MAX_ROTOR_DEGREES_PER_TICK * clamp01(rotorRpm)
                * Math.max(0.1F, clamp01(spinFactor));
        return wrap360(spin);
    }

    public static float tailRotorAngleDegrees(float rotorRpm, float ageInTicks) {
        return wrap360(rotorAngleDegrees(rotorRpm, ageInTicks) * TAIL_ROTOR_MULTIPLIER);
    }

    public static float rotorAngleRadians(float rotorRpm, float spinFactor, float ageInTicks) {
        return rotorAngleDegrees(rotorRpm, spinFactor, ageInTicks) / DEGREES_PER_RADIAN;
    }

    public static float rotorAngleRadians(float rotorRpm, float ageInTicks) {
        return rotorAngleRadians(rotorRpm, FULL_SPIN, ageInTicks);
    }

    public static float tailRotorAngleRadians(float rotorRpm, float spinFactor, float ageInTicks) {
        return tailRotorAngleDegrees(rotorRpm, ageInTicks) / DEGREES_PER_RADIAN * 0.0F
                + tailRotorAngleDegrees(rotorRpm, ageInTicks) / DEGREES_PER_RADIAN;
    }

    public static float tailRotorAngleRadians(float rotorRpm, float ageInTicks) {
        return tailRotorAngleDegrees(rotorRpm, ageInTicks) / DEGREES_PER_RADIAN;
    }

    /** Opacity of the rotor blur disc: invisible below the threshold. */
    public static float rotorBlurAlpha(float rotorRpm) {
        return rotorBlurAlpha(rotorRpm, FULL_SPIN);
    }

    /** The disc is subtler while the rotor is idling and strongest under load. */
    public static float rotorBlurAlpha(float rotorRpm, float spinFactor) {
        float rpm = clamp01(rotorRpm);
        if (rpm <= ROTOR_BLUR_RPM) {
            return 0.0F;
        }
        float span = 1.0F - ROTOR_BLUR_RPM;
        float load = 0.4F + 0.6F * Math.max(0.1F, clamp01(spinFactor));
        return clamp01((rpm - ROTOR_BLUR_RPM) / span) * ROTOR_BLUR_MAX_ALPHA * load;
    }

    public static float wrap360(float degrees) {
        float wrapped = degrees % 360.0F;
        return wrapped < 0.0F ? wrapped + 360.0F : wrapped;
    }

    public static float clamp01(float value) {
        if (Float.isNaN(value)) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(1.0F, value));
    }
    /** Revolutions per tick of the Gatling cluster: fast while the gun is live. */
    public static float clusterSpinDegreesPerTick(int gunState) {
        if (gunState == io.github.littlebird.entity.weapon.LittleBirdGunCycle.FIRING) {
            return 26.0F;
        }
        if (gunState == io.github.littlebird.entity.weapon.LittleBirdGunCycle.WARMING) {
            return 14.0F;
        }
        return 0.6F;
    }
    /**
     * Fills out with the RGBA glow of the gun muzzles for the given heat: dark red
     * when barely warm, bright orange near the overheat limit, and fully transparent
     * at zero heat. Allocation-free so the renderer can reuse one array.
     */
    public static void heatGlowColor(int heat, float[] out) {
        heatGlowColor(heat <= 0 ? 0.0F : heat / (float) LittleBirdGunCycle.MAX_HEAT, out);
    }

    /** Same, driven by a 0..1 fraction so any weapon's own heat scale can use it. */
    public static void heatGlowColor(float heatFraction, float[] out) {
        float fraction = clamp01(heatFraction);
        out[0] = 0.4F + 0.6F * fraction;
        out[1] = 0.1F + 0.65F * fraction;
        out[2] = 0.0F;
        out[3] = fraction;
    }
}
