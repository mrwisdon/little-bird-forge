package io.github.littlebird.entity.weapon;

/**
 * The Bradley's smoke wall: one trigger lays a row of canisters abreast, a fixed distance in front
 * of the hull, so the cloud reads as a screen rather than a puff around the vehicle.
 *
 * <p>It is a discharger, not an ammunition pool: what matters is how many salvos are left, whether
 * the launcher is still cooling down, and how long the cloud has to run. The layout numbers are
 * balance values owned by the config; this class only decides where the canisters go.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public final class SmokeWall {
    /**
     * Salvos before a refill, the wall layout (canisters, spacing, distance ahead of the nose),
     * the cooldown between salvos and how long a cloud lasts.
     */
    public record Spec(int canisters, double spacingBlocks, double aheadOfNoseBlocks,
                       int cooldownTicks, int durationTicks, int charges) {
        public Spec {
            canisters = Math.max(1, canisters);
            spacingBlocks = Math.max(0.0D, spacingBlocks);
            aheadOfNoseBlocks = Math.max(0.0D, aheadOfNoseBlocks);
            cooldownTicks = Math.max(0, cooldownTicks);
            durationTicks = Math.max(0, durationTicks);
            charges = Math.max(0, charges);
        }

        /** Total width the row covers: one spacing less than the number of canisters. */
        public double wallWidthBlocks() {
            return (canisters - 1) * spacingBlocks;
        }
    }

    /** Salvos left, ticks until the next salvo may fire, ticks the current cloud has left. */
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

    /** Lays one wall and starts the cooldown and the cloud. Refuses when not ready. */
    public static State fire(State state, Spec spec) {
        if (!state.canFire()) {
            return state;
        }
        return new State(state.charges() - 1, spec.cooldownTicks(),
                Math.max(state.activeTicks(), spec.durationTicks()));
    }

    public static State tick(State state, Spec spec) {
        return new State(state.charges(),
                Math.max(0, state.cooldownTicks() - 1),
                Math.max(0, state.activeTicks() - 1));
    }

    /** Tops the salvos up from a crate, never past the spec's capacity. */
    public static State supply(State state, Spec spec, int charges) {
        int wanted = Math.max(0, charges);
        return new State(Math.min(spec.charges(), state.charges() + wanted),
                state.cooldownTicks(), state.activeTicks());
    }

    /**
     * Lateral offsets of the canisters, in blocks, centred on the hull's centre line and ordered
     * from the vehicle's left to its right. A single canister sits on the centre line.
     */
    public static double[] lateralOffsetsBlocks(Spec spec) {
        double[] offsets = new double[spec.canisters()];
        double half = spec.wallWidthBlocks() / 2.0D;
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] = half - i * spec.spacingBlocks();
        }
        return offsets;
    }

    /**
     * How far in front of the entity origin the middle of the wall lands: the nose, plus the
     * distance the spec asks for.
     */
    public static double wallCenterAheadBlocks(double hullHalfLengthBlocks, Spec spec) {
        return hullHalfLengthBlocks + spec.aheadOfNoseBlocks();
    }

    /** 0..1 fraction of the salvos still available, for the HUD. */
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

    private SmokeWall() {
    }
}
