package io.github.littlebird.item;

/**
 * Where a deployed airframe ends up.
 *
 * The first version placed it at the player's eyes plus the look vector times the spawn
 * distance, so the spawn height was whatever the player happened to be aiming at. Looking at
 * the ground a few blocks ahead - which is what anyone does when placing something - put the
 * aircraft about a block underground, and it stayed there: nothing pushes an entity back out
 * of a block it started inside.
 *
 * The rule now: the aim decides the spot, the ground decides the height. Pure rules, so the
 * rule itself is unit tested.
 */
public final class GunshipPlacement {
    /** The probe starts just above the player's feet: above a step up, below any ceiling. */
    public static final double PROBE_ABOVE = 1.0D;
    /** And looks this far down, so a slope or a drop in front is found as well. */
    public static final double PROBE_BELOW = 8.0D;

    private GunshipPlacement() {
    }

    /** Top of the ground probe, in world Y, at the given feet height. */
    public static double probeFromY(double feetY) {
        return feetY + PROBE_ABOVE;
    }

    /** Bottom of the ground probe, in world Y. */
    public static double probeToY(double feetY) {
        return feetY - PROBE_BELOW;
    }

    /**
     * The height to stand an airframe at, given what the probe found. No hit means open air -
     * a cliff edge, a bridge, a boat deck - and then the player's own level stands.
     */
    public static double baseY(boolean hitGround, double groundY, double aimY) {
        return hitGround ? groundY : aimY;
    }
}
