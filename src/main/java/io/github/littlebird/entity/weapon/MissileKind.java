package io.github.littlebird.entity.weapon;

/**
 * The three guided secondary weapons of a fighter. They share one flight entity and differ only in
 * the seeker, the warhead and whether flares can spoof them.
 */
public enum MissileKind {
    /** AIM-9Z / AA-11: short range, hard turning, seducible by flares. */
    IR(MissileGuidance.IR, 10.0F, 30.0F, true, false),
    /** AGM: locks ground armour, ignores flares, hits hard. */
    AGM(new MissileGuidance.Spec(7.0D, 8.0D), 45.0F, 16.0F, false, true),
    /** AIM-260R / AA-12: long range, slower seeker, can be fired without a full lock. */
    RADAR(MissileGuidance.RADAR, 8.0F, 26.0F, false, false);

    private final MissileGuidance.Spec guidance;
    private final float directDamage;
    private final float airframeDamage;
    private final boolean seducible;
    private final boolean groundTarget;

    MissileKind(MissileGuidance.Spec guidance, float directDamage, float airframeDamage,
            boolean seducible, boolean groundTarget) {
        this.guidance = guidance;
        this.directDamage = directDamage;
        this.airframeDamage = airframeDamage;
        this.seducible = seducible;
        this.groundTarget = groundTarget;
    }

    public MissileGuidance.Spec guidance() {
        return guidance;
    }

    public float directDamage() {
        return directDamage;
    }

    public float airframeDamage() {
        return airframeDamage;
    }

    public boolean seducible() {
        return seducible;
    }

    public boolean groundTarget() {
        return groundTarget;
    }

    public static MissileKind byOrdinal(int ordinal) {
        MissileKind[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : IR;
    }
}
