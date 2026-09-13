package io.github.littlebird.entity.weapon;

/**
 * Vehicle smoke discharger: a small number of charges, a reload clock and a cloud that lingers for
 * a while.
 *
 * <p>It is deliberately not an ammunition pool: smoke is a defensive ability, so what matters is
 * how many charges are left, whether the launcher is still cooling down, and how long the cloud
 * has left to run. The numbers themselves are balance values owned by the config.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class SmokeLauncher {
    /** charges fired before a refill, the cooldown between shots, and how long a cloud lasts. */
    public record Spec(int charges, int cooldownTicks, int durationTicks) {
        public Spec {
            charges = Math.max(0, charges);
            cooldownTicks = Math.max(0, cooldownTicks);
            durationTicks = Math.max(0, durationTicks);
        }
    }

    /** charges left, ticks until the next charge may fire, ticks the current cloud has left. */
    public record State(int charges, int cooldownTicks, int activeTicks) {
        public boolean canFire() {
            return charges > 0 && cooldownTicks <= 0;
        }

        public boolean cloudActive() {
            return activeTicks > 0;
        }
    }

    public static State initial(Spec spec) {
        return new State(spec.charges(), 0, 0);
    }

    /** Fires one charge and starts the cooldown and the cloud. Refuses when not ready. */
    public static State fire(State state, Spec spec) {
        if (!state.canFire()) {
            return state;
        }
        return new State(state.charges() - 1, spec.cooldownTicks(),
                Math.max(state.activeTicks(), spec.durationTicks()));
    }

    /** Advances the cooldown and the cloud by one tick. */
    public static State tick(State state, Spec spec) {
        return new State(state.charges(),
                Math.max(0, state.cooldownTicks() - 1),
                Math.max(0, state.activeTicks() - 1));
    }

    /** Tops the charges up from a crate, never past the spec's capacity. */
    public static State supply(State state, Spec spec, int charges) {
        int wanted = Math.max(0, charges);
        int topped = Math.min(spec.charges(), state.charges() + wanted);
        return new State(topped, state.cooldownTicks(), state.activeTicks());
    }

    /** 0..1 fraction of the charges still available, for the HUD. */
    public static float chargeFraction(State state, Spec spec) {
        return spec.charges() <= 0 ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, state.charges() / (float) spec.charges()));
    }

    /** 0..1 fraction of the cooldown already elapsed, for the HUD ring. */
    public static float cooldownFraction(State state, Spec spec) {
        if (spec.cooldownTicks() <= 0 || state.cooldownTicks() <= 0) {
            return 1.0F;
        }
        return Math.max(0.0F, Math.min(1.0F,
                1.0F - state.cooldownTicks() / (float) spec.cooldownTicks()));
    }

    private SmokeLauncher() {
    }
}
