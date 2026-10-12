package com.example.horsegenetics.common.gear;

import com.example.horsegenetics.common.parts.PartAnchor;
import com.example.horsegenetics.common.parts.PartKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Horse shoes: four hooves are four separate shoes, each drawn on its own foot
 * in its own metal, and a shipped material is never given a second shoe.
 */
class HorseShoesTest {

    private static final int IRON = HorseShoes.tint(HorseShoes.Metal.IRON.rgb());
    private static final int GOLD = HorseShoes.tint(HorseShoes.Metal.GOLD.rgb());

    @Test
    @DisplayName("an unshod horse draws nothing")
    void aBareHorseDrawsNothing() {
        assertTrue(HorseShoes.worn(0, 0, 0, 0).isEmpty());
    }

    @Test
    @DisplayName("one shoe is drawn on the hoof it is on, and on no other")
    void oneShoeIsOnItsOwnHoof() {
        PartAnchor[] hooves = {PartAnchor.HOOF_FRONT_LEFT, PartAnchor.HOOF_FRONT_RIGHT,
                PartAnchor.HOOF_HIND_LEFT, PartAnchor.HOOF_HIND_RIGHT};
        for (int hoof = 0; hoof < 4; hoof++) {
            int[] t = new int[4];
            t[hoof] = IRON;
            List<WornPart> worn = HorseShoes.worn(t[0], t[1], t[2], t[3]);
            assertEquals(1, worn.size());
            assertEquals(hooves[hoof], worn.get(0).anchor());
            assertEquals(WornPieces.HORSE_SHOE, worn.get(0).piece());
            assertEquals(PartKind.SHOE, worn.get(0).part().kind());
            assertEquals(IRON, worn.get(0).part().baseTint());
        }
    }

    @Test
    @DisplayName("four shoes in mixed metals keep their own metal, in hoof order")
    void mixedMetalsStayApart() {
        int copper = HorseShoes.tint(HorseShoes.Metal.COPPER.rgb());
        int diamond = HorseShoes.tint(HorseShoes.Metal.DIAMOND.rgb());
        List<WornPart> worn = HorseShoes.worn(IRON, GOLD, copper, diamond);
        assertEquals(PartAnchor.hooves().size(), worn.size());
        int[] want = {IRON, GOLD, copper, diamond};
        for (int i = 0; i < want.length; i++) {
            assertEquals(PartAnchor.hooves().get(i), worn.get(i).anchor());
            assertEquals(want[i], worn.get(i).part().baseTint());
        }
    }

    @Test
    @DisplayName("a black metal is still a shoe: its tint is never the bare hoof's zero")
    void aBlackShoeIsNotABareHoof() {
        int black = HorseShoes.tint(0x000000);
        assertNotEquals(0, black);
        assertEquals(1, HorseShoes.worn(0, 0, black, 0).size());
    }

    @Test
    @DisplayName("every shipped shoe has its own id, and another mod's copy of the material gets no second shoe")
    void shippedMaterialsAreNotDoubled() {
        Set<String> ids = new HashSet<>();
        for (HorseShoes.Metal metal : HorseShoes.Metal.values()) {
            assertTrue(ids.add(metal.id()), metal.id());
            assertTrue(metal.id().endsWith("_horse_shoe"), metal.id());
            assertTrue(HorseShoes.ships(metal.material()), metal.material());
            assertEquals(0, metal.rgb() >>> 24, metal.id() + " is a colour, not a tint");
        }
        // The two spellings of gold are one shoe.
        assertEquals("golden_horse_shoe", HorseShoes.Metal.GOLD.id());
        assertTrue(HorseShoes.ships("gold"));
        assertTrue(HorseShoes.ships("golden"));
        assertFalse(ids.contains(HorseShoes.idFor("gold")));
        // A metal nobody ships gets one of its own.
        assertFalse(HorseShoes.ships("tin"));
        assertEquals("tin_horse_shoe", HorseShoes.idFor("tin"));
    }
}
