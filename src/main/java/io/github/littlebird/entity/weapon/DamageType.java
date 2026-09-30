package io.github.littlebird.entity.weapon;

/**
 * What a weapon throws. This is the half of the damage sum that decides whether a round can
 * defeat armour at all.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public enum DamageType {
    /**
     * Small arms: rifle and machine gun rounds. They kill people and they chew up a helicopter,
     * but a tracked hull simply absorbs them - not reduced, nothing.
     */
    BULLET,
    /**
     * Cannon rounds, tank shells, rockets and missile warheads. Anything in this class can defeat
     * heavy armour; whether it is worth firing is a question of the numbers, not of the type.
     */
    SHELL;
}
