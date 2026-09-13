package io.github.littlebird.entity.weapon;

/**
 * Which weapon a ground vehicle crew member has selected.
 *
 * <p>The tank and the IFV split their weapons over seats - the driver owns the main gun (and, from
 * the detail phase, the smoke discharger) while the gunner owns the coaxial gun - so a single
 * "selected slot" is only meaningful for the seats that actually have a choice. The allowed set is
 * therefore passed in rather than hard coded: {@link #forTankDriver()} offers main gun and smoke,
 * and a gunner seat simply has no selection.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class WeaponSelection {
    /** Slot ids; the HUD draws one row per slot in this order. */
    public static final int MAIN_GUN = 0;
    public static final int COAX = 1;
    public static final int SMOKE = 2;
    public static final int SLOTS = 3;

    private final int[] allowed;
    private int selected;

    private WeaponSelection(int[] allowed, int initial) {
        this.allowed = allowed.clone();
        this.selected = isAllowed(initial) ? initial : this.allowed[0];
    }

    /** Main gun and smoke: the two things a tank driver can point and fire. */
    public static WeaponSelection forTankDriver() {
        return new WeaponSelection(new int[]{MAIN_GUN, SMOKE}, MAIN_GUN);
    }

    /** A single weapon seat: nothing to switch, used for the gunner's coaxial gun. */
    public static WeaponSelection fixed(int slot) {
        return new WeaponSelection(new int[]{clampSlot(slot)}, slot);
    }

    public int selected() {
        return selected;
    }

    /** True when the slot is one this seat may use. */
    public boolean isAllowed(int slot) {
        for (int candidate : allowed) {
            if (candidate == slot) {
                return true;
            }
        }
        return false;
    }

    /** Selects a slot directly. Returns true when the selection changed. */
    public boolean select(int slot) {
        if (!isAllowed(slot) || slot == selected) {
            return false;
        }
        selected = slot;
        return true;
    }

    /**
     * Steps through the allowed slots. {@code delta} is +1 or -1; anything else is treated as +1,
     * and a single-slot seat is a no-op.
     */
    public int cycle(int delta) {
        if (allowed.length <= 1) {
            return selected;
        }
        int index = 0;
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i] == selected) {
                index = i;
                break;
            }
        }
        int step = delta < 0 ? -1 : 1;
        index = (index + step + allowed.length) % allowed.length;
        selected = allowed[index];
        return selected;
    }

    public boolean is(int slot) {
        return selected == slot;
    }

    /** Keeps an arbitrary number inside the slot range. */
    public static int clampSlot(int slot) {
        if (slot < 0) {
            return 0;
        }
        return Math.min(slot, SLOTS - 1);
    }
}
