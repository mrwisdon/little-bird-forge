package io.github.littlebird.entity.weapon;

/**
 * Shell types the 120mm main gun can load.
 *
 * <p>The type only names the round and gives it a HUD translation key; the ballistic numbers stay
 * in the config so a server can retune them, and nothing here invents a balance value. Each type
 * keeps its own ready rack and reserve (see the vehicle), so switching types never mixes counts.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public enum MainGunAmmoType {
    /** Armour-piercing round: the default load, best against vehicles. */
    AP("ap"),
    /** High explosive round: less armour penetration, a bigger blast. */
    HE("he"),
    /** Canister round: forty pellets in a cone, short range and lethal up close. */
    CANISTER("canister");

    private final String id;

    MainGunAmmoType(String id) {
        this.id = id;
    }

    /** Stable id for NBT and network traffic. */
    public String id() {
        return id;
    }

    /** Localisation key for the HUD, e.g. {@code hud.battlefield.shell.ap}. */
    public String translationKey() {
        return "hud.battlefield.shell." + id;
    }

    /** Number of types on the carousel. */
    public static int types() {
        return values().length;
    }

    /** Wraps an arbitrary index into the carousel. */
    public static MainGunAmmoType byOrdinal(int ordinal) {
        MainGunAmmoType[] all = values();
        int index = ordinal % all.length;
        if (index < 0) {
            index += all.length;
        }
        return all[index];
    }

    /** The next type on the carousel, wrapping at the end. */
    public MainGunAmmoType next() {
        return byOrdinal(ordinal() + 1);
    }
}
