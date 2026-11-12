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
    /** GBU-31 JDAM: a 2000 lb class guided bomb - heavy, slick, homing and steerable. */
    JDAM(240.0F, 60.0F, 10.0F, 0.985D, true, 907.0D),
    /** Incendiary canister: lighter, blunter and unguided; it sets the impact area alight. */
    INCENDIARY(90.0F, 0.0F, 7.0F, 0.970D, false, 340.0D);

    private final float damage;
    private final float groundVehicleDamage;
    private final float blastRadius;
    private final double drag;
    private final boolean guided;
    private final double massKg;

    BombKind(float damage, float groundVehicleDamage, float blastRadius, double drag,
             boolean guided, double massKg) {
        this.damage = damage;
        this.groundVehicleDamage = groundVehicleDamage;
        this.blastRadius = blastRadius;
        this.drag = drag;
        this.guided = guided;
        this.massKg = massKg;
    }

    /** Damage dealt to the entity a direct hit lands on. */
    public float damage() {
        return damage;
    }

    /**
     * Damage a direct hit deals to a tracked hull or an IFV. Kept apart from {@link #damage()}
     * because a penetrator warhead is priced against armour, not against flesh: the JDAM cracks a
     * hull over two hits, and the incendiary cannot mark one at all.
     */
    public float groundVehicleDamage() {
        return groundVehicleDamage;
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
