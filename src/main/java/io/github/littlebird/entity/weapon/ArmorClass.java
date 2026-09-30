package io.github.littlebird.entity.weapon;

/**
 * What a target is made of. Every weapon asks this one question about whatever it hit, so the
 * answer has to live in exactly one place.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public enum ArmorClass {
    /** Players and mobs: no armour at all, so the anti-personnel price applies. */
    UNARMORED,
    /** Helicopters and jets. Bullets hurt these - they are not tanks. */
    AIRFRAME,
    /** Wheeled armour: it stops rifle fire, not cannon rounds in any useful amount. */
    LIGHT_ARMOR,
    /** Tracked hulls. Only a shell gets through one. */
    HEAVY_ARMOR;
}
