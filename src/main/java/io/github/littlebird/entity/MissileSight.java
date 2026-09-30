package io.github.littlebird.entity;

import net.minecraft.world.phys.Vec3;

/**
 * A hull that can guide a missile it fired.
 *
 * <p>The TOW is a command line-of-sight weapon: the round is steered onto whatever the operator's
 * sight is holding <em>at that instant</em>, so the guiding hull has to hand the missile its
 * current sight line every tick. Before this interface existed the missile recognised exactly one
 * hull type by name and silently fell back to "fly straight" for every other vehicle, which is why
 * a Bradley TOW ignored the crosshair completely.
 *
 * <p>Not pure rules: it speaks Minecraft's Vec3.
 */
public interface MissileSight {
    /** Unit direction the operator's sight points, in world space. */
    Vec3 missileSightDirection();

    /** Where the sight ray starts: the launcher, not the operator's head. */
    Vec3 missileSightOrigin();
}
