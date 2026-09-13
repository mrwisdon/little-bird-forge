package io.github.littlebird.client;

/**
 * Geometry for the centre flight cluster, kept free of Minecraft classes so the
 * ladder and bank indicator maths are unit-tested.
 */
public final class LittleBirdHudMath {
    /** The reference ladder is airy: a line every ten degrees, not every five. */
    public static final int LADDER_STEP_DEGREES = 10;
    /** Full attitude range: the ladder scrolls, it is not all on screen at once. */
    public static final int LADDER_LIMIT_DEGREES = 90;
    /** Only lines this close to the current attitude are drawn, keeping it compact. */
    public static final double LADDER_WINDOW_DEGREES = 15.0D;
    /** Bank scale span: past this the pointer pegs at the end, like a real bank indicator. */
    public static final double BANK_ARC_LIMIT_DEGREES = 60.0D;

    private LittleBirdHudMath() {
    }

    /** Zero padded heading used by the heading box, wrapped into 0..359. */
    public static String headingLabel(float yawDeg) {
        int heading = Math.round(yawDeg) % 360;
        if (heading < 0) {
            heading += 360;
        }
        return String.format("%03d", heading);
    }

    /** Percentage label for the enemy airframe readout, 0% to 100%. */
    public static String healthPercentLabel(float fraction) {
        float clamped = Float.isFinite(fraction)
                ? Math.max(0.0F, Math.min(1.0F, fraction)) : 0.0F;
        return Math.round(clamped * 100.0F) + "%";
    }

    /** Pixels the horizon sits below the screen centre for the given nose-down pitch. */
    public static double ladderOffsetPixels(float pitchDeg, double pixelsPerDegree) {
        return Math.max(0.0D, pixelsPerDegree) * pitchDeg;
    }

    /** True while a ladder line is inside the visible band. */
    public static boolean ladderLineVisible(float lineDeg, float pitchDeg, double windowDeg) {
        return Math.abs(lineDeg - pitchDeg) <= Math.max(0.0D, windowDeg);
    }

    /** Angle of a bank scale tick in screen space, degrees clockwise from up. */
    public static double bankTickDegrees(double tickDeg, float rollDeg) {
        return tickDeg + rollDeg;
    }

    /**
     * Roll clamped to the bank scale. The airframe can roll all the way over, so without
     * this the pointer would swing off the arc and wrap onto the wrong side.
     */
    public static double clampBankDegrees(float rollDeg) {
        if (!Float.isFinite(rollDeg)) {
            return 0.0D;
        }
        return Math.max(-BANK_ARC_LIMIT_DEGREES,
                Math.min(BANK_ARC_LIMIT_DEGREES, rollDeg));
    }

    /** Clamped helper for anchors that must stay on screen. */
    public static int clampToScreen(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}