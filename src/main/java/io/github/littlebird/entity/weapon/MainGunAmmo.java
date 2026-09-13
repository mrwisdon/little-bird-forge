package io.github.littlebird.entity.weapon;

import java.util.Arrays;

/**
 * The 120mm carousel: one ready rack and one reserve per {@link MainGunAmmoType}.
 *
 * <p>The types never share a count - firing an HE round cannot leave the AP rack short - and the
 * reload clock runs for the rack being reloaded only. Switching types is free and instant, which is
 * what makes the carousel readable on the HUD: the selected type's numbers are the ones shown.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class MainGunAmmo {
    private final TwoStageMagazine.Spec spec;
    private final TwoStageMagazine.State[] states;
    private MainGunAmmoType selected;

    private MainGunAmmo(TwoStageMagazine.Spec spec, TwoStageMagazine.State[] states,
                        MainGunAmmoType selected) {
        this.spec = spec;
        this.states = states;
        this.selected = selected;
    }

    /** A full carousel: every type loaded to its ready capacity. */
    public static MainGunAmmo initial(TwoStageMagazine.Spec spec) {
        TwoStageMagazine.State[] states = new TwoStageMagazine.State[MainGunAmmoType.types()];
        Arrays.fill(states, TwoStageMagazine.initial(spec));
        return new MainGunAmmo(spec, states, MainGunAmmoType.AP);
    }

    /**
     * Rebuilds a carousel from saved racks. A short or null array is filled with a fresh rack, and
     * an unknown selection falls back to the first type, so a malformed save cannot break loading.
     */
    public static MainGunAmmo of(TwoStageMagazine.Spec spec, MainGunAmmoType selected,
                                 TwoStageMagazine.State[] saved) {
        TwoStageMagazine.State[] states = new TwoStageMagazine.State[MainGunAmmoType.types()];
        for (int i = 0; i < states.length; i++) {
            states[i] = saved != null && i < saved.length && saved[i] != null
                    ? saved[i] : TwoStageMagazine.initial(spec);
        }
        return new MainGunAmmo(spec, states, selected == null ? MainGunAmmoType.AP : selected);
    }

    public TwoStageMagazine.Spec spec() {
        return spec;
    }

    public MainGunAmmoType selected() {
        return selected;
    }

    public void select(MainGunAmmoType type) {
        if (type != null) {
            selected = type;
        }
    }

    /** Advances the carousel one type; returns the new selection. */
    public MainGunAmmoType cycleType() {
        selected = selected.next();
        return selected;
    }

    public TwoStageMagazine.State state() {
        return states[selected.ordinal()];
    }

    public TwoStageMagazine.State state(MainGunAmmoType type) {
        return states[type.ordinal()];
    }

    /** Ready rounds in the selected rack. */
    public int ready() {
        return state().ready();
    }

    public int ready(MainGunAmmoType type) {
        return state(type).ready();
    }

    public int reserve() {
        return state().reserve();
    }

    public int reserve(MainGunAmmoType type) {
        return state(type).reserve();
    }

    /** Takes one round out of the selected rack. False when the rack is empty. */
    public boolean fire() {
        TwoStageMagazine.State before = state();
        TwoStageMagazine.State after = TwoStageMagazine.fire(before, spec);
        if (after.ready() == before.ready()) {
            return false;
        }
        states[selected.ordinal()] = after;
        return true;
    }

    /** Starts reloading the selected rack; false when there is nothing to move. */
    public boolean startReload() {
        TwoStageMagazine.State before = state();
        TwoStageMagazine.State after = TwoStageMagazine.startReload(before, spec);
        if (after.reloadTicks() == before.reloadTicks()) {
            return false;
        }
        states[selected.ordinal()] = after;
        return true;
    }

    /** Advances every rack's timers by one tick. */
    public void tick() {
        for (int i = 0; i < states.length; i++) {
            states[i] = TwoStageMagazine.tick(states[i], spec);
        }
    }

    /** Tops the selected rack's reserve up from a crate. */
    public void supply(int rounds) {
        states[selected.ordinal()] = TwoStageMagazine.supply(state(), spec, rounds);
    }
}
