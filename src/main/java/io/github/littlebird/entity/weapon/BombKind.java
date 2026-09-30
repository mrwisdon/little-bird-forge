package io.github.littlebird.entity.weapon;

/**
 * The two stores a B-2 bomb bay carries. They share one entity shape and one release schedule and
 * differ in the warhead, how well they fly and whether a designation can steer them.
 *
 * <p>The damage and blast figures here are the shipped defaults; {@code LittleBirdConfig} owns the
 * live values and defaults to the same numbers, so the config is the tuning surface and this enum
 * is the honest description of the round for the rules and the tests.
 *
 * <p>Pure rules: no Minecraft classes.
 */
public enum BombKind {
    /** GBU-31 JDAM: a 2000 lb class guided bomb - heavy, slick and steerable. */
    JDAM(120.0F, 6.0F, 0.985D, true, 907.0D),
    /** Incendiary canister: lighter, blunter and unguided; it sets the impact area alight. */
    INCENDIARY(45.0F, 4.0F, 0.970D, false, 340.0D);

    private final float damage;
    private final float blastRadius;
    private final double drag;
    private final boolean guided;
    private final double massKg;

    BombKind(float damage, float blastRadius, double drag, boolean guided, double massKg) {
        this.damage = damage;
        this.blastRadius = blastRadius;
        this.drag = drag;
        this.guided = guided;
        this.massKg = massKg;
    }

    /** Damage dealt to the entity a direct hit lands on. */
    public float damage() {
        return damage;
    }

    /** Splash radius of the warhead, in blocks. */
    public float blastRadius() {
        return blastRadius;
    }

    /** Fraction of the horizontal velocity kept each tick: a slick bomb keeps more. */
    public double drag() {
        return drag;
    }

    /** True when a pod designation can steer this round. */
    public boolean guided() {
        return guided;
    }

    /** Warhead mass, in kilograms. */
    public double massKg() {
        return massKg;
    }

    /** The kind a wire ordinal names, falling back to the JDAM. */
    public static BombKind byOrdinal(int ordinal) {
        BombKind[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : JDAM;
    }
}
