package io.github.littlebird.entity.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The 120mm shell carousel. */
class MainGunAmmoTypeTest {
    @Test
    void carouselWraps() {
        assertEquals(MainGunAmmoType.AP, MainGunAmmoType.byOrdinal(0));
        assertEquals(MainGunAmmoType.HE, MainGunAmmoType.byOrdinal(1));
        assertEquals(MainGunAmmoType.CANISTER, MainGunAmmoType.byOrdinal(2));
        assertEquals(MainGunAmmoType.AP, MainGunAmmoType.byOrdinal(3), "past the end wraps");
        assertEquals(MainGunAmmoType.CANISTER, MainGunAmmoType.byOrdinal(-1), "negative wraps back");
    }

    @Test
    void nextStepsAroundTheCarousel() {
        assertEquals(MainGunAmmoType.HE, MainGunAmmoType.AP.next());
        assertEquals(MainGunAmmoType.CANISTER, MainGunAmmoType.HE.next());
        assertEquals(MainGunAmmoType.AP, MainGunAmmoType.CANISTER.next());
        assertEquals(3, MainGunAmmoType.types());
    }

    @Test
    void idsAndKeysAreUnique() {
        Set<String> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (MainGunAmmoType type : MainGunAmmoType.values()) {
            ids.add(type.id());
            keys.add(type.translationKey());
        }
        assertEquals(MainGunAmmoType.types(), ids.size());
        assertEquals(MainGunAmmoType.types(), keys.size());
        assertNotEquals(MainGunAmmoType.AP.translationKey(),
                MainGunAmmoType.HE.translationKey());
    }
}
