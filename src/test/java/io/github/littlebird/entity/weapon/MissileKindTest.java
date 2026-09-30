package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.littlebird.entity.weapon.MissileKind;
import org.junit.jupiter.api.Test;

final class MissileKindTest {
    @Test
    void onlyTheIrMissileIsSeducibleByFlares() {
        assertTrue(MissileKind.IR.seducible());
        assertFalse(MissileKind.AGM.seducible());
        assertFalse(MissileKind.RADAR.seducible());
    }

    @Test
    void onlyTheAgmLocksGroundTargets() {
        assertTrue(MissileKind.AGM.groundTarget());
        assertFalse(MissileKind.IR.groundTarget());
        assertFalse(MissileKind.RADAR.groundTarget());
    }

    @Test
    void everyKindHasGuidanceAndPositiveDamage() {
        for (MissileKind kind : MissileKind.values()) {
            assertNotNull(kind.guidance(), kind + " needs a seeker");
            assertTrue(kind.directDamage() > 0.0F, kind + " needs damage");
            assertTrue(kind.airframeDamage() > 0.0F, kind + " needs airframe damage");
        }
    }

    @Test
    void anUnknownOrdinalFallsBackToTheIrMissile() {
        assertEquals(MissileKind.IR, MissileKind.byOrdinal(-1));
        assertEquals(MissileKind.IR, MissileKind.byOrdinal(99));
        assertEquals(MissileKind.RADAR, MissileKind.byOrdinal(MissileKind.RADAR.ordinal()));
    }
}
