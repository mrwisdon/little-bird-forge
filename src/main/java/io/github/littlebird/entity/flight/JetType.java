package io.github.littlebird.entity.flight;

import io.github.littlebird.entity.weapon.JetCannon;

/**
 * Per-airframe numbers. The three airframes share every system, so the only real differences
 * are the flight profile, the gun and whether vector thrust exists: one enum keeps them from
 * drifting apart across a dozen separate if-statements.
 */
public enum JetType {
    F35(F35Profile.config(), JetCannon.F35_25MM, true),
    SU57(Su57Profile.config(), JetCannon.SU57_30MM, false),
    // B-2 Spirit: subsonic flying wing, no vector thrust. The real airframe carries no gun;
    // the shared jet code still needs a spec, so it reuses the light 25mm timing.
    B2(B2Profile.config(), JetCannon.F35_25MM, false);

    private final JetConfig config;
    private final JetCannon.Spec cannon;
    private final boolean vtol;

    JetType(JetConfig config, JetCannon.Spec cannon, boolean vtol) {
        this.config = config;
        this.cannon = cannon;
        this.vtol = vtol;
    }

    public JetConfig config() {
        return config;
    }

    public JetCannon.Spec cannon() {
        return cannon;
    }

    /** Only the F-35E has the vector-thrust hover; the Su-57 ignores the toggle. */
    public boolean supportsVtol() {
        return vtol;
    }
}
