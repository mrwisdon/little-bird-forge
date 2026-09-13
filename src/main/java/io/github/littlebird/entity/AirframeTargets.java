package io.github.littlebird.entity;

import io.github.littlebird.entity.weapon.LittleBirdBallistics;
import net.minecraft.world.entity.Entity;

/**
 * One place that decides which class of airframe a hit landed on.
 *
 * Every weapon needs the same answer - the Little Bird's gun, its rockets, the Apache's chain
 * gun and its TOW all price a hull differently from a mob - and while each weapon kept its own
 * copy of the check, an Apache shot by a Little Bird was billed as an unarmoured target.
 */
public final class AirframeTargets {
    private AirframeTargets() {
    }

    /** The airframe class of a hit target, or {@code NONE} for anything that is not an aircraft. */
    public static LittleBirdBallistics.Airframe classify(Entity entity) {
        if (entity instanceof ApacheEntity) {
            return LittleBirdBallistics.Airframe.APACHE;
        }
        if (entity instanceof LittleBirdEntity) {
            return LittleBirdBallistics.Airframe.LITTLE_BIRD;
        }
        return LittleBirdBallistics.Airframe.NONE;
    }

    /** True for anything the dogfight prices fly against. */
    public static boolean isAirframe(Entity entity) {
        return classify(entity) != LittleBirdBallistics.Airframe.NONE;
    }
}
